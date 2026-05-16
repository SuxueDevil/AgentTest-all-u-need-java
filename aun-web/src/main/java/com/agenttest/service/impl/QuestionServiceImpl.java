package com.agenttest.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.text.csv.CsvWriter;
import cn.hutool.core.io.IoUtil;
import com.agenttest.common.utils.FileExportResult;
import com.agenttest.common.PageResult;
import com.agenttest.common.exception.BusinessException;
import com.agenttest.mapper.QuestionMapper;
import com.agenttest.pojo.dto.QuestionCreateDTO;
import com.agenttest.pojo.dto.QuestionQueryDTO;
import com.agenttest.pojo.dto.QuestionUpdateDTO;
import com.agenttest.pojo.entity.Question;
import com.agenttest.pojo.vo.QuestionVO;
import com.agenttest.generator.QuestionGenerator;
import com.agenttest.generator.QuestionGenerator.GeneratedQuestion;
import com.agenttest.service.QuestionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 题库业务实现 — 封装问题的 CRUD、批量操作和导入导出逻辑。
 * <p>
 * 核心设计:
 * <ul>
 *   <li>导入支持 CSV（Hutool CsvUtil 解析）和 JSON（Jackson 解析）两种格式</li>
 *   <li>导出 CSV 用 Hutool CsvWriter，JSON 用 Jackson 序列化</li>
 *   <li>批量删除使用 MyBatis-Plus deleteBatchIds，返回实际删除条数</li>
 * </ul>
 */
@Service
public class QuestionServiceImpl implements QuestionService {

    private static final Logger log = LoggerFactory.getLogger(QuestionServiceImpl.class);

    private final QuestionMapper questionMapper;
    private final ObjectMapper objectMapper;
    private final QuestionGenerator questionGenerator;

    /** 构造器注入 */
    public QuestionServiceImpl(QuestionMapper questionMapper, ObjectMapper objectMapper,
                                QuestionGenerator questionGenerator) {
        this.questionMapper = questionMapper;
        this.objectMapper = objectMapper;
        this.questionGenerator = questionGenerator;
    }

    // ==================== CRUD ====================

