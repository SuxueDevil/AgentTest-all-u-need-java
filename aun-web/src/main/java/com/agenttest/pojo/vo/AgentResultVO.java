package com.agenttest.pojo.vo;

import lombok.Data;

import java.util.List;

/**
 * Agent 评测结果视图 — GET /api/evaluation/:id/results 的返回值元素。
 * <p>
 * 按 Agent 分组，包含 overallScore、各维度聚合得分、逐题明细。
 */
@Data
public class AgentResultVO {

    /** Agent ID */
    private Long agentId;
    /** Agent 名称 */
    private String agentName;
    /** 综合加权得分（0~1），所有维度加权平均 */
    private Double overallScore;
    /** 是否通过 */
    private Boolean passed;
    /** 平均延迟（毫秒） */
    private Long avgLatencyMs;
    /** 总消耗 token 数 */
    private Integer totalTokens;
    /** 各维度聚合得分 */
    private List<DimensionScoreVO> dimensionScores;
    /** 逐题结果明细 */
    private List<ResultItemVO> items;

    @Data
    public static class DimensionScoreVO {
        /** 维度标识 */
        private String dimensionName;
        /** 聚合得分 0~1 */
        private Double score;
        /** 聚合反馈 */
        private String feedback;
    }

    @Data
    public static class ResultItemVO {
        /** 问题 ID */
        private Long questionId;
        /** 问题标题 */
        private String questionTitle;
        /** 该题综合得分 */
        private Double score;
        /** 是否通过 */
        private Boolean passed;
        /** 响应延迟（毫秒） */
        private Integer latencyMs;
        /** 消耗 token 数 */
        private Integer tokensUsed;
        /** Agent 原始回答 */
        private String rawResponse;
        /** 该题各维度得分明细（含 Judge 反馈理由） */
        private List<DimensionScoreVO> dimensionScores;
    }
}
