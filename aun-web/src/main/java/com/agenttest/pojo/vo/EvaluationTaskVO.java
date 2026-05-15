package com.agenttest.pojo.vo;

import com.agenttest.pojo.entity.EvaluationTask.DimensionConfig;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评测任务视图对象 — Controller 返回给前端的任务数据。
 * <p>
 * 字段与 entity 基本一致，不含敏感信息。
 */
@Data
public class EvaluationTaskVO {

    /** 主键 ID */
    private Long id;
    /** 任务名称 */
    private String name;
    /** 任务描述 */
    private String description;
    /** 题目 ID 列表 */
    private List<Long> questionIds;
    /** 参评 Agent ID 列表 */
    private List<Long> agentIds;
    /** 参评 LLM ID 列表 */
    private List<Long> llmIds;
    /** 评测维度配置 */
    private List<DimensionConfig> dimensions;
    /** 题目总数 */
    private Integer questionCount;
    /** 已完成数 */
    private Integer completedCount;
    /** 任务状态 */
    private String status;
    /** 评测批次号 */
    private Integer run;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 开始时间 */
    private LocalDateTime startedAt;
    /** 完成时间 */
    private LocalDateTime completedAt;
}
