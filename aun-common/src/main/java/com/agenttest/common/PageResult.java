package com.agenttest.common;

import java.util.List;

/**
 * 分页结果 — 封装 MyBatis-Plus 分页查询结果，统一返回给前端。
 * 前端可直接用 data / total / page / pageSize 渲染分页控件。
 *
 * @param <T> 列表元素类型
 */
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

    // ==================== getters / setters ====================

    public List<T> getData() { return data; }
    public void setData(List<T> data) { this.data = data; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}
