package com.agenttest.pojo.dto;

import com.agenttest.pojo.entity.Question;
import lombok.Data;

import java.util.List;

/**
 * 问题更新请求 — 所有字段可选，前端传哪些就更新哪些。
 * 与 CreateDTO 的区别是不做 @NotBlank 校验，允许部分更新。
 */
@Data
public class QuestionUpdateDTO {

    /** 问题标题 */
    private String title;
    /** 分类: reasoning / coding / qa / translation / summarization */
    private String category;
    /** 难度: easy / medium / hard */
    private String difficulty;
    /** 问题类型: single / multi */
    private String questionType;
    /** 多轮对话内容（传 null 表示不修改 turns） */
    private List<Question.Turn> turns;
    /** 期望答案（传 null 表示不修改） */
    private String expectedAnswer;
    /** 标签列表（传 null 表示不修改） */
    private List<String> tags;
}
