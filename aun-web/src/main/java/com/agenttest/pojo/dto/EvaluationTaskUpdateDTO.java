package com.agenttest.pojo.dto;

import lombok.Data;

/**
 * 评测任务更新请求 — Controller 接收的 JSON 入参，支持部分字段更新。
 * <p>
 * 仅允许修改 name 和 description，任务启动后不允许修改题目、Agent 和维度配置。
 */
@Data
public class EvaluationTaskUpdateDTO {

    /** 任务名称 */
    private String name;

    /** 任务描述 */
    private String description;
}
