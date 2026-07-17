package com.ruoyi.ai.mapper;

import com.ruoyi.ai.domain.AgentFlow;

import java.util.List;

/**
 * agent_flowMapper接口
 * 
 * @author Jerry
 * @date 2026-07-17
 */
public interface AgentFlowMapper 
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
    public int insertAgentFlow(AgentFlow agentFlow);

    /**
     * 修改agent_flow
     * 
     * @param agentFlow agent_flow
     * @return 结果
     */
    public int updateAgentFlow(AgentFlow agentFlow);

    /**
     * 删除agent_flow
     * 
     * @param id agent_flow主键
     * @return 结果
     */
    public int deleteAgentFlowById(Long id);

    /**
     * 批量删除agent_flow
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAgentFlowByIds(Long[] ids);
}
