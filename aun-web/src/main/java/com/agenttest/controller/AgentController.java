package com.agenttest.controller;

import com.agenttest.common.Response;
import com.agenttest.common.PageResult;
import com.agenttest.pojo.dto.AgentCreateDTO;
import com.agenttest.pojo.dto.AgentQueryDTO;
import com.agenttest.pojo.dto.AgentUpdateDTO;
import com.agenttest.pojo.vo.AgentVO;
import com.agenttest.service.AgentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * Agent 管理 REST API — 提供 Agent 的完整 CRUD 和连接测试接口。
 * <p>
 * 所有接口路径前缀为 /api/agents，由 Vite 开发服务器的代理转发到本 Controller。
 * 返回格式统一为 {@link Response}，分页接口 data 字段内嵌 {@link PageResult}。
 */
@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentService agentService;

    /** 构造器注入 AgentService */
    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * 分页查询 Agent 列表。
     * Query String 参数: page, pageSize, keyword, type, status
     */
    @GetMapping
    public Response<PageResult<AgentVO>> list(AgentQueryDTO query) {
        return Response.success(agentService.page(query));
    }

    /** 查询单个 Agent 详情，id 不存在时返回 404 错误 */
    @GetMapping("/{id}")
    public Response<AgentVO> detail(@PathVariable Long id) {
        return Response.success(agentService.getById(id));
    }

    /** 创建 Agent，请求体校验失败返回 400 */
    @PostMapping
    public Response<AgentVO> create(@Valid @RequestBody AgentCreateDTO dto) {
        return Response.success(agentService.create(dto));
    }

    /** 更新 Agent，支持部分字段更新 */
    @PutMapping("/{id}")
    public Response<AgentVO> update(@PathVariable Long id, @RequestBody AgentUpdateDTO dto) {
        return Response.success(agentService.update(id, dto));
    }

    /** 删除 Agent */
    @DeleteMapping("/{id}")
    public Response<Void> delete(@PathVariable Long id) {
        agentService.delete(id);
        return Response.success();
    }

    /** 测试 Agent API 连通性，返回 true/false */
    @PostMapping("/{id}/test-connection")
    public Response<Boolean> testConnection(@PathVariable Long id) {
        return Response.success(agentService.testConnection(id));
    }
}
