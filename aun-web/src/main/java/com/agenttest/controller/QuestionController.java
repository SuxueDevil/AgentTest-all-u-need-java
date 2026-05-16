package com.agenttest.controller;

import com.agenttest.common.utils.FileExportResult;
import com.agenttest.common.PageResult;
import com.agenttest.common.Response;
import com.agenttest.pojo.dto.QuestionCreateDTO;
import com.agenttest.pojo.dto.QuestionQueryDTO;
import com.agenttest.pojo.dto.QuestionUpdateDTO;
import com.agenttest.pojo.vo.QuestionVO;
import com.agenttest.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 题库管理 REST API — 提供问题的 CRUD、批量删除和导入导出接口。
 * <p>
 * 路径前缀 /api/questions，返回格式统一为 {@link Response}。
 * 导入接口接受 multipart/form-data，导出接口返回文件流。
 */
@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    /** 构造器注入 */
    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    // ==================== CRUD ====================

    /**
     * 分页查询问题列表。
     *
     * @param query Query String 自动绑定: page, pageSize, keyword, category, difficulty, questionType
     * @return 分页结果，data 内嵌 PageResult<QuestionVO>
     */
    @GetMapping
    public Response<PageResult<QuestionVO>> list(QuestionQueryDTO query) {
        return Response.success(questionService.page(query));
    }

    /**
     * 查询单个问题详情。
     *
     * @param id 问题主键 ID
     * @return QuestionVO，id 不存在时返回 404 错误
     */
    @GetMapping("/{id}")
    public Response<QuestionVO> detail(@PathVariable Long id) {
        return Response.success(questionService.getById(id));
    }

    /**
     * 创建问题。
     *
     * @param dto 问题创建参数（title、category 必填），校验失败返回 400
     * @return 创建后的 QuestionVO
     */
    @PostMapping
    public Response<QuestionVO> create(@Valid @RequestBody QuestionCreateDTO dto) {
        return Response.success(questionService.create(dto));
    }

    /**
     * 更新问题，支持部分字段更新。
     *
     * @param id  问题主键 ID
     * @param dto 部分更新的字段
     * @return 更新后的 QuestionVO
     */
    @PutMapping("/{id}")
    public Response<QuestionVO> update(@PathVariable Long id, @RequestBody QuestionUpdateDTO dto) {
        return Response.success(questionService.update(id, dto));
    }

    /**
     * 删除单个问题。
     *
     * @param id 问题主键 ID
     * @return 空 data，删除成功
     */
    @DeleteMapping("/{id}")
    public Response<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return Response.success();
    }

    /**
     * 批量删除问题。
     *
     * @param ids 问题主键 ID 列表，请求体示例: [1, 2, 3]
     * @return 实际删除的条数
     */
    @DeleteMapping("/batch")
    public Response<Integer> batchDelete(@RequestBody List<Long> ids) {
        return Response.success(questionService.batchDelete(ids));
    }

    // ==================== 导入导出 ====================

    /**
     * 批量导入问题。
     *
     * @param file 上传文件，支持 CSV 或 JSON 格式
     * @return 导入统计: { successCount, failCount, errors: [{row, message}] }
     * @throws IOException 文件读取失败时抛出
     */
    @PostMapping("/import")
    public Response<Map<String, Object>> importQuestions(@RequestParam("file") MultipartFile file) throws IOException {
        Map<String, Object> result = questionService.importQuestions(
                file.getBytes(), file.getOriginalFilename());
        return Response.success(result);
    }

    /**
     * 导出问题为文件下载。
     *
     * @param format 导出格式（csv / json），默认 json，通过 Query String 指定
     * @return 文件字节流，Content-Disposition 设为 attachment，浏览器自动下载
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportQuestions(@RequestParam(defaultValue = "json") String format) {
        FileExportResult result = questionService.exportQuestions(format);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + URLEncoder.encode(result.getFilename(), StandardCharsets.UTF_8).replace("+", "%20"))
                .contentType(MediaType.parseMediaType(result.getContentType()))
                .body(result.getBytes());
    }

    // ==================== AI 生成 ====================

    /**
     * AI 生成题目 — 调用 LLM 按分类/难度/类型/数量批量生成并入库。
     *
     * @param params { category, difficulty, questionType, count }
     * @return 入库的题目列表
     */
    @PostMapping("/generate")
    public Response<List<QuestionVO>> generate(@RequestBody Map<String, Object> params) {
        String category = (String) params.getOrDefault("category", "reasoning");
        String difficulty = (String) params.getOrDefault("difficulty", "medium");
        String questionType = (String) params.getOrDefault("questionType", "single");
        int count = Math.min((int) params.getOrDefault("count", 5), 20);
        return Response.success(questionService.generate(category, difficulty, questionType, count));
    }
}
