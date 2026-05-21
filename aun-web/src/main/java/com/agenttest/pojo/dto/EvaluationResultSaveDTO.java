package com.agenttest.pojo.dto;

import com.agenttest.engine.AgentHttpClient.AgentResponse;
import com.agenttest.pojo.entity.EvaluationTask.DimensionConfig;
import com.agenttest.pojo.entity.Question;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 评测结果保存入参 — 将 EvaluationEngine.saveResult() 的 10 个裸参数封装为单一 DTO。
 * <p>
 * Agent 和 LLM 评测共用此 DTO，通过 agentId / llmId 区分评测目标类型。
 * agentId 和 llmId 互斥：Agent 评测时 agentId 有值 llmId 为 null，LLM 评测时相反。
 */
@Data
public class EvaluationResultSaveDTO {

    /** Agent/LLM HTTP 响应（content、latencyMs、tokensUsed 等） */
    private AgentResponse resp;

    /** 所属任务 ID */
    private Long taskId;

    /** 评测批次号 */
    private int run;

    /** Agent ID（LLM 评测时为 null） */
    private Long agentId;

    /** LLM ID（Agent 评测时为 null） */
    private Long llmId;

    /** 评测标准文本（Agent 用 description，LLM 用 "LLM模型 xxx"） */
    private String criteria;

    /** 题目 entity */
    private Question question;

    /** 维度配置列表 */
    private List<DimensionConfig> dimensions;

    /** 维度说明 Map（供 Judge 提示词使用） */
    private Map<String, String> dimDesc;

    /** 轮次序号（多轮时 1-based，单轮时 null） */
    private Integer turnOrder;
}
