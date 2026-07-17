package com.ruoyi.ai.service;

import com.ruoyi.ai.domain.AgentFlow;

import java.util.List;

/**
 * agent_flowService接口
 * 
 * @author Jerry
 * @date 2026-07-17
 */
public interface IAgentFlowService 
{
    /**
     * 查询agent_flow
     * 
     * @param id agent_flow主键
     * @return agent_flow
     */
    public AgentFlow selectAgentFlowById(Long id);

    /**
     * 查询agent_flow列表
     * 
     * @param agentFlow agent_flow
     * @return agent_flow集合
     */
    public List<AgentFlow> selectAgentFlowList(AgentFlow agentFlow);

    /**
     * 新增agent_flow
     * 
     * @param agentFlow agent_flow
     * @return 结果
     */
    public AgentFlow insertAgentFlow(AgentFlow agentFlow);

    /**
     * 修改agent_flow
     * 
     * @param agentFlow agent_flow
     * @return 结果
     */
    public int updateAgentFlow(AgentFlow agentFlow);

    /**
     * 批量删除agent_flow
     * 
     * @param ids 需要删除的agent_flow主键集合
     * @return 结果
     */
    public int deleteAgentFlowByIds(Long[] ids);

    /**
     * 删除agent_flow信息
     * 
     * @param id agent_flow主键
     * @return 结果
     */
    public int deleteAgentFlowById(Long id);
}
