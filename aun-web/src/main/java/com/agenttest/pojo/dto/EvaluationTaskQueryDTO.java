package com.agenttest.pojo.dto;

import lombok.Data;

/**
 * 评测任务列表查询参数 — Controller 通过 Query String 自动绑定。
 * <p>
 * page 默认 1，pageSize 默认 20；status 为空时不筛选。
 */
@Data
public class EvaluationTaskQueryDTO {

    /** 页码，从 1 开始，默认 1 */
    private Integer page = 1;

    /** 每页条数，默认 20 */
    private Integer pageSize = 20;

    /** 按状态筛选: pending / running / completed / cancelled / failed */
    private String status;
}
