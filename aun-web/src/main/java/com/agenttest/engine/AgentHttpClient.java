package com.agenttest.engine;

import com.agenttest.pojo.entity.Agent;
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

/**
 * Agent HTTP 客户端 — 向被测 Agent 发送评测请求并解析响应。
 * <p>
 * 自动识别响应类型：SSE 流式（data: 开头）或普通 JSON（{ 开头）。
 * 支持通过 Agent 配置自定义请求模板（{{messages}} 占位符）和响应提取路径。
 * 鉴权支持 bearer / api_key / basic / custom 四种方式。
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

    /**
     * 向指定 Agent 发送评测请求。自动识别 SSE 流式或普通 JSON 响应。
     *
     * @param agent    被测 Agent 实体
     * @param question 题目实体
     * @return 响应结果: { content, tokensUsed, latencyMs, rawRequest, rawResponse }
     */
    @SuppressWarnings("unchecked")
    public AgentResponse send(Agent agent, Question question) {
        long start = System.currentTimeMillis();

        // 1. 组装 messages 并构造请求体
        List<Map<String, String>> messages = buildMessages(question);
        String bodyStr = buildRequestBody(agent, messages);

        // 2. 设置请求头 + 鉴权
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        applyAuth(headers, agent);

        // 3. 发送请求并流式读取响应
        HttpEntity<String> request = new HttpEntity<>(bodyStr, headers);
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

    /** 解析响应 — 自动识别 SSE 或 JSON */
    @SuppressWarnings("unchecked")
    private AgentResponse parseResponse(ClientHttpResponse response, Agent agent,
                                         String bodyStr, long start) throws IOException {
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
                // SSE 流式 — 逐行读取 data: 事件
                StringBuilder contentBuf = new StringBuilder();
                for (String line; (line = reader.readLine()) != null; ) {
                    if (rawResponse.length() < 2000) rawResponse.append(line).append("\n");
                    if (line.startsWith("data: ") && !line.equals("data: [DONE]")) {
                        String json = line.substring(6);
                        try {
                            Map<String, Object> chunk = objectMapper.readValue(json, Map.class);
                            String delta = extractByPath(chunk, "choices[0].delta.content");
                            if (delta != null) contentBuf.append(delta);
                            // 最后一块可能带 usage
                            Map<String, Object> usage = (Map<String, Object>) chunk.get("usage");
                            if (usage != null) {
                                tokensUsed = ((Number) usage.getOrDefault("total_tokens", 0)).intValue();
                            }
                        } catch (Exception ignored) { /* 跳过无法解析的块 */ }
                    }
                }
                content = contentBuf.toString();
            } else {
                // 普通 JSON — 整段读取后按路径提取
                StringBuilder jsonBuf = new StringBuilder();
                for (String line; (line = reader.readLine()) != null; ) {
                    rawResponse.append(line).append("\n");
                    jsonBuf.append(line);
                }
                Map<String, Object> respMap = objectMapper.readValue(jsonBuf.toString(), Map.class);
                String path = (agent.getResponseContentPath() != null
                        && !agent.getResponseContentPath().isBlank())
                        ? agent.getResponseContentPath()
                        : "choices[0].message.content";
                content = extractByPath(respMap, path);
                Map<String, Object> usage = (Map<String, Object>) respMap.get("usage");
                if (usage != null) {
                    tokensUsed = ((Number) usage.getOrDefault("total_tokens", 0)).intValue();
                }
            }
        }

        log.info("Agent调用完成: agent={}, latency={}ms, tokens={}", agent.getName(), latencyMs, tokensUsed);
        return new AgentResponse(content, tokensUsed, latencyMs, bodyStr,
                rawResponse.toString().trim());
    }

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

    /** 组装 messages 数组 — 单轮取 title，多轮取 turns */
    private List<Map<String, String>> buildMessages(Question question) {
        List<Map<String, String>> messages = new ArrayList<>();
        if ("multi".equals(question.getQuestionType()) && question.getTurns() != null) {
            for (Turn turn : question.getTurns()) {
                Map<String, String> msg = new LinkedHashMap<>();
                msg.put("role", turn.getRole());
                msg.put("content", turn.getContent());
                messages.add(msg);
            }
        } else {
            Map<String, String> msg = new LinkedHashMap<>();
            msg.put("role", "user");
            msg.put("content", question.getTitle());
            messages.add(msg);
        }
        return messages;
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
