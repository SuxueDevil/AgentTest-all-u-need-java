package com.agenttest.service;

import com.agenttest.pojo.dto.JudgeRequest;
import com.agenttest.pojo.vo.JudgeVerdict;

/**
 * Judge 评分服务接口 — 调用 LLM-as-Judge 对 Agent 回答进行多维度评分。
 */
public interface JudgeService {

    /**
     * 调用 Judge LLM 进行多维度评分。
     *
     * @param request JudgeRequest（question, expectedAnswer, agentResponse, criteria, dimensions）
     * @return JudgeVerdict 评分结果，调用失败时返回空结果（score=0 + error feedback）
     */
    JudgeVerdict evaluate(JudgeRequest request);
}
