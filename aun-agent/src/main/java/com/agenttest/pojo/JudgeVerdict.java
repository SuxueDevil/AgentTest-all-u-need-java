package com.agenttest.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Judge LLM 评分结果 — Spring AI 结构化输出直接映射到此 Record。
 * <p>
 * dimensions 包含每个维度的得分和反馈，overall 为加权综合分（0~1）。
 * 所有字段标记为 required，确保 Judge 模型输出完整。
 */
public record JudgeVerdict(
    @JsonProperty(required = true)
    List<DimensionVerdict> dimensions,

    @JsonProperty(required = true)
    double overall
) {
    /** 单维度评分 */
    public record DimensionVerdict(
        @JsonProperty(required = true)
        String name,

        @JsonProperty(required = true)
        double score,

        @JsonProperty(required = true)
        String feedback
    ) {}
}
