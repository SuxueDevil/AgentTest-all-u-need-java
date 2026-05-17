package com.agenttest.pojo.dto;

import lombok.Data;

/** LLM 更新请求 — 所有字段可选，传哪些就更新哪些 */
@Data
public class LLMUpdateDTO {

    private String name;
    private String model;
    private String endpointUrl;
    private String apiKey;
}
