package com.agenttest.mapper;

import com.agenttest.pojo.entity.EvaluationTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评测任务数据访问层。
 * 继承 BaseMapper<EvaluationTask>，获得 CRUD 基础能力。
 * 复杂查询（如按状态筛选分页）使用 MyBatis-Plus LambdaQueryWrapper 构造条件。
 */
@Mapper
public interface EvaluationTaskMapper extends BaseMapper<EvaluationTask> {
}
