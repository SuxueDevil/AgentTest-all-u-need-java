package com.agenttest.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 单题评测结果 — 数据库实体，映射 evaluation_result 表。
 * <p>
 * 每条记录代表"某个 Agent 对某道题的评测结果"，含维度得分、延迟、token 消耗等。
 * dimensionScores 和 rawRequest 为 JSON 列，使用 JacksonTypeHandler 自动序列化。
 */
@Data
@TableName(value = "evaluation_result", autoResultMap = true)
public class EvaluationResult {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属任务 ID，外键关联 evaluation_task.id */
    private Long taskId;

    /** Agent ID，外键关联 agent.id（LLM 评测时为空） */
    private Long agentId;

    /** LLM ID，外键关联 llm.id（Agent 评测时为空） */
    private Long llmId;

    /** 问题 ID，外键关联 question.id */
    private Long questionId;

    /** 所属批次号，对应 evaluation_task.run */
    private Integer run;

    /**
     * 多轮对话轮次序号（1-based），单轮题目为 null。
     * 一条多轮题目会产生多条结果记录，按 turnOrder 区分。
     */
    private Integer turnOrder;

    /** 综合加权得分（0~1），由各维度得分按权重计算 */
    private Double overallScore;

    /** 是否通过（overallScore >= 任务设定的阈值） */
    private Boolean passed;

    /** 响应延迟（毫秒） */
    private Integer latencyMs;

    /** 消耗 token 数 */
    private Integer tokensUsed;

    /** 维度得分明细 [{dimensionName, score, feedback}] */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<DimensionScore> dimensionScores;

    /** 原始请求 JSON（发送给 Agent 的完整请求体） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object rawRequest;

    /** Agent 回答内容（从 SSE 流拼接或 JSON 提取后的纯文本） */
    private String agentResponse;

    /** Agent 原始响应文本（SSE dump 或 JSON 原文，截断存储） */
    private String rawResponse;

    /** 创建时间，数据库自动填充 */
    private LocalDateTime createdAt;

    /**
     * 维度得分 — dimension_scores JSON 数组的内嵌对象。
     * <p>
     * dimensionName: 维度标识；score: 得分（0~1）；feedback: 评判理由。
     */
    @Data
    public static class DimensionScore {
        /** 维度标识，如 accuracy */
        private String dimensionName;
        /** 得分 0~1 */
        private Double score;
        /** 评判反馈 */
        private String feedback;
    }
}
