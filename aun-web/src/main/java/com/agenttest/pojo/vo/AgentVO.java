package com.agenttest.pojo.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 视图对象 — Controller 返回给前端的 Agent 数据。
 * <p>
 * 与 entity 的关键区别: <b>不含 authCredential 字段</b>，防止敏感凭证泄露到前端。
 * 其他字段与 entity 一致，由 Service 层通过 BeanUtil.copyProperties 从 entity 转换。
 */
public class AgentVO {

    private Long id;
    private String name;
    private String description;
    private String model;
    private String type;
    private String status;
    /** Agent API 端点 URL */
    private String endpointUrl;
    /** 鉴权方式（仅表示类型，不含凭证值） */
    private String authType;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ==================== getters / setters ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEndpointUrl() { return endpointUrl; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }
    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
