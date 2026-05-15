package com.agenttest.pojo.dto;

/**
 * 问题列表查询参数 — Controller 通过 Query String 自动绑定。
 * <p>
 * 支持按 keyword（模糊匹配 title）、category、difficulty、questionType 筛选，
 * 未传的字段不参与筛选，page 默认 1，pageSize 默认 20。
 */
public class QuestionQueryDTO {

    /** 页码，从 1 开始，默认 1 */
    private Integer page = 1;

    /** 每页条数，默认 20 */
    private Integer pageSize = 20;

    /** 搜索关键词，模糊匹配 title */
    private String keyword;

    /** 按分类筛选: reasoning / coding / qa / translation / summarization */
    private String category;

    /** 按难度筛选: easy / medium / hard */
    private String difficulty;

    /** 按类型筛选: single / multi */
    private String questionType;

    // ==================== getters / setters ====================

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
}
