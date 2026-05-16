package com.agenttest.pojo.dto;

import com.agenttest.pojo.entity.Question;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 问题创建请求 — Controller 接收的入参。
 * <p>
 * title 和 category 为必填，questionType 未传时默认 single。
 * 多轮问题时需传入 turns 数组，每项含 turnOrder / role / content。
 */
@Data
public class QuestionCreateDTO {

    /** 问题标题（必填） */
    @NotBlank(message = "问题标题不能为空")
    private String title;

    /** 分类（必填）: reasoning / coding / qa / translation / summarization */
    @NotBlank(message = "问题分类不能为空")
    private String category;

    /** 难度: easy / medium / hard，默认 medium */
    private String difficulty;

    /** 问题类型: single / multi，默认 single */
    private String questionType;

    /** 多轮对话内容，单轮时可为空或单元素数组 */
    private List<Question.Turn> turns;

    /** 期望答案（评分参考） */
    private String expectedAnswer;

    /** 标签列表 */
    private List<String> tags;
}
