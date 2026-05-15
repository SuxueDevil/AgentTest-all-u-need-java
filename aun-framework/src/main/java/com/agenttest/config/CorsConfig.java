package com.agenttest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * 跨域配置 — 允许前端开发服务器 (端口 5173) 跨域访问后端 API。
 * 生产环境通过 Nginx 或 Docker Compose 网络处理跨域，此配置仅在开发阶段生效。
 */
@Configuration
public class CorsConfig {

    /**
     * 注册 CorsFilter Bean，对所有路径 (/**) 放行跨域请求。
     * 允许所有 HTTP 方法、所有请求头，预检缓存 1 小时。
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
