package com.agenttest.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.agenttest.common.PageResult;
import com.agenttest.common.exception.BusinessException;
import com.agenttest.mapper.AgentMapper;
import com.agenttest.pojo.dto.AgentCreateDTO;
import com.agenttest.pojo.dto.AgentQueryDTO;
import com.agenttest.pojo.dto.AgentUpdateDTO;
import com.agenttest.pojo.entity.Agent;
import com.agenttest.pojo.vo.AgentVO;
import com.agenttest.service.AgentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Agent 业务实现 — 封装 Agent CRUD 和外部 API 连接测试的完整逻辑。
 * <p>
 * 核心设计:
 * <ul>
 *   <li>所有公开方法返回 AgentVO，entity → VO 转换在私有方法 toVO() 中完成</li>
 *   <li>authCredential 始终在服务端使用，不会通过 VO 泄露给前端</li>
 *   <li>调用外部 Agent API 时使用 RestTemplate，支持 bearer / api_key / basic 三种鉴权</li>
 * </ul>
 */
@Service
public class AgentServiceImpl implements AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentServiceImpl.class);

    private final AgentMapper agentMapper;
    private final RestTemplate restTemplate;

    /** 构造器注入，Spring 自动装配 AgentMapper 和 RestTemplate */
    public AgentServiceImpl(AgentMapper agentMapper, RestTemplate restTemplate) {
        this.agentMapper = agentMapper;
        this.restTemplate = restTemplate;
    }

    // ==================== 公开方法 ====================

    /**
     * 分页查询 Agent。
     * keyword 同时模糊匹配 name 和 description 字段；
     * type 和 status 为精确匹配，为空时不作为筛选条件。
     */
    @Override
    public PageResult<AgentVO> page(AgentQueryDTO query) {
        // 构建动态查询条件
        LambdaQueryWrapper<Agent> wrapper = new LambdaQueryWrapper<>();
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.and(w -> w.like(Agent::getName, query.getKeyword())
                    .or().like(Agent::getDescription, query.getKeyword()));
        }
        if (query.getType() != null && !query.getType().isBlank()) {
            wrapper.eq(Agent::getType, query.getType());
        }
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            wrapper.eq(Agent::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(Agent::getCreatedAt);

        // 执行分页查询
        IPage<Agent> page = agentMapper.selectPage(
                new Page<>(query.getPage(), query.getPageSize()), wrapper);

        // entity 列表 → VO 列表
        List<AgentVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, page.getTotal(),
                query.getPage(), query.getPageSize());
    }

    /** 查询单个 Agent 详情，不存在抛 BusinessException(404) */
    @Override
    public AgentVO getById(Long id) {
        return toVO(getEntityById(id));
    }

    /** 创建 Agent，默认状态设为 active，返回 VO */
    @Override
    public AgentVO create(AgentCreateDTO dto) {
        Agent agent = new Agent();
        BeanUtil.copyProperties(dto, agent);
        agent.setStatus("active");
        agentMapper.insert(agent);
        return toVO(agent);
    }

    /** 更新 Agent，使用 BeanUtil.copyProperties 将 DTO 非 null 字段复制到 entity */
    @Override
    public AgentVO update(Long id, AgentUpdateDTO dto) {
        Agent agent = getEntityById(id);
        BeanUtil.copyProperties(dto, agent);
        agentMapper.updateById(agent);
        return toVO(agent);
    }

    /** 删除 Agent，先校验存在再执行删除 */
    @Override
    public void delete(Long id) {
        getEntityById(id);           // 不存在会抛异常
        agentMapper.deleteById(id);
    }

    /**
     * 测试 Agent API 连通性。
     * 向 agent.endpointUrl 发送 POST 请求，body 为一条简单的 ping 消息。
     * 根据 agent.authType 自动设置请求头鉴权。
     * 请求成功（2xx）返回 true，任何异常（超时、网络错误、非 2xx）返回 false。
     */
    @Override
    public boolean testConnection(Long id) {
        Agent agent = getEntityById(id);
        if (agent.getEndpointUrl() == null || agent.getEndpointUrl().isBlank()) {
            throw new BusinessException(400, "Agent未配置endpoint URL");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            applyAuth(headers, agent);

            // 发送最小化 ping 请求，max_tokens=5 限制开销
            String body = "{\"messages\":[{\"role\":\"user\",\"content\":\"ping\"}],\"max_tokens\":5}";
            HttpEntity<String> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    agent.getEndpointUrl(), HttpMethod.POST, request, String.class);

            boolean success = response.getStatusCode().is2xxSuccessful();
            log.info("Agent连接测试: id={}, name={}, success={}", id, agent.getName(), success);
            return success;
        } catch (Exception e) {
            log.warn("Agent连接测试失败: id={}, name={}, error={}", id, agent.getName(), e.getMessage());
            return false;
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 按 ID 查询 Agent entity，不存在时抛出 BusinessException。
     * 私有方法，供内部逻辑使用（testConnection 等需要 entity 的 authCredential）。
     */
    private Agent getEntityById(Long id) {
        Agent agent = agentMapper.selectById(id);
        if (agent == null) {
            throw new BusinessException(404, "Agent不存在");
        }
        return agent;
    }

    /**
     * entity → VO 转换。
     * 使用 Hutool BeanUtil.copyProperties 自动复制同名字段，
     * authCredential 因 VO 中无此字段而自动跳过，从而实现安全脱敏。
     */
    private AgentVO toVO(Agent entity) {
        AgentVO vo = new AgentVO();
        BeanUtil.copyProperties(entity, vo);
        return vo;
    }

    /**
     * 根据 Agent 的 authType 向 HTTP 请求头注入对应的鉴权信息。
     * <ul>
     *   <li>bearer  → Authorization: Bearer xxx</li>
     *   <li>api_key → X-API-Key: xxx</li>
     *   <li>basic   → Authorization: Basic base64(xxx)</li>
     *   <li>none / 其他 → 不添加鉴权头</li>
     * </ul>
     */
    private void applyAuth(HttpHeaders headers, Agent agent) {
        if ("bearer".equalsIgnoreCase(agent.getAuthType())) {
            headers.setBearerAuth(agent.getAuthCredential());
        } else if ("api_key".equalsIgnoreCase(agent.getAuthType())) {
            headers.set("X-API-Key", agent.getAuthCredential());
        } else if ("basic".equalsIgnoreCase(agent.getAuthType())) {
            headers.setBasicAuth(agent.getAuthCredential());
        }
    }
}
