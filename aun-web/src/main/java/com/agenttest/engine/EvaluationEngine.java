package com.agenttest.engine;

import com.agenttest.engine.AgentHttpClient.AgentResponse;
import com.agenttest.mapper.AgentMapper;
import com.agenttest.mapper.LLMMapper;
import com.agenttest.mapper.EvaluationResultMapper;
import com.agenttest.mapper.EvaluationTaskMapper;
import com.agenttest.mapper.QuestionMapper;
import com.agenttest.pojo.dto.JudgeRequest;
import com.agenttest.pojo.entity.*;
import com.agenttest.pojo.entity.EvaluationTask.DimensionConfig;
import com.agenttest.pojo.entity.EvaluationResult.DimensionScore;
import com.agenttest.pojo.vo.JudgeVerdict;
import com.agenttest.service.JudgeService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 评测执行引擎 — 负责异步执行 Agent/LLM 评测、裁判、结果落库。
 * <p>
 * 从 {@link com.agenttest.service.impl.EvaluationServiceImpl} 中抽离，
 * 解决原 executeAsync 200+ 行方法中 Agent 和 LLM 两段代码大量重复的问题。
 * <p>
 * 核心流程: 预加载 → 并行发送 → Judge 裁判 → 落库 → 更新进度 → 收尾。
 * 线程池使用 evaluationExecutor（I/O 密集型，core=16 max=32 queue=200）。
 */
@Component
public class EvaluationEngine {

    private static final Logger log = LoggerFactory.getLogger(EvaluationEngine.class);

    private final Executor executor;
    private final EvaluationTaskMapper taskMapper;
    private final EvaluationResultMapper resultMapper;
    private final AgentMapper agentMapper;
    private final LLMMapper llmMapper;
    private final QuestionMapper questionMapper;
    private final AgentHttpClient httpClient;
    private final JudgeService judgeService;
    private final ObjectMapper objectMapper;

    /** 运行中任务的取消标志位，key=taskId, value=true=取消 */
    private final Map<Long, Boolean> cancelFlags = new ConcurrentHashMap<>();

    /** 构造器注入 */
    public EvaluationEngine(EvaluationTaskMapper taskMapper,
                            EvaluationResultMapper resultMapper,
                            AgentMapper agentMapper,
                            LLMMapper llmMapper,
                            QuestionMapper questionMapper,
                            AgentHttpClient httpClient,
                            JudgeService judgeService,
                            ObjectMapper objectMapper,
                            @Qualifier("evaluationExecutor") Executor executor) {
        this.taskMapper = taskMapper;
        this.resultMapper = resultMapper;
        this.agentMapper = agentMapper;
        this.llmMapper = llmMapper;
        this.questionMapper = questionMapper;
        this.httpClient = httpClient;
        this.judgeService = judgeService;
        this.objectMapper = objectMapper;
        this.executor = executor;
    }

    // ==================== 取消控制 ====================

    /** 设置取消标志，运行中的轮次检测到后立即中断 */
    public void cancel(Long taskId) {
        cancelFlags.put(taskId, true);
        log.info("评测引擎取消标志已设置，taskId={}", taskId);
    }

    /** 清除取消标志（新任务启动时清除残留标志） */
    public void clearCancelFlag(Long taskId) {
        cancelFlags.remove(taskId);
    }

    /** 检查当前轮是否已被取消 */
    private boolean isCancelled(Long taskId) {
        return Boolean.TRUE.equals(cancelFlags.get(taskId));
    }

    // ==================== 异步执行入口 ====================

    /**
     * 异步执行评测 — 遍历 agentIds × questionIds + llmIds × questionIds，
     * 并行发送 HTTP 请求、调用 Judge 裁判、写库更新进度。
     * <p>
     * 调用方负责将此方法提交到线程池（CompletableFuture.runAsync 或 @Async），
     * 否则会阻塞调用线程直到评测全部完成。
     *
     * @param taskId 任务 ID
     */
    public void execute(Long taskId) {
        EvaluationTask task = taskMapper.selectById(taskId);
        if (task == null) {
            log.warn("评测任务不存在，taskId={}", taskId);
            return;
        }

        List<Long> questionIds = task.getQuestionIds();
        List<Long> agentIds = task.getAgentIds();
        List<Long> llmIds = task.getLlmIds() != null ? task.getLlmIds() : List.of();
        List<DimensionConfig> dimensions = task.getDimensions();
        int run = task.getRun();

        // 预加载实体，避免并行时重复查库
        Map<Long, Agent> agentMap = preloadAgents(agentIds);
        Map<Long, LLM> llmMap = preloadLLMs(llmIds);
        Map<Long, Question> questionMap = preloadQuestions(questionIds);

        // 构建维度说明 Map（供 Judge 提示词使用）
        Map<String, String> dimDesc = buildDimDesc(dimensions);

        // 并行提交所有 Agent×Question 和 LLM×Question 的评测任务
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // Agent × Question
        for (Long agentId : agentIds) {
            for (Long questionId : questionIds) {
                if (isCancelled(taskId)) break;
                Agent agent = agentMap.get(agentId);
                Question question = questionMap.get(questionId);
                if (agent == null || question == null || agent.getEndpointUrl() == null) continue;

                futures.add(evaluateAgentPair(agent, question, taskId, run, dimensions, dimDesc));
            }
        }

        // LLM × Question
        for (Long llmId : llmIds) {
            LLM llm = llmMap.get(llmId);
            if (llm == null || llm.getEndpointUrl() == null) continue;
            for (Long questionId : questionIds) {
                if (isCancelled(taskId)) break;
                Question question = questionMap.get(questionId);
                if (question == null) continue;

                futures.add(evaluateLLMPair(llm, question, taskId, run, dimensions, dimDesc));
            }
        }

        // 等待全部完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 收尾 — 仅当未被取消时将状态置为 completed
        cancelFlags.remove(taskId);
        EvaluationTask latest = taskMapper.selectById(taskId);
        if (latest != null && "running".equals(latest.getStatus())) {
            latest.setStatus("completed");
            latest.setCompletedAt(LocalDateTime.now());
            taskMapper.updateById(latest);
            log.info("评测任务全部完成，id={}", taskId);
        }
    }

