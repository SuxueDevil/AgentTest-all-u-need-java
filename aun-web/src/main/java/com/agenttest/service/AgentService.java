package com.agenttest.service;

import com.agenttest.common.PageResult;
import com.agenttest.pojo.dto.AgentCreateDTO;
import com.agenttest.pojo.dto.AgentQueryDTO;
import com.agenttest.pojo.dto.AgentUpdateDTO;
import com.agenttest.pojo.vo.AgentVO;

/**
 * Agent 业务接口 — 定义 Agent 的 CRUD 和连接测试操作。
 * 所有方法的返回值统一为 AgentVO（不含 authCredential）。
 */
public interface AgentService {

    /**
     * 分页查询 Agent 列表。
     * 支持 keyword 模糊搜索（匹配 name + description）、type/status 精确筛选。
     */
    PageResult<AgentVO> page(AgentQueryDTO query);

    /**
     * 查询单个 Agent，不存在时抛出 BusinessException(404)。
     */
    AgentVO getById(Long id);

    /**
     * 创建 Agent，自动设置 status 为 active。
     */
    AgentVO create(AgentCreateDTO dto);

    /**
     * 更新 Agent，只更新前端传的非 null 字段。
     */
    AgentVO update(Long id, AgentUpdateDTO dto);

    /**
     * 删除 Agent，不存在时抛出异常。
     */
    void delete(Long id);

    /**
     * 测试 Agent 连接 — 向 agent.endpointUrl 发送一条简单的 ping 消息，
     * 根据 agent.authType 自动附加鉴权头。返回 true 表示连通。
     */
    boolean testConnection(Long id);
}
