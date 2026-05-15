package com.agenttest.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.agenttest.common.PageResult;
import com.agenttest.common.exception.BusinessException;
import com.agenttest.engine.EvaluationEngine;
import com.agenttest.mapper.AgentMapper;
import com.agenttest.mapper.LLMMapper;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 评测任务业务实现 — CRUD + 结果聚合。
 * <p>
 * 执行逻辑已抽离到 {@link EvaluationEngine}，
 * start() 将任务异步提交给 Engine 后立即返回，不阻塞 Controller。
 * cancel() 通知 Engine 设置取消标志，运行中的轮次检测后中断。
 */
@Service
public class EvaluationServiceImpl implements EvaluationService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationServiceImpl.class);

    private final EvaluationTaskMapper taskMapper;
    private final EvaluationResultMapper resultMapper;
    private final AgentMapper agentMapper;
    private final LLMMapper llmMapper;
    private final QuestionMapper questionMapper;
    private final EvaluationEngine engine;
    private final Executor executor;

    /** 构造器注入 */
    public EvaluationServiceImpl(EvaluationTaskMapper taskMapper,
                                  EvaluationResultMapper resultMapper,
                                  AgentMapper agentMapper,
                                  LLMMapper llmMapper,
                                  QuestionMapper questionMapper,
                                  EvaluationEngine engine,
                                  @Qualifier("evaluationExecutor") Executor executor) {
        this.taskMapper = taskMapper;
        this.resultMapper = resultMapper;
        this.agentMapper = agentMapper;
        this.llmMapper = llmMapper;
        this.questionMapper = questionMapper;
        this.engine = engine;
        this.executor = executor;
    }

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
        task.setLlmIds(dto.getLlmIds());
        task.setDimensions(dto.getDimensions());
        int targetCount = dto.getAgentIds().size()
                + (dto.getLlmIds() != null ? dto.getLlmIds().size() : 0);

        // 多轮题目的 questionCount 按 user 消息数计算，每条 user 消息 = 一轮评测
        int totalTurnCount = 0;
        for (Long qid : dto.getQuestionIds()) {
            Question q = questionMapper.selectById(qid);
            if (q != null && "multi".equals(q.getQuestionType()) && q.getTurns() != null) {
                totalTurnCount += (int) q.getTurns().stream()
                        .filter(t -> !"assistant".equalsIgnoreCase(t.getRole())).count();
            } else {
                totalTurnCount += 1;
            }
        }
        task.setQuestionCount(totalTurnCount * targetCount);
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

        engine.clearCancelFlag(id);
        log.info("评测任务启动，id={} questions={} agents={}", id,
                task.getQuestionIds().size(), task.getAgentIds().size());

        // 异步提交给 Engine，不阻塞 Controller 返回
        CompletableFuture.runAsync(() -> engine.execute(id), executor);
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
        // 通知 Engine 设置取消标志，运行中的轮次检测后中断
        engine.cancel(id);
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
        if ("pending".equals(task.getStatus())) {
            throw new BusinessException(400, "任务尚未启动");
        }

        // 查询当前批次的单题结果
        LambdaQueryWrapper<EvaluationResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EvaluationResult::getTaskId, id)
               .eq(EvaluationResult::getRun, task.getRun());
        List<EvaluationResult> allResults = resultMapper.selectList(wrapper);

        log.info("获取评测结果，taskId={} 共 {} 条记录", id, allResults.size());
        return aggregateByTarget(task, allResults);
    }

    // ==================== 结果聚合 ====================

    /**
     * 按 Agent / LLM 分组聚合评测结果。
     */
    private List<AgentResultVO> aggregateByTarget(EvaluationTask task,
                                                  List<EvaluationResult> allResults) {
        List<AgentResultVO> result = new ArrayList<>();

        // Agent 结果聚合
        Map<Long, List<EvaluationResult>> agentGrouped = allResults.stream()
                .filter(r -> r.getAgentId() != null)
                .collect(Collectors.groupingBy(EvaluationResult::getAgentId));
        for (Long agentId : task.getAgentIds() != null ? task.getAgentIds() : List.<Long>of()) {
            List<EvaluationResult> ar = agentGrouped.getOrDefault(agentId, List.of());
            result.add(buildAgentResult(agentId, ar, task.getDimensions()));
        }

        // LLM 结果聚合
        Map<Long, List<EvaluationResult>> llmGrouped = allResults.stream()
                .filter(r -> r.getLlmId() != null)
                .collect(Collectors.groupingBy(EvaluationResult::getLlmId));
        for (Long llmId : task.getLlmIds() != null ? task.getLlmIds() : List.<Long>of()) {
            List<EvaluationResult> lr = llmGrouped.getOrDefault(llmId, List.of());
            result.add(buildLLMResult(llmId, lr, task.getDimensions()));
        }

        return result;
    }

    /** 构建单个 Agent 的聚合结果 */
    private AgentResultVO buildAgentResult(Long agentId, List<EvaluationResult> results,
                                           List<DimensionConfig> dimensions) {
        Agent agent = agentMapper.selectById(agentId);
        return buildTargetResult(agentId, agent != null ? agent.getName() : "未知Agent", results, dimensions);
    }

    /** 构建单个 LLM 的聚合结果 */
    private AgentResultVO buildLLMResult(Long llmId, List<EvaluationResult> results,
                                          List<DimensionConfig> dimensions) {
        LLM llm = llmMapper.selectById(llmId);
        return buildTargetResult(llmId, llm != null ? llm.getName() : "未知LLM", results, dimensions);
    }

    /** 构建聚合结果的通用方法 */
    private AgentResultVO buildTargetResult(Long targetId, String targetName,
                                             List<EvaluationResult> results,
                                             List<DimensionConfig> dimensions) {
        AgentResultVO vo = new AgentResultVO();
        vo.setAgentId(targetId);
        vo.setAgentName(targetName);
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
        item.setTurnOrder(r.getTurnOrder());
        item.setScore(r.getOverallScore());
        item.setPassed(r.getPassed());
        item.setLatencyMs(r.getLatencyMs());
        item.setTokensUsed(r.getTokensUsed());
        // Agent 原文（截断前500字展示）
        String agentResp = r.getAgentResponse();
        item.setRawResponse(agentResp != null && agentResp.length() > 500
                ? agentResp.substring(0, 500) + "…" : agentResp);
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
