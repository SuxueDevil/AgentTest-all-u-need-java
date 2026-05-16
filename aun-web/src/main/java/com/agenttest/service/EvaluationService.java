package com.agenttest.service;

import com.agenttest.common.PageResult;
import com.agenttest.pojo.dto.EvaluationTaskCreateDTO;
import com.agenttest.pojo.dto.EvaluationTaskQueryDTO;
import com.agenttest.pojo.dto.EvaluationTaskUpdateDTO;
import com.agenttest.pojo.vo.AgentResultVO;
import com.agenttest.pojo.vo.EvaluationTaskVO;
import com.agenttest.pojo.vo.TaskProgressVO;

import java.util.List;

/**
 * 评测任务业务接口 — 定义任务的 CRUD、执行控制、进度查询和结果聚合。
 * <p>
 * start 方法触发异步评测，cancel 中断正在执行的任务，
 * progress 供前端轮询，results 返回按 Agent 分组的聚合结果。
 */
public interface EvaluationService {

    /** 分页查询任务列表，按创建时间倒序，status 为空时不筛选 */
    PageResult<EvaluationTaskVO> page(EvaluationTaskQueryDTO query);

    /** 查询单个任务详情，不存在时抛 BusinessException(404) */
    EvaluationTaskVO getById(Long id);

    /** 创建评测任务，自动计算 questionCount，状态默认为 pending */
    EvaluationTaskVO create(EvaluationTaskCreateDTO dto);

    /** 更新任务（仅允许修改 name 和 description），running 状态的任务不可更新 */
    EvaluationTaskVO update(Long id, EvaluationTaskUpdateDTO dto);

    /** 删除任务，级联删除所有关联的评测结果 */
    void delete(Long id);

    /** 启动评测 — 将状态改为 running 并异步执行所有 Agent×题目 的评测 */
    void start(Long id);

    /** 重新开始评测 — 重置状态为 pending，清空已有结果，保留原配置 */
    void restart(Long id);

    /** 取消评测 — 将状态改为 cancelled，中断正在执行的异步任务 */
    void cancel(Long id);

    /** 获取任务进度 — 返回 status + questionCount + completedCount */
    TaskProgressVO getProgress(Long id);

    /** 获取评测结果 — 按 Agent 分组聚合，含逐题明细 */
    List<AgentResultVO> getResults(Long id);
}
