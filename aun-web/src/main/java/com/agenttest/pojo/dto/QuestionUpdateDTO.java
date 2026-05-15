package com.agenttest.pojo.dto;

import com.agenttest.pojo.entity.Question;
import java.util.List;

/**
 * 问题更新请求 — 所有字段可选，前端传哪些就更新哪些。
 * 与 CreateDTO 的区别是不做 @NotBlank 校验，允许部分更新。
 */
public class QuestionUpdateDTO {

    private String title;
    private String category;
    private String difficulty;
    private String questionType;
    /** 多轮对话内容（传 null 表示不修改 turns） */
    private List<Question.Turn> turns;
    /** 期望答案（传 null 表示不修改） */
    private String expectedAnswer;
    private List<String> tags;

    // ==================== getters / setters ====================

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
}
