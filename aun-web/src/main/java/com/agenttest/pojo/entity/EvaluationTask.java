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
 * 评测任务 — 数据库实体，映射 evaluation_task 表。
 * <p>
 * questionIds / agentIds / dimensions 三个 JSON 列使用 JacksonTypeHandler 自动序列化。
 * status 枚举: pending → running → completed / cancelled / failed。
 */
@Data
@TableName(value = "evaluation_task", autoResultMap = true)
public class EvaluationTask {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务名称（必填，最长 200 字符） */
    private String name;

    /** 任务描述 */
    private String description;

    /** 题目 ID 列表，JSON 数组 [1, 2, 3] */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> questionIds;

    /** 参评 Agent ID 列表，JSON 数组 [1, 2] */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> agentIds;

    /** 评测维度配置 [{name, displayName, weight, threshold}] */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<DimensionConfig> dimensions;

    /** 题目总数，创建时根据 questionIds 计算 */
    private Integer questionCount;

    /** 已完成评测数，执行过程中递增 */
    private Integer completedCount;

    /** 任务状态: pending / running / completed / cancelled / failed */
    private String status;

    /** 评测批次号，每次重新开始自增，用于区分不同轮次的结果 */
    private Integer run;

    /** 创建时间，数据库自动填充 */
    private LocalDateTime createdAt;

    /** 开始执行时间 */
    private LocalDateTime startedAt;

    /** 完成时间 */
    private LocalDateTime completedAt;

    /**
     * 评测维度配置 — dimensions JSON 数组的内嵌对象。
     * <p>
     * name: 维度标识（如 accuracy）；displayName: 显示名称（如 准确性）；
     * weight: 权重（0~1，所有维度权重之和为 1）；threshold: 通过阈值（0~1）。
     */
    @Data
    public static class DimensionConfig {
        /** 维度标识，如 accuracy */
        private String name;
        /** 显示名称，如 准确性 */
        private String displayName;
        /** 权重 0~1 */
        private Double weight;
        /** 通过阈值 0~1 */
        private Double threshold;
    }
}
