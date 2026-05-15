package com.agenttest.pojo.dto;

import lombok.Data;

/**
 * Agent 列表查询参数 — Controller 通过 Query String 自动绑定。
 * <p>
 * Spring MVC 会自动将 ?page=1&pageSize=20&keyword=xxx&type=llm 绑定到此对象的字段。
 * 未传的字段使用默认值: page=1, pageSize=20。
 */
@Data
public class AgentQueryDTO {

    /** 页码，从 1 开始，默认 1 */
    private Integer page = 1;

    /** 每页条数，默认 20 */
    private Integer pageSize = 20;

    /** 搜索关键词，匹配 name 和 description */
    private String keyword;

    /** 按类型筛选 */
    private String type;

    /** 按状态筛选 */
    private String status;
}
