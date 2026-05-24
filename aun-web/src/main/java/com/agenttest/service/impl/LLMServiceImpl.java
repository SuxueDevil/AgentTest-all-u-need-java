package com.agenttest.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.agenttest.common.PageResult;
import com.agenttest.common.exception.BusinessException;
import com.agenttest.mapper.LLMMapper;
import com.agenttest.mapper.EvaluationResultMapper;
import com.agenttest.pojo.dto.LLMCreateDTO;
import com.agenttest.pojo.dto.LLMQueryDTO;
import com.agenttest.pojo.dto.LLMUpdateDTO;
import com.agenttest.pojo.entity.LLM;
import com.agenttest.pojo.vo.LLMVO;
import com.agenttest.service.LLMService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * LLM 模型业务实现 — 封装 LLM 的 CRUD 逻辑。
 * <p>
 * 核心职责：
 * <ul>
 *   <li>LLM 模型注册与管理（增删改查）</li>
 *   <li>分页查询时 keyword 模糊匹配 model 名称</li>
 *   <li>删除 LLM 前预留关联结果清理扩展点</li>
 * </ul>
 *
 * @author MT
 * @since 2026-05-21
 */
@Service
@RequiredArgsConstructor
public class LLMServiceImpl implements LLMService {

    private static final Logger log = LoggerFactory.getLogger(LLMServiceImpl.class);

    private final LLMMapper llmMapper;
    private final EvaluationResultMapper evaluationResultMapper;

    /**
     * 分页查询 LLM 模型列表。
     * keyword 模糊匹配 LLM 名称，按创建时间倒序。
     *
     * @param query 查询条件（keyword）及分页参数（page / pageSize）
     * @return 分页结果，含 LLMVO 列表及 total / page / pageSize
     */
    @Override
    public PageResult<LLMVO> page(LLMQueryDTO query) {
        LambdaQueryWrapper<LLM> wrapper = new LambdaQueryWrapper<LLM>()
                .like(query.getKeyword() != null && !query.getKeyword().isBlank(),
                        LLM::getName, query.getKeyword())
                .orderByDesc(LLM::getCreatedAt);

        IPage<LLM> page = llmMapper.selectPage(
                new Page<>(query.getPage(), query.getPageSize()), wrapper);

        List<LLMVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        log.info("分页查询LLM完成，共 {} 条", page.getTotal());
        return new PageResult<>(voList, page.getTotal(), query.getPage(), query.getPageSize());
    }

    /**
     * 查询单个 LLM 详情。
     *
     * @param id LLM 主键 ID
     * @return LLMVO（不含 apiKey 的脱敏视图）
     * @throws BusinessException 当 LLM 不存在时抛出，错误码 404
     */
    @Override
    public LLMVO getById(Long id) {
        return toVO(getEntityById(id));
    }

    /**
     * 创建 LLM 模型，默认状态设为 active。
     *
     * @param dto LLM 创建参数（name、model 必填），校验失败由 GlobalExceptionHandler 处理
     * @return 创建后的 LLMVO，含数据库自动生成的主键
     */
    @Override
    public LLMVO create(LLMCreateDTO dto) {
        log.info("创建LLM，name={} model={}", dto.getName(), dto.getModel());
        LLM llm = new LLM();
        BeanUtil.copyProperties(dto, llm);
        llm.setStatus("active");
        llmMapper.insert(llm);
        return toVO(llm);
    }

    /**
     * 更新 LLM 模型，DTO 中非 null 字段覆盖到 entity。
     *
     * @param id  LLM 主键 ID
     * @param dto 部分更新的字段（name / model / endpointUrl / apiKey）
     * @return 更新后的 LLMVO
     * @throws BusinessException 当 LLM 不存在时抛出，错误码 404
     */
    @Override
    public LLMVO update(Long id, LLMUpdateDTO dto) {
        log.info("更新LLM，id={}", id);
        LLM llm = getEntityById(id);
        BeanUtil.copyProperties(dto, llm);
        llmMapper.updateById(llm);
        return toVO(llm);
    }

    /**
     * 删除 LLM 模型（物理删除）。
     * <p>
     * 先校验存在、预留关联结果清理扩展点，再执行删除。
     * 若后续 evaluation_result 增加 llm_id 外键，需在此处先清理关联记录。
     *
     * @param id LLM 主键 ID
     * @throws BusinessException 当 LLM 不存在时抛出，错误码 404
     */
    @Override
    public void delete(Long id) {
        log.info("删除LLM，id={}", id);
        getEntityById(id);
        // 预留：若后续 evaluation_result 增加 llm_id 外键，此处先清理关联记录
        llmMapper.deleteById(id);
    }

    /**
     * 按 ID 查询 LLM entity，不存在时抛 BusinessException。
     *
     * @param id LLM 主键 ID
     * @return LLM 数据库实体
     * @throws BusinessException 当 LLM 不存在时抛出，错误码 404
     */
    private LLM getEntityById(Long id) {
        LLM llm = llmMapper.selectById(id);
        if (llm == null) {
            throw new BusinessException(404, "LLM不存在");
        }
        return llm;
    }

    /**
     * entity → VO 转换，使用 BeanUtil.copyProperties 自动复制同名字段。
     * LLMVO 中无 apiKey 字段，实现自动脱敏。
     */
    private LLMVO toVO(LLM entity) {
        LLMVO vo = new LLMVO();
        BeanUtil.copyProperties(entity, vo);
        return vo;
    }
}
