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
     *
     * @param query Query String 自动绑定: page, pageSize, keyword, type, status
     * @return 分页结果，data 内嵌 PageResult<AgentVO>
     */
    @GetMapping
    public Response<PageResult<AgentVO>> list(AgentQueryDTO query) {
        return Response.success(agentService.page(query));
    }

    /**
     * 查询单个 Agent 详情。
     *
     * @param id Agent 主键 ID
     * @return AgentVO，id 不存在时返回 404 错误
     */
    @GetMapping("/{id}")
    public Response<AgentVO> detail(@PathVariable Long id) {
        return Response.success(agentService.getById(id));
    }

    /**
     * 创建 Agent。
     *
     * @param dto Agent 创建参数（name、type 必填），校验失败返回 400
     * @return 创建后的 AgentVO
     */
    @PostMapping
    public Response<AgentVO> create(@Valid @RequestBody AgentCreateDTO dto) {
        return Response.success(agentService.create(dto));
    }

    /**
     * 更新 Agent，支持部分字段更新。
     *
     * @param id  Agent 主键 ID
     * @param dto 部分更新的字段
     * @return 更新后的 AgentVO
     */
    @PutMapping("/{id}")
    public Response<AgentVO> update(@PathVariable Long id, @RequestBody AgentUpdateDTO dto) {
        return Response.success(agentService.update(id, dto));
    }

    /**
     * 删除 Agent。
     *
     * @param id Agent 主键 ID
     * @return 空 data，删除成功
     */
    @DeleteMapping("/{id}")
    public Response<Void> delete(@PathVariable Long id) {
        agentService.delete(id);
        return Response.success();
    }

    /**
     * 测试 Agent API 连通性。
     *
     * @param id Agent 主键 ID
     * @return true 表示连通，false 表示失败
     */
    @PostMapping("/{id}/test-connection")
    public Response<Boolean> testConnection(@PathVariable Long id) {
        return Response.success(agentService.testConnection(id));
    }
}
