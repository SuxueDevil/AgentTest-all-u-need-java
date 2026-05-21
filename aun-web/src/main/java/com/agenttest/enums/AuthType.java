package com.agenttest.enums;

import com.agenttest.common.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import java.util.Map;

/**
 * 鉴权类型策略枚举 — 每种鉴权方式自闭环 apply() 逻辑。
 * <p>
 * 原 applyAuth() 方法散落两处（AgentServiceImpl + AgentHttpClient），
 * 魔法字符串 if-else + 吞异常 + Custom Header 无白名单，存在安全风险。
 * 重构后枚举自包含、异常显式化、Custom Header 仅允许 X-Custom- 前缀。
 * <p>
 * 用法: {@code AuthType.from(type).apply(headers, credential);}
 */
public enum AuthType {

    BEARER {
        @Override
        public void apply(HttpHeaders headers, String credential) {
            if (credential == null || credential.isBlank()) {
                throw new BusinessException(400, "Bearer 鉴权凭证不能为空");
            }
            headers.setBearerAuth(credential);
        }
    },

    API_KEY {
        @Override
        public void apply(HttpHeaders headers, String credential) {
            if (credential == null || credential.isBlank()) {
                throw new BusinessException(400, "API Key 鉴权凭证不能为空");
            }
            headers.set("X-API-Key", credential);
        }
    },

    /**
     * Basic 鉴权 — credential 格式为 "username:password"。
     * 冒号分隔取用户名和密码，格式错误抛 BusinessException。
     */
    BASIC {
        @Override
        public void apply(HttpHeaders headers, String credential) {
            if (credential == null || !credential.contains(":")) {
                throw new BusinessException(400, "Basic 鉴权凭证格式错误，需为 username:password");
            }
            String[] parts = credential.split(":", 2);
            headers.setBasicAuth(parts[0], parts[1]);
        }
    },

    /**
     * 自定义 Header 鉴权 — credential 为 JSON 对象，逐项设为 Header。
     * 仅允许 X-Custom- 前缀的 Header 名，防止注入恶意头（Host、X-Forwarded-For 等）。
     */
    CUSTOM {
        private static final ObjectMapper MAPPER = new ObjectMapper();
        private static final Logger LOG = LoggerFactory.getLogger(AuthType.class);

        @Override
        public void apply(HttpHeaders headers, String credential) {
            if (credential == null || credential.isBlank()) {
                throw new BusinessException(400, "自定义鉴权凭证不能为空");
            }
            try {
                Map<String, String> map = MAPPER.readValue(credential,
                        new TypeReference<Map<String, String>>() {});
                map.forEach((key, value) -> {
                    if (key.startsWith("X-Custom-")) {
                        headers.set(key, value);
                    } else {
                        LOG.warn("忽略非法自定义 Header（仅允许 X-Custom- 前缀）: {}", key);
                    }
                });
            } catch (JsonProcessingException e) {
                throw new BusinessException(400, "自定义鉴权 JSON 解析失败: " + e.getMessage());
            }
        }
    },

    /** 无鉴权 */
    NONE {
        @Override
        public void apply(HttpHeaders headers, String credential) {
            // 什么都不做
        }
    };

    /**
     * 向 HTTP 请求头注入鉴权信息。
     *
     * @param headers    HTTP 请求头（会被原地修改）
     * @param credential 鉴权凭证
     */
    public abstract void apply(HttpHeaders headers, String credential);

    /**
     * 根据鉴权类型字符串获取枚举实例。
     * 大小写不敏感，null 或无法识别时返回 NONE 并打 warn。
     *
     * @param type 鉴权类型字符串（bearer / api_key / basic / custom / none）
     * @return 对应的枚举实例，无法识别则返回 NONE
     */
    public static AuthType from(String type) {
        if (type == null || type.isBlank()) {
            return NONE;
        }
        try {
            return valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            LoggerFactory.getLogger(AuthType.class)
                    .warn("未知鉴权类型 '{}'，降级为 NONE（无鉴权）", type);
            return NONE;
        }
    }
}
