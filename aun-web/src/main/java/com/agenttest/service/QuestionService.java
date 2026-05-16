package com.agenttest.service;

import com.agenttest.common.utils.FileExportResult;
import com.agenttest.common.PageResult;
import com.agenttest.pojo.dto.QuestionCreateDTO;
import com.agenttest.pojo.dto.QuestionQueryDTO;
import com.agenttest.pojo.dto.QuestionUpdateDTO;
import com.agenttest.pojo.vo.QuestionVO;

import java.util.List;
import java.util.Map;

/**
 * 题库业务接口 — 定义问题的 CRUD、批量删除、导入导出操作。
 */
public interface QuestionService {

    /**
     * 分页查询问题列表。
     * 支持 keyword 模糊匹配 title，category/difficulty/questionType 精确筛选。
     */
    PageResult<QuestionVO> page(QuestionQueryDTO query);

    /** 查询单个问题，不存在时抛出 BusinessException(404) */
    QuestionVO getById(Long id);

    /** 创建问题，questionType 未传时默认 single */
    QuestionVO create(QuestionCreateDTO dto);

    /** 更新问题，只更新前端传的非 null 字段 */
    QuestionVO update(Long id, QuestionUpdateDTO dto);

    /** 删除单个问题 */
    void delete(Long id);

    /** 批量删除问题，返回实际删除的条数 */
    int batchDelete(List<Long> ids);

    /**
     * 批量导入问题。
     * 根据文件扩展名自动识别 CSV 或 JSON 格式，
     * 返回导入结果 Map: { successCount, failCount, errors }。
     */
    Map<String, Object> importQuestions(byte[] fileBytes, String filename);

    /**
     * 导出问题为 CSV 或 JSON 字节数组。
     * @param format 导出格式: csv / json
     * @return 文件导出结果（bytes + filename + contentType）
     */
    FileExportResult exportQuestions(String format);

    /**
     * AI 生成题目 — 调用 LLM 按分类/难度/类型/数量批量生成并入库。
     *
     * @param category     分类: reasoning/coding/qa/translation/summarization
     * @param difficulty   难度: easy/medium/hard
     * @param questionType 类型: single / multi
     * @param count        生成数量 1~20
     * @return 入库的题目列表
     */
    List<QuestionVO> generate(String category, String difficulty,
                               String questionType, int count);
}