    // ==================== Agent / LLM 评测 ====================

    /**
     * 评测单个 Agent × Question 组合 — 提交到线程池并行执行。
     * <p>
     * 根据题目类型选择单轮或多轮发送方式，每轮应答依次 Judge → 落库 → 更新进度。
     */
    private CompletableFuture<Void> evaluateAgentPair(Agent agent, Question question,
                                                       Long taskId, int run,
                                                       List<DimensionConfig> dimensions,
                                                       Map<String, String> dimDesc) {
        return CompletableFuture.runAsync(() -> {
            if (isCancelled(taskId)) return;
            try {
                // 发送 HTTP 请求 — 多轮逐轮发送，单轮一次发送
                List<AgentResponse> responses = isMultiTurn(question)
                        ? httpClient.sendMultiTurn(agent, question)
                        : List.of(httpClient.send(agent, question));

                // 逐轮裁判 + 落库
                for (int turnIdx = 0; turnIdx < responses.size(); turnIdx++) {
                    if (isCancelled(taskId)) break;
                    saveResult(responses.get(turnIdx), taskId, run,
                            agent.getId(), null,
                            agent.getDescription(), question,
                            dimensions, dimDesc,
                            isMultiTurn(question) ? turnIdx + 1 : null);
                }

                log.info("评测完成: task={} agent={} question={} turns={}",
                        taskId, agent.getId(), question.getId(), responses.size());
            } catch (Exception e) {
                log.error("评测失败: task={} agent={} question={} error={}",
                        taskId, agent.getId(), question.getId(), e.getMessage());
            }
        }, executor);
    }

    /**
     * 评测单个 LLM × Question 组合 — 提交到线程池并行执行。
     */
    private CompletableFuture<Void> evaluateLLMPair(LLM llm, Question question,
                                                     Long taskId, int run,
                                                     List<DimensionConfig> dimensions,
                                                     Map<String, String> dimDesc) {
        return CompletableFuture.runAsync(() -> {
            if (isCancelled(taskId)) return;
            try {
                List<AgentResponse> responses = isMultiTurn(question)
                        ? httpClient.sendMultiTurnToLLM(llm, question)
                        : List.of(httpClient.sendToLLM(llm, question));

                for (int turnIdx = 0; turnIdx < responses.size(); turnIdx++) {
                    if (isCancelled(taskId)) break;
                    saveResult(responses.get(turnIdx), taskId, run,
                            null, llm.getId(),
                            "LLM模型 " + llm.getName(), question,
                            dimensions, dimDesc,
                            isMultiTurn(question) ? turnIdx + 1 : null);
                }

                log.info("评测完成: task={} llm={} question={} turns={}",
                        taskId, llm.getId(), question.getId(), responses.size());
            } catch (Exception e) {
                log.error("评测失败: task={} llm={} question={} error={}",
                        taskId, llm.getId(), question.getId(), e.getMessage());
            }
        }, executor);
    }

    // ==================== 结果落库（Agent / LLM 共用） ====================

