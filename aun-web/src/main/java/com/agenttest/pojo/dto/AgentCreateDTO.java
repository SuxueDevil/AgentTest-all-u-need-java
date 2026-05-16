package com.agenttest.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Agent 创建请求 — Controller 接收的入参。
 * 使用 Jakarta Validation 注解做基础校验，校验失败由 GlobalExceptionHandler 统一处理。
 */
@Data
public class AgentCreateDTO {

    /** Agent 名称（必填） */
    @NotBlank(message = "Agent名称不能为空")
    private String name;

    /** 描述信息 */
    private String description;

    /** 底层模型，如 gpt-4o */
    private String model;

    /** Agent 类型（必填） */
    @NotBlank(message = "Agent类型不能为空")
    private String type;

    /** 外部 Agent API 端点 URL */
    private String endpointUrl;

    /** 请求模板 JSON，{{messages}} 占位符会被替换 */
    private String requestBody;

    /** 响应内容提取路径，如 choices[0].message.content */
    private String responseContentPath;

    /** 鉴权方式: none / bearer / api_key / basic */
    private String authType;

    /** 鉴权凭证（如 API Key、Bearer Token 等，明文存储） */
    private String authCredential;
}
