package com.agenttest.mapper;

import com.agenttest.pojo.entity.Agent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent 数据访问层。
 * 继承 MyBatis-Plus 的 BaseMapper<Agent>，自动获得:
 * <ul>
 *   <li>selectById / selectList / selectPage — 查询</li>
 *   <li>insert — 插入</li>
 *   <li>updateById — 按 ID 更新</li>
 *   <li>deleteById — 按 ID 删除</li>
 * </ul>
 * 复杂查询可在此接口中自定义方法，配合 XML 或注解 SQL。
 */
@Mapper
public interface AgentMapper extends BaseMapper<Agent> {
}