    /**
     * 裁判 + 构造 EvaluationResult + 写库 + 更新进度。
     * <p>
     * Agent 和 LLM 共用此方法，通过 agentId/llmId 和 criteria 区分。
     *
     * @param resp       Agent/LLM 响应
     * @param taskId     任务 ID
     * @param run        批次号
     * @param agentId    Agent ID（LLM 评测时为 null）
     * @param llmId      LLM ID（Agent 评测时为 null）
     * @param criteria   评测标准文本
     * @param question   题目 entity
     * @param dimensions 维度配置列表
     * @param dimDesc    维度说明 Map
     * @param turnOrder  轮次序号（多轮时 1-based，单轮时 null）
     */
    private void saveResult(AgentResponse resp, Long taskId, int run,
                            Long agentId, Long llmId,
                            String criteria, Question question,
                            List<DimensionConfig> dimensions,
                            Map<String, String> dimDesc,
                            Integer turnOrder) {
        // 1. 调用 Judge 裁判
        JudgeRequest judgeReq = new JudgeRequest();
        judgeReq.setQuestion(question.getTitle());
        judgeReq.setExpectedAnswer(question.getExpectedAnswer());
        judgeReq.setAgentResponse(resp.content);
        judgeReq.setCriteria(criteria);
        judgeReq.setDimensions(dimDesc);
        JudgeVerdict verdict = judgeService.evaluate(judgeReq);

        // 2. 维度得分转换
        List<DimensionScore> dimensionScores = verdict.dimensions().stream()
                .map(dv -> {
                    DimensionScore ds = new DimensionScore();
                    ds.setDimensionName(dv.name());
                    ds.setScore(dv.score());
                    ds.setFeedback(dv.feedback());
                    return ds;
                }).collect(Collectors.toList());

        // 3. 综合分 + 通过判定
        double overall = verdict.overall();
        double avgThreshold = dimensions.stream()
                .mapToDouble(DimensionConfig::getThreshold)
                .average().orElse(0.5);

        // 4. 构造结果实体
        EvaluationResult result = new EvaluationResult();
        result.setTaskId(taskId);
        result.setAgentId(agentId);
        result.setLlmId(llmId);
        result.setQuestionId(question.getId());
        result.setOverallScore(overall);
        result.setRun(run);
        result.setPassed(overall >= avgThreshold);
        result.setLatencyMs(resp.latencyMs);
        result.setTokensUsed(resp.tokensUsed);
        result.setDimensionScores(dimensionScores);
        result.setRawRequest(parseJsonSafely(resp.rawRequest));
        result.setAgentResponse(resp.content);
        result.setRawResponse(resp.rawResponse);
        result.setTurnOrder(turnOrder);
        resultMapper.insert(result);

        // 5. 每完成一轮立刻 +1，前端 3s 轮询可看到进度逐步增长
        EvaluationTask latest = taskMapper.selectById(taskId);
        if (latest != null) {
            latest.setCompletedCount((latest.getCompletedCount() == null ? 0 : latest.getCompletedCount()) + 1);
            taskMapper.updateById(latest);
        }
    }

    // ==================== 预加载 ====================

    /** 预加载 Agent 实体到 Map，批量查询避免 N+1 */
    private Map<Long, Agent> preloadAgents(List<Long> agentIds) {
        Map<Long, Agent> map = new HashMap<>();
        for (Long id : agentIds) {
            Agent a = agentMapper.selectById(id);
            if (a != null) map.put(id, a);
        }
        return map;
    }

    /** 预加载 LLM 实体到 Map */
    private Map<Long, LLM> preloadLLMs(List<Long> llmIds) {
        Map<Long, LLM> map = new HashMap<>();
        for (Long id : llmIds) {
            LLM l = llmMapper.selectById(id);
            if (l != null) map.put(id, l);
        }
        return map;
    }

    /** 预加载 Question 实体到 Map */
    private Map<Long, Question> preloadQuestions(List<Long> questionIds) {
        Map<Long, Question> map = new HashMap<>();
        for (Long id : questionIds) {
            Question q = questionMapper.selectById(id);
            if (q != null) map.put(id, q);
        }
        return map;
    }

    // ==================== 工具方法 ====================

    /** 构建维度说明 Map — 供 Judge 提示词使用 */
    private Map<String, String> buildDimDesc(List<DimensionConfig> dimensions) {
        return dimensions.stream().collect(
                Collectors.toMap(DimensionConfig::getName,
                        d -> d.getDisplayName() + "(权重" + d.getWeight() + ",阈值" + d.getThreshold() + ")"));
    }

    /**
     * 判断题目是否为多轮（有 turns 数据且类型为 multi）。
     */
    private boolean isMultiTurn(Question q) {
        return "multi".equals(q.getQuestionType()) && q.getTurns() != null && !q.getTurns().isEmpty();
    }

    /**
     * 安全解析 JSON 字符串为 Object，避免 JacksonTypeHandler 双重序列化。
     * 解析失败时返回原字符串兜底。
     */
    private Object parseJsonSafely(String jsonStr) {
        if (jsonStr == null || jsonStr.isBlank()) return null;
        try {
            if (jsonStr.trim().startsWith("{")) {
                return objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
            } else if (jsonStr.trim().startsWith("[")) {
                return objectMapper.readValue(jsonStr, new TypeReference<List<Object>>() {});
            }
        } catch (Exception e) {
            log.warn("rawRequest JSON 解析失败，保留原字符串存储: {}", e.getMessage());
        }
        return jsonStr;
    }
}
