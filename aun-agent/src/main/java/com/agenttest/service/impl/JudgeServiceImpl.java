package com.agenttest.service.impl;

import com.agenttest.pojo.JudgeVerdict;
import com.agenttest.service.JudgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Judge 评分服务实现 — 构建评分 Prompt，调用 LLM-as-Judge，解析结构化结果。
 * <p>
 * 使用 BeanOutputConverter 将 LLM 返回的 JSON 自动映射为 JudgeVerdict Record。
 * temperature=0.1 保证评判一致性；调用失败时返回 score=0 + error feedback。
 */
@Service
public class JudgeServiceImpl implements JudgeService {

    private static final Logger log = LoggerFactory.getLogger(JudgeServiceImpl.class);

    private final ChatClient judgeClient;
    private final BeanOutputConverter<JudgeVerdict> outputConverter;

    /** 构造器注入 JudgeConfig 提供的 ChatClient Bean */
    public JudgeServiceImpl(ChatClient judgeClient) {
        this.judgeClient = judgeClient;
        this.outputConverter = new BeanOutputConverter<>(JudgeVerdict.class);
    }

    @Override
    public JudgeVerdict evaluate(String question, String expectedAnswer,
                                  String agentResponse, String criteria,
                                  Map<String, String> dimensions) {
        String prompt = buildPrompt(question, expectedAnswer, agentResponse,
                criteria, dimensions);
        try {
            String content = judgeClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (content == null || content.isBlank()) {
                throw new RuntimeException("Judge返回空响应");
            }

            JudgeVerdict verdict = outputConverter.convert(content);
            log.info("Judge评分完成: overall={}", verdict.overall());
            return verdict;
        } catch (Exception e) {
            log.error("Judge评分失败: {}", e.getMessage());
            return fallbackVerdict(dimensions, e.getMessage());
        }
    }

    /**
     * 构建评分 Prompt。
     * 包含：Agent 角色定义、用户问题、期望答案、Agent 回答、评分维度、JSON 格式要求。
     */
    private String buildPrompt(String question, String expectedAnswer,
                                String agentResponse, String criteria,
                                Map<String, String> dimensions) {
        String dimList = dimensions.entrySet().stream()
                .map(e -> "- " + e.getKey() + "：" + e.getValue())
                .collect(Collectors.joining("\n"));

        String truncated = agentResponse != null && agentResponse.length() > 4096
                ? agentResponse.substring(0, 4096) + "…(已截断)"
                : agentResponse;

        String schema = outputConverter.getJsonSchema();

        return """
               你是一个专业的 AI 评测裁判。请根据以下信息对 Agent 的回答进行多维度评分。

               【Agent 角色定义】
               %s

               【用户问题】
               %s

               【期望答案】
               %s

               【Agent 回答】
               %s

               【评分维度及说明】
               %s

               请用 0.0~1.0 为每个维度打分，1.0 代表完美满足角色要求和期望答案。
               每个维度的 feedback 用 1~2 句话解释评分理由。

               请严格按以下 JSON Schema 返回：
               %s
               """.formatted(
                criteria != null ? criteria : "通用 AI 助手",
                question != null ? question : "",
                expectedAnswer != null ? expectedAnswer : "无期望答案",
                truncated != null ? truncated : "",
                dimList,
                schema
        );
    }

    /** 调用失败时返回的全 0 结果，不阻断评测流程 */
    private JudgeVerdict fallbackVerdict(Map<String, String> dimensions, String errorMsg) {
        List<JudgeVerdict.DimensionVerdict> dims = dimensions.keySet().stream()
                .map(name -> new JudgeVerdict.DimensionVerdict(name, 0.0, "评分失败: " + errorMsg))
                .collect(Collectors.toList());
        return new JudgeVerdict(dims, 0.0);
    }
}
