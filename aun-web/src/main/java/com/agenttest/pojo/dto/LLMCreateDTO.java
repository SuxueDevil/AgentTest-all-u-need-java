package com.agenttest.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** LLM 创建请求 */
@Data
public class LLMCreateDTO {

    /** LLM 名称（必填） */
    @NotBlank(message = "LLM名称不能为空")
    private String name;

    /** 模型标识（必填），如 deepseek-chat */
    @NotBlank(message = "模型标识不能为空")
    private String model;

    /** API 端点 URL */
    private String endpointUrl;

    /** API Key */
    private String apiKey;
}
