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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * LLM 业务实现 — 封装 LLM 模型的 CRUD 逻辑。
 */
@Service
public class LLMServiceImpl implements LLMService {

    private static final Logger log = LoggerFactory.getLogger(LLMServiceImpl.class);

    private final LLMMapper llmMapper;
    private final EvaluationResultMapper evaluationResultMapper;

    public LLMServiceImpl(LLMMapper llmMapper, EvaluationResultMapper evaluationResultMapper) {
        this.llmMapper = llmMapper;
        this.evaluationResultMapper = evaluationResultMapper;
    }

    @Override
    public PageResult<LLMVO> page(LLMQueryDTO query) {
        LambdaQueryWrapper<LLM> wrapper = new LambdaQueryWrapper<>();
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.like(LLM::getName, query.getKeyword());
        }
        wrapper.orderByDesc(LLM::getCreatedAt);

        IPage<LLM> page = llmMapper.selectPage(
                new Page<>(query.getPage(), query.getPageSize()), wrapper);

        List<LLMVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        log.info("分页查询LLM完成，共 {} 条", page.getTotal());
        return new PageResult<>(voList, page.getTotal(), query.getPage(), query.getPageSize());
    }

    @Override
    public LLMVO getById(Long id) {
        return toVO(getEntityById(id));
    }

    @Override
    public LLMVO create(LLMCreateDTO dto) {
        log.info("创建LLM，name={} model={}", dto.getName(), dto.getModel());
        LLM llm = new LLM();
        BeanUtil.copyProperties(dto, llm);
        llm.setStatus("active");
        llmMapper.insert(llm);
        return toVO(llm);
    }

    @Override
    public LLMVO update(Long id, LLMUpdateDTO dto) {
        log.info("更新LLM，id={}", id);
        LLM llm = getEntityById(id);
        BeanUtil.copyProperties(dto, llm);
        llmMapper.updateById(llm);
        return toVO(llm);
    }

    @Override
    public void delete(Long id) {
        log.info("删除LLM，id={}", id);
        getEntityById(id);
        // 预留：若后续 evaluation_result 增加 llm_id 外键，此处先清理关联记录
        llmMapper.deleteById(id);
    }

    private LLM getEntityById(Long id) {
        LLM llm = llmMapper.selectById(id);
        if (llm == null) {
            throw new BusinessException(404, "LLM不存在");
        }
        return llm;
    }

    private LLMVO toVO(LLM entity) {
        LLMVO vo = new LLMVO();
        BeanUtil.copyProperties(entity, vo);
        return vo;
    }
}
