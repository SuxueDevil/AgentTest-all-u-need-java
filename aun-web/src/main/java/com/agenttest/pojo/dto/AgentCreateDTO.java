package com.agenttest.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * Agent 创建请求 — Controller 接收的入参。
 * 使用 Jakarta Validation 注解做基础校验，校验失败由 GlobalExceptionHandler 统一处理。
 */
public class AgentCreateDTO {

    /** Agent 名称（必填） */
    @NotBlank(message = "Agent名称不能为空")
    private String name;

    /** 描述 */
    private String description;

    /** 底层模型，如 gpt-4o */
    private String model;

    /** Agent 类型（必填） */
    @NotBlank(message = "Agent类型不能为空")
    private String type;

    /** 外部 Agent API 端点 URL */
    private String endpointUrl;

    /** 鉴权方式: none / bearer / api_key / basic */
    private String authType;

    /** 鉴权凭证 */
    private String authCredential;

    /** 标签列表 */
    private List<String> tags;

    // ==================== getters / setters ====================

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getEndpointUrl() { return endpointUrl; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }
    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }
    public String getAuthCredential() { return authCredential; }
    public void setAuthCredential(String authCredential) { this.authCredential = authCredential; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
