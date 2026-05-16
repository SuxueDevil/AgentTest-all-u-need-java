package com.agenttest.pojo.dto;

import lombok.Data;

import java.util.Map;

/**
 * Judge 评分请求 DTO — 封装 LLM-as-Judge 所需的全部入参。
 * <p>
 * 包含被测 Agent 的回答、题目信息、Agent 角色标准、评分维度。
 */
@Data
public class JudgeRequest {

    /** 用户问题 */
    private String question;

    /** 期望答案（可为空） */
    private String expectedAnswer;

    /** Agent 的实际回答 */
    private String agentResponse;

    /** Agent 角色定义与评估标准 */
    private String criteria;

    /** 评分维度 Map（name → 说明） */
    private Map<String, String> dimensions;
}
