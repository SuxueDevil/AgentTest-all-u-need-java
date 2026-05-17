package com.agenttest.service;

import com.agenttest.common.PageResult;
import com.agenttest.pojo.dto.LLMCreateDTO;
import com.agenttest.pojo.dto.LLMQueryDTO;
import com.agenttest.pojo.dto.LLMUpdateDTO;
import com.agenttest.pojo.vo.LLMVO;

/** LLM 业务接口 — 定义 LLM 模型的 CRUD 操作 */
public interface LLMService {

    /** 分页查询 LLM 列表，支持 keyword 模糊搜索 name */
    PageResult<LLMVO> page(LLMQueryDTO query);

    /** 查询单个 LLM，不存在时抛 BusinessException(404) */
    LLMVO getById(Long id);

    /** 创建 LLM */
    LLMVO create(LLMCreateDTO dto);

    /** 更新 LLM，只更新传的非 null 字段 */
    LLMVO update(Long id, LLMUpdateDTO dto);

    /** 删除 LLM */
    void delete(Long id);
}
