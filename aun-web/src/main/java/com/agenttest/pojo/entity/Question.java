package com.agenttest.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 题库 — 数据库实体，映射 question 表。
 * <p>
 * turns 为多轮对话 JSON 数组: [{turnOrder, role, content}]，
 * tags 为 JSON 字符串数组，两个字段均使用 JacksonTypeHandler 自动序列化/反序列化。
 */
@TableName(value = "question", autoResultMap = true)
public class Question {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 问题标题（必填，最长 300 字符） */
    private String title;

    /** 分类: reasoning / coding / qa / translation / summarization */
    private String category;

    /** 难度: easy / medium / hard */
    private String difficulty;

    /** 问题类型: single（单轮）/ multi（多轮） */
    private String questionType;

    /**
     * 多轮对话内容，JSON 数组 [{turnOrder, role, content}]。
     * 单轮问题时 turns 为空或单元素数组。
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Turn> turns;

    /** 期望答案，作为评分参考 */
    private String expectedAnswer;

    /** 标签列表，JSON 字符串数组 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    /** 创建时间，数据库自动填充 */
    private LocalDateTime createdAt;

    /** 更新时间，数据库自动更新 */
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
    public List<Turn> getTurns() { return turns; }
    public void setTurns(List<Turn> turns) { this.turns = turns; }
    public String getExpectedAnswer() { return expectedAnswer; }
    public void setExpectedAnswer(String expectedAnswer) { this.expectedAnswer = expectedAnswer; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    /**
     * 多轮对话中的单轮 — JSON 序列化的内嵌对象。
     * <p>
     * turnOrder: 轮次序号（1-based）；role: user / assistant；content: 消息文本。
     */
    public static class Turn {
        /** 轮次序号，从 1 开始 */
        private Integer turnOrder;
        /** 角色: user / assistant */
        private String role;
        /** 消息内容 */
        private String content;

        public Integer getTurnOrder() { return turnOrder; }
        public void setTurnOrder(Integer turnOrder) { this.turnOrder = turnOrder; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}
