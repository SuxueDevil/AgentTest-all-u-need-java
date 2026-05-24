package com.agenttest.controller;

import com.agenttest.common.PageResult;
import com.agenttest.common.Response;
import com.agenttest.pojo.dto.LLMCreateDTO;
import com.agenttest.pojo.dto.LLMQueryDTO;
import com.agenttest.pojo.dto.LLMUpdateDTO;
import com.agenttest.pojo.vo.LLMVO;
import com.agenttest.service.LLMService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * LLM 模型管理 REST API — 提供 LLM 的完整 CRUD 接口。
 * <p>
 * 路径前缀 /api/llms，返回格式统一为 {@link Response}。
 */
@RestController
@RequestMapping("/api/llms")
@Validated
@RequiredArgsConstructor
public class LLMController {

    private final LLMService llmService;

    /** 分页查询 LLM 列表 */
    @GetMapping
    public Response<PageResult<LLMVO>> list(LLMQueryDTO query) {
        return Response.success(llmService.page(query));
    }

    /** 查询单个 LLM */
    @GetMapping("/{id}")
    public Response<LLMVO> detail(@PathVariable Long id) {
        return Response.success(llmService.getById(id));
    }

    /** 创建 LLM */
    @PostMapping
    public Response<LLMVO> create(@Valid @RequestBody LLMCreateDTO dto) {
        return Response.success(llmService.create(dto));
    }

    /** 更新 LLM */
    @PutMapping("/{id}")
    public Response<LLMVO> update(@PathVariable Long id, @RequestBody LLMUpdateDTO dto) {
        return Response.success(llmService.update(id, dto));
    }

    /** 删除 LLM */
    @DeleteMapping("/{id}")
    public Response<Void> delete(@PathVariable Long id) {
        llmService.delete(id);
        return Response.success();
    }
}
