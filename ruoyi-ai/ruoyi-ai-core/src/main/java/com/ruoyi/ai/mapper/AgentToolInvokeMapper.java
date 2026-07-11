package com.ruoyi.ai.mapper;

import com.ruoyi.ai.domain.AgentToolInvoke;

import java.util.List;

/**
 * agent_tool_invokeMapper接口
 *
 * @author Jerry
 * @date 2026-07-07
 */
public interface AgentToolInvokeMapper
{
    /**
     * 查询agent_tool_invoke
     *
     * @param id agent_tool_invoke主键
     * @return agent_tool_invoke
     */
    public AgentToolInvoke selectAgentToolInvokeById(Long id);

    /**
     * 查询agent_tool_invoke列表
     *
     * @param agentToolInvoke agent_tool_invoke
     * @return agent_tool_invoke集合
     */
    public List<AgentToolInvoke> selectAgentToolInvokeList(AgentToolInvoke agentToolInvoke);

    /**
     * 新增agent_tool_invoke
     *
     * @param agentToolInvoke agent_tool_invoke
     * @return 结果
     */
    public int insertAgentToolInvoke(AgentToolInvoke agentToolInvoke);

    /**
     * 修改agent_tool_invoke
     *
     * @param agentToolInvoke agent_tool_invoke
     * @return 结果
     */
    public int updateAgentToolInvoke(AgentToolInvoke agentToolInvoke);

    /**
     * 删除agent_tool_invoke
     *
     * @param id agent_tool_invoke主键
     * @return 结果
     */
    public int deleteAgentToolInvokeById(Long id);

    /**
     * 批量删除agent_tool_invoke
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAgentToolInvokeByIds(Long[] ids);
}
