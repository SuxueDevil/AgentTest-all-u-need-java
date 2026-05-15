package com.agenttest.pojo.vo;

import com.agenttest.pojo.entity.Question;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 问题视图对象 — Controller 返回给前端的问题数据。
 * <p>
 * 与 entity 字段基本一致（问题数据无敏感字段），
 * 由 Service 层通过 BeanUtil.copyProperties 从 entity 转换。
 */
public class QuestionVO {

    private Long id;
    private String title;
    private String category;
    private String difficulty;
    private String questionType;
    /** 多轮对话内容 */
    private List<Question.Turn> turns;
    /** 期望答案 */
    private String expectedAnswer;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ==================== getters / setters ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    public List<Question.Turn> getTurns() { return turns; }
    public void setTurns(List<Question.Turn> turns) { this.turns = turns; }
    public String getExpectedAnswer() { return expectedAnswer; }
    public void setExpectedAnswer(String expectedAnswer) { this.expectedAnswer = expectedAnswer; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
