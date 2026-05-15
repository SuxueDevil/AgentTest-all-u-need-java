package com.agenttest.pojo.dto;

import lombok.Data;

/**
 * AI 题目生成请求参数 — 封装调用 QuestionGenerator 所需的全部入参。
 * <p>
 * 由 Controller 层接收前端 JSON 后构造，经 Service 层透传至 QuestionGenerator。
 * 各字段均有默认值，前端可省略。
 */
@Data
public class QuestionGenerateDTO {

    /** 题目分类: reasoning / coding / qa / translation / summarization，默认 reasoning */
    private String category = "reasoning";

    /** 难度等级: easy / medium / hard，默认 medium */
    private String difficulty = "medium";

    /** 题目类型: single（单轮） / multi（多轮），默认 single */
    private String questionType = "single";

    /** 生成数量 1~20，默认 5，Controller 层负责截断上限 */
    private int count = 5;

    /** 主题场景（可选），如"医疗问诊"，为空时不注入 Prompt */
    private String topic;
}
