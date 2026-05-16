package com.agenttest.engine;

import cn.hutool.core.bean.BeanUtil;
import com.agenttest.pojo.entity.Agent;
import com.agenttest.pojo.entity.Question;
import com.agenttest.pojo.entity.Question.Turn;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Agent HTTP 客户端 — 向被测 Agent 发送评测请求并解析响应。
 * <p>
 * 根据 Agent 的 authType 自动附加鉴权头，支持 bearer / api_key / basic / custom。
 * 单轮问题组装为单条 user message；多轮问题将 turns 逐条加入 messages 数组。
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
     * 向指定 Agent 发送评测请求。
     *
     * @param agent    被测 Agent 实体（含 endpointUrl + authType + authCredential）
     * @param question 题目实体（单轮取 title，多轮取 turns）
     * @return 响应结果: { content, tokensUsed, latencyMs, rawRequest, rawResponse }
     */
    public AgentResponse send(Agent agent, Question question) {
        long start = System.currentTimeMillis();

        // 1. 组装 messages 数组
        List<Map<String, String>> messages = buildMessages(question);

        // 2. 构造请求体
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messages", messages);
        body.put("max_tokens", 1024);

        String rawRequest;
        try {
            rawRequest = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            rawRequest = body.toString();
        }

        // 3. 设置请求头 + 鉴权
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        applyAuth(headers, agent);

        // 4. 发送请求
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                agent.getEndpointUrl(), HttpMethod.POST, request, String.class);

        int latencyMs = (int) (System.currentTimeMillis() - start);

        // 5. 解析响应
        String content = "";
        int tokensUsed = 0;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> respMap = objectMapper.readValue(response.getBody(), Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) respMap.get("choices");
            if (choices != null && !choices.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                if (message != null) {
                    content = Objects.toString(message.get("content"), "");
                }
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> usage = (Map<String, Object>) respMap.get("usage");
            if (usage != null) {
                tokensUsed = ((Number) usage.getOrDefault("total_tokens", 0)).intValue();
            }
        } catch (Exception e) {
            // 非标准响应，尝试直接使用原始文本
            content = response.getBody();
            log.warn("解析Agent响应失败: {}", e.getMessage());
        }

        log.info("Agent调用完成: agent={}, latency={}ms, tokens={}", agent.getName(), latencyMs, tokensUsed);
        return new AgentResponse(content, tokensUsed, latencyMs, rawRequest, response.getBody());
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

    /**
     * Agent 响应封装 — 包含模型原文、token 消耗、延迟等完整信息。
     */
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
