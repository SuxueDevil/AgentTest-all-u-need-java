package com.agenttest.common;

import lombok.Data;

import java.util.List;

/**
 * 分页结果 — 封装 MyBatis-Plus 分页查询结果，统一返回给前端。
 * 前端可直接用 data / total / page / pageSize 渲染分页控件。
 *
 * @param <T> 列表元素类型
 */
@Data
public class PageResult<T> {

    /** 当前页数据列表 */
    private List<T> data;
    /** 符合条件的总记录数 */
    private long total;
    /** 当前页码（从 1 开始） */
    private int page;
    /** 每页大小 */
    private int pageSize;

    /**
     * @param data     当前页数据
     * @param total    总记录数
     * @param page     当前页码
     * @param pageSize 每页大小
     */
    public PageResult(List<T> data, long total, int page, int pageSize) {
        this.data = data;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }
}
