package com.agenttest.controller;

import com.agenttest.common.PageResult;
import com.agenttest.common.Response;
import com.agenttest.pojo.dto.EvaluationTaskCreateDTO;
import com.agenttest.pojo.dto.EvaluationTaskQueryDTO;
import com.agenttest.pojo.dto.EvaluationTaskUpdateDTO;
import com.agenttest.pojo.vo.AgentResultVO;
import com.agenttest.pojo.vo.EvaluationTaskVO;
import com.agenttest.pojo.vo.TaskProgressVO;
import com.agenttest.service.EvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评测任务管理 REST API — 提供任务的 CRUD、启动、取消、进度轮询和结果查询。
 * <p>
 * 路径前缀 /api/evaluation，返回格式统一为 {@link Response}。
 * 启动评测为异步操作，前端通过 /progress 接口 3s 轮询获取执行进度。
 */
@RestController
@RequestMapping("/api/evaluation")
@Validated
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    // ==================== CRUD ====================

    /**
     * 分页查询评测任务列表。
     *
     * @param query page, pageSize, status（可选）
     * @return 按创建时间倒序排列的任务列表
     */
    @GetMapping
    public Response<PageResult<EvaluationTaskVO>> list(EvaluationTaskQueryDTO query) {
        return Response.success(evaluationService.page(query));
    }

    /**
     * 查询评测任务详情。
     *
     * @param id 任务 ID
     * @return 任务完整信息（含 questionIds / agentIds / dimensions）
     */
    @GetMapping("/{id}")
    public Response<EvaluationTaskVO> detail(@PathVariable Long id) {
        return Response.success(evaluationService.getById(id));
    }

    /**
     * 创建评测任务。
     *
     * @param dto 任务名称 + 题目列表 + Agent 列表 + 维度配置（均必填）
     * @return 创建后的任务（状态为 pending）
     */
    @PostMapping
    public Response<EvaluationTaskVO> create(@Valid @RequestBody EvaluationTaskCreateDTO dto) {
        return Response.success(evaluationService.create(dto));
    }

    /**
     * 更新评测任务（仅允许修改 name 和 description）。
     * 运行中的任务不可编辑。
     *
     * @param id  任务 ID
     * @param dto 部分更新的字段
     * @return 更新后的任务信息
     */
    @PutMapping("/{id}")
    public Response<EvaluationTaskVO> update(@PathVariable Long id,
                                              @RequestBody EvaluationTaskUpdateDTO dto) {
        return Response.success(evaluationService.update(id, dto));
    }

    /**
     * 删除评测任务（含关联的所有评测结果）。
     * 运行中的任务需先取消再删除。
     *
     * @param id 任务 ID
     * @return 空 data，删除成功
     */
    @DeleteMapping("/{id}")
    public Response<Void> delete(@PathVariable Long id) {
        evaluationService.delete(id);
        return Response.success();
    }

    // ==================== 执行控制 ====================

    /**
     * 启动评测 — 异步执行，立即返回。
     * 后台遍历 agentIds × questionIds 逐条评测并入库结果。
     *
     * @param id 任务 ID
     * @return 空 data
     */
    @PostMapping("/{id}/start")
    public Response<Void> start(@PathVariable Long id) {
        evaluationService.start(id);
        return Response.success();
    }

    /**
     * 重新开始评测 — 重置状态为 pending 并清空结果，保留原有配置。
     *
     * @param id 任务 ID
     * @return 空 data
     */
    @PostMapping("/{id}/restart")
    public Response<Void> restart(@PathVariable Long id) {
        evaluationService.restart(id);
        return Response.success();
    }

    /**
     * 取消评测 — 设置取消标志位，后台异步任务检测到后中断执行。
     *
     * @param id 任务 ID
     * @return 空 data
     */
    @PostMapping("/{id}/cancel")
    public Response<Void> cancel(@PathVariable Long id) {
        evaluationService.cancel(id);
        return Response.success();
    }

    /**
     * 轮询任务进度 — 前端每 3s 调用一次，用于展示进度条。
     *
     * @param id 任务 ID
     * @return { status, questionCount, completedCount }
     */
    @GetMapping("/{id}/progress")
    public Response<TaskProgressVO> progress(@PathVariable Long id) {
        return Response.success(evaluationService.getProgress(id));
    }

    /**
     * 查询评测结果 — 按 Agent 分组返回各维度得分和逐题明细。
     * 仅在任务状态为 completed 或 cancelled 时可查。
     *
     * @param id 任务 ID
     * @return 按 Agent 分组的评测结果列表
     */
    @GetMapping("/{id}/results")
    public Response<List<AgentResultVO>> results(@PathVariable Long id) {
        return Response.success(evaluationService.getResults(id));
    }
}
