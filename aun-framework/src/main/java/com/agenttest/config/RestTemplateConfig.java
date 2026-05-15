package com.agenttest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP 客户端配置 — 用于调用外部 Agent API 进行评测和连接测试。
 * 使用 JDK 内置的 {@link java.net.HttpURLConnection} 实现，无额外三方依赖。
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 创建 RestTemplate Bean。
     * 连接超时 10s：DNS 解析 + TCP 握手的时间上限。
     * 读取超时 30s：等待 Agent 返回响应的最长时间。
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        return new RestTemplate(factory);
    }
}
