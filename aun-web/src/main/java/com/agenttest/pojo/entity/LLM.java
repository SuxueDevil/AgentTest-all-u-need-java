package com.agenttest.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * LLM 模型 — 待评测的底层大语言模型，区别于 Agent 应用。
 * <p>
 * LLM 总是走标准 OpenAI 兼容格式，请求体固定包含 model 字段。
 * Agent 可以自定义请求模板，LLM 则不能。
 */
@Data
@TableName("llm")
public class LLM {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** LLM 名称（必填），如 DeepSeek V4 */
    private String name;

    /** 模型标识（必填），如 deepseek-chat、gpt-4o，会被拼入请求体 model 字段 */
    private String model;

    /** API 端点 URL，如 https://api.deepseek.com/v1/chat/completions */
    private String endpointUrl;

    /** API Key */
    private String apiKey;

    /** 状态: active / inactive */
    private String status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
