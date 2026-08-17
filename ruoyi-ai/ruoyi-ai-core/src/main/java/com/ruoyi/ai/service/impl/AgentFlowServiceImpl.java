package com.ruoyi.ai.service.impl;

import com.ruoyi.ai.domain.AgentFlow;
import com.ruoyi.ai.mapper.AgentFlowMapper;
import com.ruoyi.ai.service.IAgentFlowService;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * agent_flowService业务层处理
 * 
 * @author Jerry
 * @date 2026-07-17
 */
@Service
public class AgentFlowServiceImpl implements IAgentFlowService 
{
    @Autowired
    private AgentFlowMapper agentFlowMapper;

    /**
     * 查询agent_flow
     * 
     * @param id agent_flow主键
     * @return agent_flow
     */
    @Cacheable(cacheNames = "agentFlow", key = "#id")
    @Override
    public AgentFlow selectAgentFlowById(Long id)
    {
        return agentFlowMapper.selectAgentFlowById(id);
    }

    /**
     * 查询agent_flow列表
     * 
     * @param agentFlow agent_flow
     * @return agent_flow
     */
    @Override
    public List<AgentFlow> selectAgentFlowList(AgentFlow agentFlow)
    {
        return agentFlowMapper.selectAgentFlowList(agentFlow);
    }

    /**
     * 新增agent_flow
     * 
     * @param agentFlow agent_flow
     * @return 结果
     */
    @Override
    public AgentFlow insertAgentFlow(AgentFlow agentFlow)
    {
        agentFlow.setCreateTime(DateUtils.getNowDate());
        agentFlowMapper.insertAgentFlow(agentFlow);
        return agentFlow;
    }

    /**
     * 修改agent_flow
     * 
     * @param agentFlow agent_flow
     * @return 结果
     */
    @CacheEvict(cacheNames = "agentFlow", key = "#agentFlow.id")
    @Override
    public int updateAgentFlow(AgentFlow agentFlow)
    {
        agentFlow.setUpdateTime(DateUtils.getNowDate());
        return agentFlowMapper.updateAgentFlow(agentFlow);
    }

    /**
     * 批量删除agent_flow
     *
     * @param ids 需要删除的agent_flow主键
     * @return 结果
     */
    @CacheEvict(cacheNames = "agentFlow", allEntries = true)
    @Override
    public int deleteAgentFlowByIds(Long[] ids)
    {
        return agentFlowMapper.deleteAgentFlowByIds(ids);
    }

    /**
     * 删除agent_flow信息
     *
     * @param id agent_flow主键
     * @return 结果
     */
    @CacheEvict(cacheNames = "agentFlow", key = "#id")
    @Override
    public int deleteAgentFlowById(Long id)
    {
        return agentFlowMapper.deleteAgentFlowById(id);
    }

    @Override
    public AgentFlow selectAgentFlowByVisitId(String visitId) {
        return agentFlowMapper.selectAgentFlowByVisitUrl(visitId);
    }
}
