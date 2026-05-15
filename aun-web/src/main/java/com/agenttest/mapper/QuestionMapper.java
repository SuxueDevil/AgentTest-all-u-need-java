package com.agenttest.mapper;

import com.agenttest.pojo.entity.Question;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 题库数据访问层。
 * 继承 MyBatis-Plus 的 BaseMapper<Question>，自动获得 CRUD 方法，
 * 无需额外定义接口方法即可满足当前需求。
 */
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
}
