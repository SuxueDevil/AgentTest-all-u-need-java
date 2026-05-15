package com.agenttest.pojo.dto;


/**
 * Agent 更新请求 — 所有字段可选，前端传哪些就更新哪些。
 * 与 CreateDTO 的区别是不做 @NotBlank 校验，允许部分更新。
 */
public class AgentUpdateDTO {

    private String name;
    private String description;
    private String model;
    private String type;
    /** API 端点 URL */
    private String endpointUrl;
    /** 鉴权方式 */
    private String authType;
    /** 鉴权凭证（前端传空字符串表示不修改） */
    private String authCredential;

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
}
