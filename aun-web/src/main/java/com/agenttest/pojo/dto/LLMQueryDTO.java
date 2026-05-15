package com.agenttest.pojo.dto;

import lombok.Data;

/** LLM 查询参数 */
@Data
public class LLMQueryDTO {

    /** 搜索关键词，匹配 name */
    private String keyword;

    /** 页码，默认 1 */
    private int page = 1;

    /** 每页条数，默认 20 */
    private int pageSize = 20;
}
