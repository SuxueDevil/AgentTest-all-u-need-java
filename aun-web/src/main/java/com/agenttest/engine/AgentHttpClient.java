package com.agenttest.engine;

import com.agenttest.pojo.entity.Agent;
import com.agenttest.pojo.entity.LLM;
import com.agenttest.pojo.entity.Question;
import com.agenttest.pojo.entity.Question.Turn;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent HTTP 客户端 — 向被测 Agent 发送评测请求并解析响应。
 * <p>
 * 单轮 send() 一次性发送全部 questions，
 * 多轮 sendMultiTurn() 逐轮迭代发送，每轮用前一轮的真实回答构建上下文。
 * 自动识别响应类型：SSE 流式（data: 开头）或普通 JSON（{ 开头）。
 */
@Component
public class AgentHttpClient {

    private static final Logger log = LoggerFactory.getLogger(AgentHttpClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /** 构造器注入 */
    public AgentHttpClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    // ==================== Agent 单轮 ====================

    /**
     * 向 Agent 发送单次评测请求（单轮题目或多轮迭代中的一个 turn）。
     *
     * @param agent    被测 Agent 实体
     * @param messages 本次请求的 messages 数组（由调用方组装好）
     * @return 响应结果
     */
    private AgentResponse execute(Agent agent, List<Map<String, String>> messages) {
        long start = System.currentTimeMillis();
        String bodyStr = buildRequestBody(agent, messages);

        try {
            return restTemplate.execute(agent.getEndpointUrl(), HttpMethod.POST,
                    req -> {
                        req.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                        applyAuth(req.getHeaders(), agent);
                        req.getBody().write(bodyStr.getBytes(StandardCharsets.UTF_8));
                    },
                    res -> parseResponse(res, agent, bodyStr, start));
        } catch (Exception e) {
            int latencyMs = (int) (System.currentTimeMillis() - start);
            log.error("Agent调用失败: {} - {}", agent.getName(), e.getMessage());
            return new AgentResponse("", 0, latencyMs, bodyStr, e.getMessage());
        }
    }

    /**
     * 向 Agent 发送评测请求（单轮题目入口）。
     * 【Java 类比】≈ AgentHttpClient.execute(agent, messages[])
     */
    public AgentResponse send(Agent agent, Question question) {
        return execute(agent, buildMessages(question));
    }

    // ==================== Agent 多轮迭代 ====================

    /**
     * 多轮迭代评测 — 仅发送 user 消息，逐轮用实际的 LLM 回答构建上下文。
     * 每轮独立 HTTP 调用，返回列表长度等于 user 消息数。
     *
     * @param agent    被测 Agent
     * @param question 多轮题目
     * @return 每轮一个 AgentResponse，顺序与 user 消息顺序一致
     */
    public List<AgentResponse> sendMultiTurn(Agent agent, Question question) {
        List<Turn> userTurns = getUserTurns(question);
        List<AgentResponse> results = new ArrayList<>();
        // 对话历史: 逐轮追加 user 消息 + 实际 LLM 回答
        List<Map<String, String>> history = new ArrayList<>();

        for (int i = 0; i < userTurns.size(); i++) {
            Turn turn = userTurns.get(i);

            // 追加当前 user 消息
            Map<String, String> userMsg = new LinkedHashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", turn.getContent());
            history.add(userMsg);

            // 发送完整对话历史（复制一份避免并发修改）
            AgentResponse resp = execute(agent, new ArrayList<>(history));
            results.add(resp);

            // 将 LLM 真实回答追加到历史，供下一轮上下文使用
            Map<String, String> assistantMsg = new LinkedHashMap<>();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", resp.content);
            history.add(assistantMsg);

            log.info("Agent多轮评测 turn={}/{} agent={} latency={}ms",
                    i + 1, userTurns.size(), agent.getName(), resp.latencyMs);
        }
        return results;
    }

    // ==================== LLM 单轮 ====================

    /**
     * 向 LLM 发送单次请求（单轮题目或多轮迭代中的一个 turn）。
     *
     * @param llm      LLM 实体
     * @param messages 消息数组
     * @return 响应结果
     */
    private AgentResponse executeLLM(LLM llm, List<Map<String, String>> messages) {
        long start = System.currentTimeMillis();
        String messagesJson;
        try {
            messagesJson = objectMapper.writeValueAsString(messages);
        } catch (Exception e) {
            return new AgentResponse("", 0, 0, "", "JSON序列化失败: " + e.getMessage());
        }
        String bodyStr = "{\"model\":\"" + llm.getModel()
                + "\",\"messages\":" + messagesJson + ",\"max_tokens\":1024}";

        try {
            log.info("LLM调用: {} model={} url={}", llm.getName(), llm.getModel(), llm.getEndpointUrl());
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            httpHeaders.setBearerAuth(llm.getApiKey());
            HttpEntity<String> entity = new HttpEntity<>(bodyStr, httpHeaders);
            ResponseEntity<String> response = restTemplate.exchange(
                    llm.getEndpointUrl(), HttpMethod.POST, entity, String.class);
            int latencyMs = (int) (System.currentTimeMillis() - start);
            String content = "";
            int tokensUsed = 0;
            if (response.getBody() != null) {
                Map<String, Object> respMap = objectMapper.readValue(response.getBody(), Map.class);
                content = extractByPath(respMap, "choices[0].message.content");
                Map<String, Object> usage = (Map<String, Object>) respMap.get("usage");
                if (usage != null) {
                    tokensUsed = ((Number) usage.getOrDefault("total_tokens", 0)).intValue();
                }
            }
            log.info("LLM调用完成: {} latency={}ms tokens={}", llm.getName(), latencyMs, tokensUsed);
            return new AgentResponse(content, tokensUsed, latencyMs, bodyStr,
                    response.getBody() != null ? response.getBody() : "");
        } catch (Exception e) {
            int latencyMs = (int) (System.currentTimeMillis() - start);
            log.error("LLM调用失败: {} - {}", llm.getName(), e.getMessage());
            return new AgentResponse("", 0, latencyMs, bodyStr, e.getMessage());
        }
    }

    /**
     * 向 LLM 发送评测请求（单轮题目入口）。
     */
    public AgentResponse sendToLLM(LLM llm, Question question) {
        return executeLLM(llm, buildMessages(question));
    }

    // ==================== LLM 多轮迭代 ====================

    /**
     * LLM 多轮迭代评测 — 逐轮发送 user 消息，用实际回答构建上下文。
     *
     * @param llm      LLM 实体
     * @param question 多轮题目
     * @return 每轮一个 AgentResponse
     */
    public List<AgentResponse> sendMultiTurnToLLM(LLM llm, Question question) {
        List<Turn> userTurns = getUserTurns(question);
        List<AgentResponse> results = new ArrayList<>();
        List<Map<String, String>> history = new ArrayList<>();

        for (int i = 0; i < userTurns.size(); i++) {
            Turn turn = userTurns.get(i);

            Map<String, String> userMsg = new LinkedHashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", turn.getContent());
            history.add(userMsg);

            AgentResponse resp = executeLLM(llm, new ArrayList<>(history));
            results.add(resp);

            Map<String, String> assistantMsg = new LinkedHashMap<>();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", resp.content);
            history.add(assistantMsg);

            log.info("LLM多轮评测 turn={}/{} llm={} latency={}ms",
                    i + 1, userTurns.size(), llm.getName(), resp.latencyMs);
        }
        return results;
    }

    // ==================== 响应解析 ====================

    /** LLM 用解析 — 固定标准 OpenAI 路径 */
    private AgentResponse parseResponse(ClientHttpResponse response,
                                         String bodyStr, long start) throws IOException {
        return parseResponseInternal(response, bodyStr, start,
                "choices[0].message.content");
    }

    /** Agent 用解析 — 按 Agent 配置路径提取 */
    private AgentResponse parseResponse(ClientHttpResponse response, Agent agent,
                                         String bodyStr, long start) throws IOException {
        String contentPath = (agent.getResponseContentPath() != null
                && !agent.getResponseContentPath().isBlank())
                ? agent.getResponseContentPath()
                : "choices[0].message.content";
        return parseResponseInternal(response, bodyStr, start, contentPath);
    }

    /** 解析响应 — 自动识别 SSE 或 JSON */
    @SuppressWarnings("unchecked")
    private AgentResponse parseResponseInternal(ClientHttpResponse response,
                                                 String bodyStr, long start,
                                                 String contentPath) throws IOException {
        int latencyMs = (int) (System.currentTimeMillis() - start);
        StringBuilder rawResponse = new StringBuilder();
        String content = "";
        int tokensUsed = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {

            boolean isSSE = response.getHeaders().getContentType() != null
                    && response.getHeaders().getContentType().toString().contains("text/event-stream");
            if (!isSSE) {
                reader.mark(10);
                String firstLine = reader.readLine();
                reader.reset();
                isSSE = firstLine != null && firstLine.startsWith("data:");
            }

            if (isSSE) {
                StringBuilder contentBuf = new StringBuilder();
                for (String line; (line = reader.readLine()) != null; ) {
                    if (rawResponse.length() < 2000) rawResponse.append(line).append("\n");
                    if (line.startsWith("data: ") && !line.equals("data: [DONE]")) {
                        String json = line.substring(6);
                        try {
                            Map<String, Object> chunk = objectMapper.readValue(json, Map.class);
                            String delta = extractByPath(chunk, "choices[0].delta.content");
                            if (delta != null) contentBuf.append(delta);
                            Map<String, Object> usage = (Map<String, Object>) chunk.get("usage");
                            if (usage != null) {
                                tokensUsed = ((Number) usage.getOrDefault("total_tokens", 0)).intValue();
                            }
                        } catch (Exception ignored) { /* 跳过无法解析的块 */ }
                    }
                }
                content = contentBuf.toString();
            } else {
                StringBuilder jsonBuf = new StringBuilder();
                for (String line; (line = reader.readLine()) != null; ) {
                    rawResponse.append(line).append("\n");
                    jsonBuf.append(line);
                }
                Map<String, Object> respMap = objectMapper.readValue(jsonBuf.toString(), Map.class);
                content = extractByPath(respMap, contentPath);
                Map<String, Object> usage = (Map<String, Object>) respMap.get("usage");
                if (usage != null) {
                    tokensUsed = ((Number) usage.getOrDefault("total_tokens", 0)).intValue();
                }
            }
        }

        log.info("HTTP调用完成: latency={}ms, tokens={}", latencyMs, tokensUsed);
        return new AgentResponse(content, tokensUsed, latencyMs, bodyStr,
                rawResponse.toString().trim());
    }

    // ==================== 构造请求 ====================

    /** 构造请求体 — 使用模板或默认 OpenAI 格式 */
    private String buildRequestBody(Agent agent, List<Map<String, String>> messages) {
        try {
            String messagesJson = objectMapper.writeValueAsString(messages);
            if (agent.getRequestBody() != null && !agent.getRequestBody().isBlank()) {
                return agent.getRequestBody().replace("{{messages}}", messagesJson);
            }
            return "{\"messages\":" + messagesJson + ",\"max_tokens\":1024}";
        } catch (Exception e) {
            return "{\"messages\":[]}";
        }
    }

    /**
     * 组装 messages 数组 — 单轮取 title，多轮取 turns。
     * 多轮场景仅发送 role=user 的消息，预填的 assistant 期望回答不发给被测模型。
     */
    private List<Map<String, String>> buildMessages(Question question) {
        List<Map<String, String>> messages = new ArrayList<>();
        if ("multi".equals(question.getQuestionType()) && question.getTurns() != null) {
            for (Turn turn : question.getTurns()) {
                if (!"assistant".equalsIgnoreCase(turn.getRole())) {
                    Map<String, String> msg = new LinkedHashMap<>();
                    msg.put("role", turn.getRole());
                    msg.put("content", turn.getContent());
                    messages.add(msg);
                }
            }
        } else {
            Map<String, String> msg = new LinkedHashMap<>();
            msg.put("role", "user");
            msg.put("content", question.getTitle());
            messages.add(msg);
        }
        return messages;
    }

    /**
     * 获取多轮题目中的 user 消息列表（过滤 assistant）。
     * 返回空列表表示非多轮题目。
     */
    private List<Turn> getUserTurns(Question question) {
        if (!"multi".equals(question.getQuestionType()) || question.getTurns() == null) {
            return List.of();
        }
        return question.getTurns().stream()
                .filter(t -> !"assistant".equalsIgnoreCase(t.getRole()))
                .collect(Collectors.toList());
    }

    /** 按路径从 JSON Map 中提取值。格式: choices[0].delta.content */
    @SuppressWarnings("unchecked")
    private String extractByPath(Map<String, Object> root, String path) {
        Object current = root;
        for (String seg : path.split("\\.")) {
            if (current == null) return null;
            int bracketIdx = seg.indexOf('[');
            String key = bracketIdx > 0 ? seg.substring(0, bracketIdx) : seg;
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(key);
            }
            if (bracketIdx > 0 && current instanceof List) {
                try {
                    String numStr = seg.substring(bracketIdx + 1, seg.length() - 1);
                    current = ((List<?>) current).get(Integer.parseInt(numStr));
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return current != null ? current.toString() : null;
    }

    /** 根据 Agent 的 authType 向 HTTP 请求头注入鉴权信息 */
    @SuppressWarnings("unchecked")
    private void applyAuth(HttpHeaders headers, Agent agent) {
        if ("bearer".equalsIgnoreCase(agent.getAuthType())) {
            headers.setBearerAuth(agent.getAuthCredential());
        } else if ("api_key".equalsIgnoreCase(agent.getAuthType())) {
            headers.set("X-API-Key", agent.getAuthCredential());
        } else if ("basic".equalsIgnoreCase(agent.getAuthType())) {
            headers.setBasicAuth(agent.getAuthCredential());
        } else if ("custom".equalsIgnoreCase(agent.getAuthType())) {
            try {
                Map<String, String> headerMap = objectMapper.readValue(
                        agent.getAuthCredential(), Map.class);
                headerMap.forEach(headers::set);
            } catch (Exception e) {
                log.warn("自定义Header解析失败: {}", e.getMessage());
            }
        }
    }

    /** Agent 响应封装 */
    public static class AgentResponse {
        public final String content;
        public final int tokensUsed;
        public final int latencyMs;
        public final String rawRequest;
        public final String rawResponse;

        public AgentResponse(String content, int tokensUsed, int latencyMs,
                             String rawRequest, String rawResponse) {
            this.content = content;
            this.tokensUsed = tokensUsed;
            this.latencyMs = latencyMs;
            this.rawRequest = rawRequest;
            this.rawResponse = rawResponse;
        }
    }
}
