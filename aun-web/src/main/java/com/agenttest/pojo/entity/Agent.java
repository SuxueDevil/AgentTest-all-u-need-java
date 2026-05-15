package com.agenttest.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 待评测 Agent — 数据库实体，映射 agent 表。
 * <p>
 * {@link TableName} 指定表名和自动结果映射（JacksonTypeHandler 需要 autoResultMap=true）。
 * 字段名采用驼峰，MyBatis-Plus 自动映射到下划线格式的数据库列。
 */
@TableName("agent")
public class Agent {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Agent 名称（必填） */
    private String name;

    /** 描述信息 */
    private String description;

    /** 底层模型标识，如 gpt-4o、claude-opus-4-7 */
    private String model;

    /** 类型: llm / multi-modal / tool-use / code-gen / rag */
    private String type;

    /** 状态: active=运行中 / inactive=已停用 / evaluating=评测中 / error=异常 */
    private String status;

    /** 外部 Agent API 的完整 URL，如 https://api.openai.com/v1/chat/completions */
    private String endpointUrl;

    /** 鉴权方式: none / bearer / api_key / basic */
    private String authType;

    /**
     * 鉴权凭证（明文存储，MVP 阶段不加密）。
     * 该字段不会通过 VO 返回给前端，仅在服务端 HTTP 调用时使用。
     */
    private String authCredential;


    /** 创建时间，数据库自动填充 */
    private LocalDateTime createdAt;

    /** 更新时间，数据库自动更新 */
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
    public String getAuthCredential() { return authCredential; }
    public void setAuthCredential(String authCredential) { this.authCredential = authCredential; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
