package com.agenttest.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agent 视图对象 — Controller 返回给前端的 Agent 数据。
 * <p>
 * 与 entity 字段一致，由 Service 层通过 BeanUtil.copyProperties 从 entity 转换。
 * 编辑时前端需回显 authCredential，因此 VO 包含该字段（内部评测工具，非对外暴露）。
 */
@Data
public class AgentVO {

    /** 主键 ID */
    private Long id;
    /** Agent 名称 */
    private String name;
    /** 描述信息 */
    private String description;
    /** 底层模型标识（可选） */
    private String model;
    /** Agent 类型: llm / multi-modal / tool-use / code-gen / rag */
    private String type;
    /** 状态: active=运行中 / inactive=已停用 / evaluating=评测中 / error=异常 */
    private String status;
    /** 外部 Agent API 端点 URL */
    private String endpointUrl;
    /** 请求模板 JSON */
    private String requestBody;
    /** 响应协议: sse / json / auto */
    private String responseProtocol;
    /** 响应内容提取路径 */
    private String responseContentPath;
    /** 鉴权方式 */
    private String authType;
    /** 鉴权凭证 — 编辑时需回显，仅内部评测使用 */
    private String authCredential;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
