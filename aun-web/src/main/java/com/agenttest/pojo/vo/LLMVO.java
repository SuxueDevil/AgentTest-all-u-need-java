package com.agenttest.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** LLM 视图对象 — 不含 apiKey（创建/编辑时可传入但列表不返回） */
@Data
public class LLMVO {

    private Long id;
    private String name;
    private String model;
    private String endpointUrl;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
