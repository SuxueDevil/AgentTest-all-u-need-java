package com.agenttest.service.impl;

import com.agenttest.pojo.dto.JudgeRequest;
import com.agenttest.pojo.vo.JudgeVerdict;
import com.agenttest.service.JudgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class JudgeServiceImpl implements JudgeService {

    private static final Logger log = LoggerFactory.getLogger(JudgeServiceImpl.class);

    private final ChatClient judgeClient;
    /** LLM 结构化输出转换器，将 JSON 自动映射为 JudgeVerdict Record */
    private final BeanOutputConverter<JudgeVerdict> outputConverter = new BeanOutputConverter<>(JudgeVerdict.class);

    /**
     * 调用 Judge LLM 进行多维度评分。
     * <p>
     * 优先用 BeanOutputConverter 解析，失败时手动提取 JSON 兜底。
     * 调用失败时返回 score=0 + error feedback，不抛异常阻断评测流程。
     */
    @Override
    public JudgeVerdict evaluate(JudgeRequest request) {
        String prompt = buildPrompt(request.getQuestion(), request.getExpectedAnswer(),
                request.getAgentResponse(), request.getCriteria(), request.getDimensions());
        try {
            String content = judgeClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (content == null || content.isBlank()) {
                throw new RuntimeException("Judge返回空响应");
            }

            // 先提取 JSON — LLM 可能会加 markdown 或说明文字
            content = extractJson(content);

            JudgeVerdict verdict = outputConverter.convert(content);
            log.info("Judge评分完成: overall={}", verdict.overall());
            return verdict;
        } catch (Exception e) {
            log.error("Judge评分失败: {}", e.getMessage());
            return fallbackVerdict(request.getDimensions(), e.getMessage());
        }
    }

    /** 手动从 LLM 返回文本中提取 JSON 并映射为 JudgeVerdict，容错字段顺序和多余文本 */
    private JudgeVerdict parseManually(String content, Map<String, String> dimensions) {
        String json = extractJson(content);
        try {
            Map<String, Object> map = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(json, new com.fasterxml.jackson.core.type.TypeReference<>() {});
            double overall = ((Number) map.getOrDefault("overall", 0.0)).doubleValue();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> dims = (List<Map<String, Object>>) map.get("dimensions");
            List<JudgeVerdict.DimensionVerdict> verdicts = new java.util.ArrayList<>();
            if (dims != null) {
                for (Map<String, Object> d : dims) {
                    String name = (String) d.get("name");
                    double score = ((Number) d.getOrDefault("score", 0.0)).doubleValue();
                    String feedback = (String) d.getOrDefault("feedback", "");
                    verdicts.add(new JudgeVerdict.DimensionVerdict(name, score, feedback));
                }
            }
            return new JudgeVerdict(verdicts, overall);
        } catch (Exception e) {
            throw new RuntimeException("手动JSON解析失败: " + e.getMessage());
        }
    }

    /** 从 LLM 返回内容中提取 JSON 片段 */
    private String extractJson(String content) {
        // 去掉 markdown 代码块
        String cleaned = content
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();
        // 找第一个 { 到最后一个 }
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }

    /**
     * 构建评分 Prompt — 给模板示例而非 JSON Schema，避免 LLM 原样输出 Schema。
     */
    private String buildPrompt(String question, String expectedAnswer,
                                String agentResponse, String criteria,
                                Map<String, String> dimensions) {
        String dimList = dimensions.entrySet().stream()
                .map(e -> "- " + e.getKey() + "：" + e.getValue())
                .collect(Collectors.joining("\n"));

        String dimTemplate = dimensions.keySet().stream()
                .map(name -> "    {\"name\": \"" + name + "\", \"score\": 0.0, \"feedback\": \"评分理由\"}")
                .collect(Collectors.joining(",\n"));

        String truncated = agentResponse != null && agentResponse.length() > 4096
                ? agentResponse.substring(0, 4096) + "…(已截断)"
                : agentResponse;

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
               只返回 JSON，不要额外说明。

               返回格式示例：
               {
                 "dimensions": [
               %s
                 ],
                 "overall": 0.0
               }
               """.formatted(
                criteria != null ? criteria : "通用 AI 助手",
                question != null ? question : "",
                expectedAnswer != null ? expectedAnswer : "无期望答案",
                truncated != null ? truncated : "",
                dimList,
                dimTemplate
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
