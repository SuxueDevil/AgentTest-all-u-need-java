package com.agenttest.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 待评测 Agent — 数据库实体，映射 agent 表。
 * <p>
 * {@link TableName} 指定表名和自动结果映射（JacksonTypeHandler 需要 autoResultMap=true）。
 * 字段名采用驼峰，MyBatis-Plus 自动映射到下划线格式的数据库列。
 */
@Data
@TableName("agent")
public class Agent {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Agent 名称（必填） */
    private String name;

    /** 描述信息 */
    private String description;

    /** 底层模型标识（可选，仅标注参考），如 gpt-4o */
    private String model;

    /** 类型: llm / multi-modal / tool-use / code-gen / rag */
    private String type;

    /** 状态: active=运行中 / inactive=已停用 / evaluating=评测中 / error=异常 */
    private String status;

    /** 外部 Agent API 的完整 URL，如 https://api.openai.com/v1/chat/completions */
    private String endpointUrl;

    /** 请求模板 JSON，{{messages}} 占位符会被替换为实际消息数组 */
    private String requestBody;

    /** 响应协议: sse=流式 / json=普通JSON / auto=自动识别 */
    private String responseProtocol;

    /** 响应内容提取路径，如 choices[0].message.content，为空则取原始响应体 */
    private String responseContentPath;

    /** 鉴权方式: none / bearer / api_key / basic */
    private String authType;

    /**
     * 鉴权凭证（明文存储，MVP 阶段不加密）。
     * 该字段不会通过 VO 返回给前端，仅在服务端 HTTP 调用时使用。
     */
    private String authCredential;

    /** 创建时间，数据库自动填充 */
    private LocalDateTime createdAt;

    /** 逻辑删除标记（0=未删 / 1=已删），MyBatis-Plus 自动处理 */
    @TableLogic
    private Integer deleted;

    /** 更新时间，数据库自动更新 */
    private LocalDateTime updatedAt;
}
