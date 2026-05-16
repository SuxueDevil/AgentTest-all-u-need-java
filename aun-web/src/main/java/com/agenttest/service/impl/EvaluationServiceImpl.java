package com.agenttest.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.agenttest.common.PageResult;
import com.agenttest.common.exception.BusinessException;
import com.agenttest.engine.AgentHttpClient;
import com.agenttest.engine.AgentHttpClient.AgentResponse;
import com.agenttest.engine.ScoringEngine;
import com.agenttest.mapper.AgentMapper;
import com.agenttest.mapper.EvaluationResultMapper;
import com.agenttest.mapper.EvaluationTaskMapper;
import com.agenttest.mapper.QuestionMapper;
import com.agenttest.pojo.dto.EvaluationTaskCreateDTO;
import com.agenttest.pojo.dto.EvaluationTaskQueryDTO;
import com.agenttest.pojo.dto.EvaluationTaskUpdateDTO;
import com.agenttest.pojo.entity.*;
import com.agenttest.pojo.entity.EvaluationTask.DimensionConfig;
import com.agenttest.pojo.entity.EvaluationResult.DimensionScore;
import com.agenttest.pojo.vo.*;
import com.agenttest.pojo.vo.AgentResultVO.*;
import com.agenttest.service.EvaluationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 评测任务业务实现 — CRUD + 异步执行 + 进度轮询 + 结果聚合。
 * <p>
 * start() 触发异步评测，cancel() 通过标志位中断。
 * progress() 返回任务级状态和计数值，供前端 3s 轮询。
 * getResults() 按 Agent 分组聚合所有单题结果。
 */
