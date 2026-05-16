package com.agenttest.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Judge LLM 配置 — 提供 ChatClient Bean 用于 LLM-as-Judge 评分。
 * <p>
 * 结构化输出通过 prompt 内要求 JSON + BeanOutputConverter 手动解析实现，
 * 兼容 Spring AI 1.0.0-M7（无需 2.0 的 ENABLE_NATIVE_STRUCTURED_OUTPUT）。
 */
@Configuration
public class JudgeConfig {

    @Bean
    ChatClient judgeClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
