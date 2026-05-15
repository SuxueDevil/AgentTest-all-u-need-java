package com.agenttest.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 任务进度视图 — GET /api/evaluation/:id/progress 的返回值。
 * <p>
 * 前端通过 3s 轮询获取此对象，展示进度条和状态。
 */
@Data
@AllArgsConstructor
public class TaskProgressVO {

    /** 当前任务状态: pending / running / completed / cancelled / failed */
    private String status;

    /** 题目总数 */
    private Integer questionCount;

    /** 已完成数 */
    private Integer completedCount;
}
