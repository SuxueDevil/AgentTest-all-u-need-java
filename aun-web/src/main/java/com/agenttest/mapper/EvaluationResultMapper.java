package com.agenttest.mapper;

import com.agenttest.pojo.entity.EvaluationResult;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评测结果数据访问层。
 * 继承 BaseMapper<EvaluationResult>，获得 CRUD 基础能力。
 * 查询单条: selectOne(LambdaQueryWrapper) 按 taskId + agentId + questionId 定位。
 */
@Mapper
public interface EvaluationResultMapper extends BaseMapper<EvaluationResult> {
}
