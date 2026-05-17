package com.agenttest.pojo.dto;

import com.agenttest.pojo.entity.EvaluationTask.DimensionConfig;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 评测任务创建请求 — Controller 接收的 JSON 入参。
 * <p>
 * name 和 questionIds 必填；agentIds 至少选一个；dimensions 至少配置一个维度。
 */
@Data
public class EvaluationTaskCreateDTO {

    /** 任务名称（必填） */
    @NotBlank(message = "任务名称不能为空")
    private String name;

    /** 任务描述 */
    private String description;

    /** 题目 ID 列表（必填，至少选一题） */
    @NotEmpty(message = "请至少选择一道题目")
    private List<Long> questionIds;

    /** 参评 Agent ID 列表（可选） */
    private List<Long> agentIds;

    /** 参评 LLM ID 列表（可选） */
    private List<Long> llmIds;

    /** 评测维度配置（必填，至少一个维度） */
    @NotEmpty(message = "请至少配置一个评测维度")
    private List<DimensionConfig> dimensions;
}
