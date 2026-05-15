package com.agenttest.pojo.dto;

import lombok.Data;

/**
 * Agent 更新请求 — 所有字段可选，前端传哪些就更新哪些。
 * 与 CreateDTO 的区别是不做 @NotBlank 校验，允许部分更新。
 */
@Data
public class AgentUpdateDTO {

    /** Agent 名称 */
    private String name;
    /** 描述信息 */
    private String description;
    /** 底层模型标识（可选） */
    private String model;
    /** Agent 类型: llm / multi-modal / tool-use / code-gen / rag */
    private String type;
    /** 外部 Agent API 端点 URL */
    private String endpointUrl;
    /** 请求模板 JSON */
    private String requestBody;
    /** 响应协议: sse / json / auto */
    private String responseProtocol;

    /** 响应内容提取路径 */
    private String responseContentPath;
    /** 鉴权方式: none / bearer / api_key / basic */
    private String authType;
    /** 鉴权凭证（前端传空字符串表示不修改） */
    private String authCredential;
}