    /**
     * 分页查询问题。
     * keyword 模糊匹配 title；category、difficulty、questionType 精确匹配。
     *
     * @param query 查询条件（keyword / category / difficulty / questionType）及分页参数
     * @return 分页结果，含 QuestionVO 列表及 total / page / pageSize
     */
    @Override
    public PageResult<QuestionVO> page(QuestionQueryDTO query) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        // keyword 模糊搜索 title
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.like(Question::getTitle, query.getKeyword());
        }
        // 精确筛选
        if (query.getCategory() != null && !query.getCategory().isBlank()) {
            wrapper.eq(Question::getCategory, query.getCategory());
        }
        if (query.getDifficulty() != null && !query.getDifficulty().isBlank()) {
            wrapper.eq(Question::getDifficulty, query.getDifficulty());
        }
        if (query.getQuestionType() != null && !query.getQuestionType().isBlank()) {
            wrapper.eq(Question::getQuestionType, query.getQuestionType());
        }
        wrapper.orderByDesc(Question::getCreatedAt);

        IPage<Question> page = questionMapper.selectPage(
                new Page<>(query.getPage(), query.getPageSize()), wrapper);

        List<QuestionVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        log.info("分页查询问题完成，共 {} 条", page.getTotal());
        return new PageResult<>(voList, page.getTotal(), query.getPage(), query.getPageSize());
    }

    /**
     * 查询单个问题详情。
     *
     * @param id 问题主键 ID
     * @return QuestionVO
     * @throws BusinessException 当问题不存在时抛出，错误码 404
     */
    @Override
    public QuestionVO getById(Long id) {
        log.info("查询问题详情，id={}", id);
        return toVO(getEntityById(id));
    }

    /**
     * 创建问题，difficulty 未传默认 medium，questionType 未传默认 single。
     *
     * @param dto 问题创建参数（title、category 必填）
     * @return 创建后的 QuestionVO
     */
    @Override
    public QuestionVO create(QuestionCreateDTO dto) {
        log.info("创建问题，title={} category={}", dto.getTitle(), dto.getCategory());
        Question question = new Question();
        BeanUtil.copyProperties(dto, question);
        // 默认值
        if (question.getDifficulty() == null) {
            question.setDifficulty("medium");
        }
        if (question.getQuestionType() == null) {
            question.setQuestionType("single");
        }
        questionMapper.insert(question);
        return toVO(question);
    }

    /**
     * 更新问题，DTO 非 null 字段覆盖到 entity。
     *
     * @param id  问题主键 ID
     * @param dto 部分更新的字段
     * @return 更新后的 QuestionVO
     * @throws BusinessException 当问题不存在时抛出，错误码 404
     */
    @Override
    public QuestionVO update(Long id, QuestionUpdateDTO dto) {
        log.info("更新问题，id={}", id);
        Question question = getEntityById(id);
        BeanUtil.copyProperties(dto, question);
        questionMapper.updateById(question);
        return toVO(question);
    }

    /**
     * 删除单个问题。
     *
     * @param id 问题主键 ID
     * @throws BusinessException 当问题不存在时抛出，错误码 404
     */
    @Override
    public void delete(Long id) {
        log.info("删除问题，id={}", id);
        getEntityById(id);
        questionMapper.deleteById(id);
    }

    /**
     * 批量删除问题。
     *
     * @param ids 问题主键 ID 列表
     * @return 实际删除的条数
     * @throws BusinessException 当 ids 为空时抛出，错误码 400
     */
    @Override
    public int batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(400, "请选择要删除的问题");
        }
        log.info("批量删除问题，共 {} 条", ids.size());
        return questionMapper.deleteBatchIds(ids);
    }

    // ==================== 导入导出 ====================

    /**
     * 批量导入问题。
     * 根据文件名后缀判断格式（.csv / .json），逐条解析后批量插入。
     *
     * @param fileBytes 上传文件的字节数组
     * @param filename  原始文件名，用于判断格式（支持 .csv / .json）
     * @return Map: { successCount: int, failCount: int, errors: [{row, message}] }
     * @throws BusinessException 当文件格式不支持时抛出，错误码 400
     */
    @Override
    public Map<String, Object> importQuestions(byte[] fileBytes, String filename) {
        String ext = filename.toLowerCase();
        if (ext.endsWith(".csv")) {
            return importFromCsv(fileBytes);
        } else if (ext.endsWith(".json")) {
            return importFromJson(fileBytes);
        }
        throw new BusinessException(400, "不支持的文件格式，请上传 CSV 或 JSON 文件");
    }

    /**
     * 导出全部问题为文件。
     *
     * @param format 导出格式（csv / json），默认 json
     * @return 文件导出结果（bytes + filename + contentType）
     */
    @Override
    public FileExportResult exportQuestions(String format) {
        List<Question> all = questionMapper.selectList(null);
        if ("csv".equalsIgnoreCase(format)) {
            return new FileExportResult(exportToCsv(all), "questions.csv",
                    "text/csv;charset=UTF-8");
        }
        return new FileExportResult(exportToJson(all), "questions.json",
                "application/json;charset=UTF-8");
    }

    // ==================== 导入实现 ====================

    /**
     * CSV 导入 — 首行为表头，后续每行一条问题记录。
     * 列顺序: title, category, difficulty, questionType, expectedAnswer, tags(逗号分隔)。
     *
     * @param fileBytes CSV 文件字节数组
     * @return Map: { successCount, failCount, errors: [{row, message}] }
     */
    private Map<String, Object> importFromCsv(byte[] fileBytes) {
        int successCount = 0;
        int failCount = 0;
        List<Map<String, Object>> errors = new ArrayList<>();

        String content = new String(fileBytes, StandardCharsets.UTF_8);
        CsvReader reader = CsvUtil.getReader();
        CsvData csvData = reader.read(new StringReader(content));
        List<CsvRow> rows = csvData.getRows();

        if (rows.isEmpty()) {
            throw new BusinessException(400, "CSV 文件为空");
        }

        // 跳过表头行
        for (int i = 1; i < rows.size(); i++) {
            try {
                List<String> cols = rows.get(i).getRawList();
                if (cols.size() < 2) continue; // 跳过空行

                Question question = new Question();
                question.setTitle(cols.get(0));
                question.setCategory(cols.size() > 1 ? cols.get(1) : null);
                question.setDifficulty(cols.size() > 2 ? cols.get(2) : "medium");
                question.setQuestionType(cols.size() > 3 ? cols.get(3) : "single");
                question.setExpectedAnswer(cols.size() > 4 ? cols.get(4) : null);
                // tags 用逗号分隔
                if (cols.size() > 5 && !cols.get(5).isBlank()) {
                    question.setTags(Arrays.asList(cols.get(5).split(",")));
                }
                questionMapper.insert(question);
                successCount++;
            } catch (Exception e) {
                failCount++;
                Map<String, Object> err = new HashMap<>();
                err.put("row", i);
                err.put("message", e.getMessage());
                errors.add(err);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("errors", errors);
        log.info("CSV导入完成: 成功{}条, 失败{}条", successCount, failCount);
        return result;
    }

    /**
     * JSON 导入 — 解析 JSON 数组，每项反序列化为 Question 对象后插入。
     *
     * @param fileBytes JSON 文件字节数组
     * @return Map: { successCount, failCount, errors: [{row, message}] }
     * @throws BusinessException 当 JSON 解析失败时抛出，错误码 400
     */
    private Map<String, Object> importFromJson(byte[] fileBytes) {
        int successCount = 0;
        int failCount = 0;
        List<Map<String, Object>> errors = new ArrayList<>();

        try {
            List<Question> questions = objectMapper.readValue(fileBytes,
                    new TypeReference<List<Question>>() {});

            for (int i = 0; i < questions.size(); i++) {
                try {
                    Question q = questions.get(i);
                    if (q.getDifficulty() == null) q.setDifficulty("medium");
                    if (q.getQuestionType() == null) q.setQuestionType("single");
                    questionMapper.insert(q);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    Map<String, Object> err = new HashMap<>();
                    err.put("row", i + 1);
                    err.put("message", e.getMessage());
                    errors.add(err);
                }
            }
        } catch (Exception e) {
            throw new BusinessException(400, "JSON 解析失败: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("errors", errors);
        log.info("JSON导入完成: 成功{}条, 失败{}条", successCount, failCount);
        return result;
    }

    // ==================== 导出实现 ====================

    /**
     * 导出为 CSV 字节数组，首行为表头。
     *
     * @param questions 待导出的问题列表
     * @return CSV 文件字节数组
     */
    private byte[] exportToCsv(List<Question> questions) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CsvWriter writer = CsvUtil.getWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        // 写入表头
        writer.write(new String[]{"title", "category", "difficulty", "questionType",
                "expectedAnswer", "tags"});

        for (Question q : questions) {
            String tags = q.getTags() != null ? String.join(",", q.getTags()) : "";
            writer.write(new String[]{
                    q.getTitle(),
                    q.getCategory(),
                    q.getDifficulty(),
                    q.getQuestionType(),
                    q.getExpectedAnswer() != null ? q.getExpectedAnswer() : "",
                    tags
            });
        }
        IoUtil.close(out);
        return out.toByteArray();
    }

    /**
     * 导出为 JSON 字节数组，直接序列化 entity 列表。
     *
     * @param questions 待导出的问题列表
     * @return JSON 文件字节数组
     * @throws BusinessException 当序列化失败时抛出，错误码 500
     */
    private byte[] exportToJson(List<Question> questions) {
        try {
            return objectMapper.writeValueAsBytes(questions);
        } catch (Exception e) {
            throw new BusinessException(500, "JSON 序列化失败: " + e.getMessage());
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 按 ID 查询 Question entity，不存在时抛 BusinessException。
     *
     * @param id 问题主键 ID
     * @return Question 数据库实体
     * @throws BusinessException 当问题不存在时抛出，错误码 404
     */
    private Question getEntityById(Long id) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new BusinessException(404, "问题不存在");
        }
        return question;
    }

    /**
     * entity → VO 转换，使用 Hutool BeanUtil.copyProperties 自动复制同名字段。
     *
     * @param entity Question 数据库实体
     * @return QuestionVO
     */
    private QuestionVO toVO(Question entity) {
        QuestionVO vo = new QuestionVO();
        BeanUtil.copyProperties(entity, vo);
        return vo;
    }

    // ==================== AI 生成 ====================

    @Override
    public List<QuestionVO> generate(String category, String difficulty,
                                      String questionType, int count, String topic) {
        int safeCount = Math.min(count, 20);
        List<GeneratedQuestion> generated = questionGenerator.generate(
                category, difficulty, questionType, safeCount, topic);

        List<QuestionVO> result = new ArrayList<>();
        for (GeneratedQuestion gq : generated) {
            Question q = new Question();
            q.setTitle(gq.title());
            q.setCategory(category != null ? category : "reasoning");
            q.setDifficulty(difficulty != null ? difficulty : "medium");
            q.setQuestionType(questionType != null ? questionType : "single");
            q.setExpectedAnswer(gq.expectedAnswer());
            q.setTags(gq.tags());

            // 多轮时转换 turns
            if ("multi".equals(questionType) && gq.turns() != null) {
                q.setTurns(gq.turns().stream().map(t -> {
                    Question.Turn turn = new Question.Turn();
                    turn.setTurnOrder(t.turnOrder());
                    turn.setRole(t.role());
                    turn.setContent(t.content());
                    return turn;
                }).collect(Collectors.toList()));
            }

            questionMapper.insert(q);
            result.add(toVO(q));
        }
        log.info("AI生成题目完成: 生成{}道, 入库{}道", generated.size(), result.size());
        return result;
    }
}