@Service
public class EvaluationServiceImpl implements EvaluationService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationServiceImpl.class);

    private final EvaluationTaskMapper taskMapper;
    private final EvaluationResultMapper resultMapper;
    private final AgentMapper agentMapper;
    private final QuestionMapper questionMapper;
    private final AgentHttpClient httpClient;
    private final ScoringEngine scoringEngine;

    /** 构造器注入 */
    public EvaluationServiceImpl(EvaluationTaskMapper taskMapper,
                                  EvaluationResultMapper resultMapper,
                                  AgentMapper agentMapper,
                                  QuestionMapper questionMapper,
                                  AgentHttpClient httpClient,
                                  ScoringEngine scoringEngine) {
        this.taskMapper = taskMapper;
        this.resultMapper = resultMapper;
        this.agentMapper = agentMapper;
        this.questionMapper = questionMapper;
        this.httpClient = httpClient;
        this.scoringEngine = scoringEngine;
    }

    /** 运行中任务的取消标志位，key=taskId, value=true=取消 */
    private final Map<Long, Boolean> cancelFlags = new ConcurrentHashMap<>();

    // ==================== CRUD ====================

    @Override
    public PageResult<EvaluationTaskVO> page(EvaluationTaskQueryDTO query) {
        LambdaQueryWrapper<EvaluationTask> wrapper = new LambdaQueryWrapper<>();
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            wrapper.eq(EvaluationTask::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(EvaluationTask::getCreatedAt);

        IPage<EvaluationTask> page = taskMapper.selectPage(
                new Page<>(query.getPage(), query.getPageSize()), wrapper);

        List<EvaluationTaskVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        log.info("分页查询评测任务完成，共 {} 条", page.getTotal());
        return new PageResult<>(voList, page.getTotal(), query.getPage(), query.getPageSize());
    }

    @Override
    public EvaluationTaskVO getById(Long id) {
        log.info("查询评测任务详情，id={}", id);
        return toVO(getTaskEntity(id));
    }

    @Override
    public EvaluationTaskVO create(EvaluationTaskCreateDTO dto) {
        log.info("创建评测任务，name={} questions={} agents={}", dto.getName(),
                dto.getQuestionIds().size(), dto.getAgentIds().size());

        EvaluationTask task = new EvaluationTask();
        task.setName(dto.getName());
        task.setDescription(dto.getDescription());
        task.setQuestionIds(dto.getQuestionIds());
        task.setAgentIds(dto.getAgentIds());
        task.setDimensions(dto.getDimensions());
        task.setQuestionCount(dto.getQuestionIds().size());
        task.setCompletedCount(0);
        task.setStatus("pending");
        task.setRun(1);
        taskMapper.insert(task);
        return toVO(task);
    }

    @Override
    public EvaluationTaskVO update(Long id, EvaluationTaskUpdateDTO dto) {
        EvaluationTask task = getTaskEntity(id);
        if ("running".equals(task.getStatus())) {
            throw new BusinessException(400, "运行中的任务不可编辑");
        }
        if (dto.getName() != null) task.setName(dto.getName());
        if (dto.getDescription() != null) task.setDescription(dto.getDescription());
        log.info("更新评测任务，id={}", id);
        taskMapper.updateById(task);
        return toVO(task);
    }

    @Override
    public void delete(Long id) {
        EvaluationTask task = getTaskEntity(id);
        if ("running".equals(task.getStatus())) {
            throw new BusinessException(400, "运行中的任务不可删除，请先取消");
        }
        log.info("删除评测任务，id={}", id);
        // 先删关联结果，再删任务（数据库有 ON DELETE CASCADE 兜底）
        LambdaQueryWrapper<EvaluationResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EvaluationResult::getTaskId, id);
        resultMapper.delete(wrapper);
        taskMapper.deleteById(id);
    }

    // ==================== 执行控制 ====================

    @Override
    public void start(Long id) {
        EvaluationTask task = getTaskEntity(id);
        if (!"pending".equals(task.getStatus())) {
            throw new BusinessException(400, "仅 pending 状态的任务可以启动");
        }
        task.setStatus("running");
        task.setStartedAt(LocalDateTime.now());
        taskMapper.updateById(task);

        cancelFlags.remove(id);
        log.info("评测任务启动，id={} questions={} agents={}", id,
                task.getQuestionIds().size(), task.getAgentIds().size());

        // 异步执行，不阻塞 Controller 返回
        executeAsync(id);
    }

    @Override
    public void restart(Long id) {
        EvaluationTask task = getTaskEntity(id);
        if ("running".equals(task.getStatus())) {
            throw new BusinessException(400, "运行中的任务不能重新开始，请先取消");
        }
        // 不删除旧结果，递增批次号，历史结果按 run 保留
        task.setRun((task.getRun() == null ? 0 : task.getRun()) + 1);
        task.setStatus("pending");
        task.setCompletedCount(0);
        task.setStartedAt(null);
        task.setCompletedAt(null);
        taskMapper.updateById(task);
        log.info("评测任务重新开始，id={} run={}", id, task.getRun());
    }

    @Override
    public void cancel(Long id) {
        EvaluationTask task = getTaskEntity(id);
        if (!"running".equals(task.getStatus())) {
            throw new BusinessException(400, "仅 running 状态的任务可以取消");
        }
        cancelFlags.put(id, true);
        task.setStatus("cancelled");
        task.setCompletedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        log.info("评测任务已取消，id={}", id);
    }

    @Override
    public TaskProgressVO getProgress(Long id) {
        EvaluationTask task = getTaskEntity(id);
        return new TaskProgressVO(task.getStatus(), task.getQuestionCount(),
                task.getCompletedCount());
    }

    @Override
    public List<AgentResultVO> getResults(Long id) {
        EvaluationTask task = getTaskEntity(id);
        if (!"completed".equals(task.getStatus()) && !"cancelled".equals(task.getStatus())) {
            throw new BusinessException(400, "任务尚未完成，无法查看结果");
        }

        // 查询当前批次的单题结果
        LambdaQueryWrapper<EvaluationResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EvaluationResult::getTaskId, id)
               .eq(EvaluationResult::getRun, task.getRun());
        List<EvaluationResult> allResults = resultMapper.selectList(wrapper);

        log.info("获取评测结果，taskId={} 共 {} 条记录", id, allResults.size());
        return aggregateByAgent(task, allResults);
    }

    // ==================== 异步执行 ====================

    /**
     * 异步执行评测 — 遍历 agentIds × questionIds 逐个调用 Agent 并评分。
     * 使用 @Async 确保不阻塞 HTTP 请求线程。
     * 每完成一条结果即更新 completedCount，取消标志位为 true 时中断。
     */
    @Async("evaluationExecutor")
    public void executeAsync(Long taskId) {
        EvaluationTask task = taskMapper.selectById(taskId);
        if (task == null) return;

        List<Long> questionIds = task.getQuestionIds();
        List<Long> agentIds = task.getAgentIds();
        List<DimensionConfig> dimensions = task.getDimensions();

        for (Long agentId : agentIds) {
            // 检查取消标志
            if (Boolean.TRUE.equals(cancelFlags.get(taskId))) break;

            Agent agent = agentMapper.selectById(agentId);
            if (agent == null || agent.getEndpointUrl() == null) {
                log.warn("Agent {} 不存在或未配置 endpoint，跳过", agentId);
                continue;
            }

            for (Long questionId : questionIds) {
                if (Boolean.TRUE.equals(cancelFlags.get(taskId))) break;

                Question question = questionMapper.selectById(questionId);
                if (question == null) {
                    log.warn("题目 {} 不存在，跳过", questionId);
                    continue;
                }

                try {
                    // 调用 Agent
                    AgentResponse resp = httpClient.send(agent, question);

                    // 评分
                    List<DimensionScore> dimensionScores = scoringEngine.score(
                            resp.content, question.getExpectedAnswer(), dimensions);
                    double overall = scoringEngine.overallScore(dimensionScores, dimensions);

                    // 判断是否通过（取各维度阈值的平均值）
                    double avgThreshold = dimensions.stream()
                            .mapToDouble(DimensionConfig::getThreshold)
                            .average().orElse(0.5);
                    boolean passed = overall >= avgThreshold;

                    // 保存结果
                    EvaluationResult result = new EvaluationResult();
                    result.setTaskId(taskId);
                    result.setAgentId(agentId);
                    result.setQuestionId(questionId);
                    result.setOverallScore(overall);
                    result.setRun(task.getRun());
                    result.setPassed(passed);
                    result.setLatencyMs(resp.latencyMs);
                    result.setTokensUsed(resp.tokensUsed);
                    result.setDimensionScores(dimensionScores);
                    result.setRawRequest(resp.rawRequest);
                    result.setRawResponse(resp.rawResponse);
                    resultMapper.insert(result);

                    // 更新进度
                    task.setCompletedCount((task.getCompletedCount() == null ? 0
                            : task.getCompletedCount()) + 1);
                    taskMapper.updateById(task);

                    log.info("评测完成: task={} agent={} question={} score={}", taskId, agentId, questionId, overall);
                } catch (Exception e) {
                    log.error("评测失败: task={} agent={} question={} error={}", taskId, agentId, questionId, e.getMessage());
                    // 继续下一条，不因单条失败中断整个任务
                }
            }
        }

        // 全部完成后更新状态
        cancelFlags.remove(taskId);
        EvaluationTask latest = taskMapper.selectById(taskId);
        if (latest != null && "running".equals(latest.getStatus())) {
            latest.setStatus("completed");
            latest.setCompletedAt(LocalDateTime.now());
            taskMapper.updateById(latest);
            log.info("评测任务全部完成，id={}", taskId);
        }
    }

    // ==================== 结果聚合 ====================

    /**
     * 按 Agent 分组聚合评测结果。
     * 每个 Agent 的 overallScore 为各题得分的加权平均，
     * dimensionScores 为各维度得分按题平均。
     */
    private List<AgentResultVO> aggregateByAgent(EvaluationTask task,
                                                  List<EvaluationResult> allResults) {
        Map<Long, List<EvaluationResult>> grouped = allResults.stream()
                .collect(Collectors.groupingBy(EvaluationResult::getAgentId));

        return task.getAgentIds().stream()
                .map(agentId -> {
                    List<EvaluationResult> agentResults = grouped.getOrDefault(agentId, List.of());
                    return buildAgentResult(agentId, agentResults, task.getDimensions());
                })
                .collect(Collectors.toList());
    }

    /** 构建单个 Agent 的聚合结果 */
    private AgentResultVO buildAgentResult(Long agentId, List<EvaluationResult> results,
                                           List<DimensionConfig> dimensions) {
        Agent agent = agentMapper.selectById(agentId);

        AgentResultVO vo = new AgentResultVO();
        vo.setAgentId(agentId);
        vo.setAgentName(agent != null ? agent.getName() : "未知Agent");
        vo.setAvgLatencyMs(results.isEmpty() ? 0 :
                (long) results.stream().mapToInt(EvaluationResult::getLatencyMs).average().orElse(0));
        vo.setTotalTokens(results.stream().mapToInt(EvaluationResult::getTokensUsed).sum());
        vo.setOverallScore(results.isEmpty() ? 0 :
                results.stream().mapToDouble(EvaluationResult::getOverallScore).average().orElse(0));

        // 聚合各维度得分
        if (!results.isEmpty()) {
            vo.setDimensionScores(aggregateDimensions(results));
        } else {
            vo.setDimensionScores(List.of());
        }

        // 通过判定（取维度阈值平均）
        double avgThreshold = dimensions.stream()
                .mapToDouble(DimensionConfig::getThreshold)
                .average().orElse(0.5);
        vo.setPassed(vo.getOverallScore() >= avgThreshold);

        // 逐题明细
        vo.setItems(results.stream().map(r -> buildResultItem(r, dimensions)).collect(Collectors.toList()));

        return vo;
    }

    /** 聚合维度得分 — 对每个维度取所有题目得分的平均值 */
    private List<AgentResultVO.DimensionScoreVO> aggregateDimensions(List<EvaluationResult> results) {
        if (results.isEmpty()) return List.of();
        // 取第一条结果的维度名列表
        List<String> dimNames = results.get(0).getDimensionScores().stream()
                .map(DimensionScore::getDimensionName).collect(Collectors.toList());

        return dimNames.stream().map(name -> {
            double avg = results.stream()
                    .flatMapToDouble(r -> r.getDimensionScores().stream()
                            .filter(d -> d.getDimensionName().equals(name))
                            .mapToDouble(DimensionScore::getScore))
                    .average().orElse(0);
            AgentResultVO.DimensionScoreVO ds = new AgentResultVO.DimensionScoreVO();
            ds.setDimensionName(name);
            ds.setScore(Math.round(avg * 1000.0) / 1000.0);
            ds.setFeedback(avg >= 0.8 ? "整体表现优秀" : avg >= 0.6 ? "整体表现良好" : "整体表现一般");
            return ds;
        }).collect(Collectors.toList());
    }

    /** 构建单题结果明细 */
    private AgentResultVO.ResultItemVO buildResultItem(EvaluationResult r,
                                                        List<DimensionConfig> dimensions) {
        Question question = questionMapper.selectById(r.getQuestionId());
        AgentResultVO.ResultItemVO item = new AgentResultVO.ResultItemVO();
        item.setQuestionId(r.getQuestionId());
        item.setQuestionTitle(question != null ? question.getTitle() : "未知题目");
        item.setScore(r.getOverallScore());
        item.setPassed(r.getPassed());
        item.setLatencyMs(r.getLatencyMs());
        item.setTokensUsed(r.getTokensUsed());
        item.setDimensionScores(r.getDimensionScores().stream().map(ds -> {
            AgentResultVO.DimensionScoreVO dsv = new AgentResultVO.DimensionScoreVO();
            dsv.setDimensionName(ds.getDimensionName());
            dsv.setScore(ds.getScore());
            dsv.setFeedback(ds.getFeedback());
            return dsv;
        }).collect(Collectors.toList()));
        return item;
    }

    // ==================== 工具方法 ====================

    /** 按 ID 查询任务 entity，不存在时抛 BusinessException(404) */
    private EvaluationTask getTaskEntity(Long id) {
        EvaluationTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(404, "评测任务不存在");
        }
        return task;
    }

    /** entity → VO */
    private EvaluationTaskVO toVO(EvaluationTask entity) {
        EvaluationTaskVO vo = new EvaluationTaskVO();
        BeanUtil.copyProperties(entity, vo);
        return vo;
    }
}
