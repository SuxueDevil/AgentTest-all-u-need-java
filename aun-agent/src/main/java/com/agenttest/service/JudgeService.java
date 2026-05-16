package com.agenttest.service;

import com.agenttest.pojo.JudgeVerdict;

import java.util.Map;

/**
 * Judge 评分服务接口 — 调用 LLM-as-Judge 对 Agent 回答进行多维度评分。
 */
public interface JudgeService {

    /**
     * 调用 Judge LLM 进行多维度评分。
     *
     * @param question       用户问题文本
     * @param expectedAnswer 期望答案（可为 null）
     * @param agentResponse  Agent 的实际回答
     * @param criteria       Agent 评估标准（角色定义 + 期望回答风格）
     * @param dimensions     评测维度配置（name → weight/threshold）
     * @return JudgeVerdict 评分结果，调用失败时返回空结果（score=0 + error feedback）
     */
    JudgeVerdict evaluate(String question, String expectedAnswer,
                          String agentResponse, String criteria,
                          Map<String, String> dimensions);
}
