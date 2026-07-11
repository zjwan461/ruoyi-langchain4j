package com.ruoyi.ai.mapper;

import com.ruoyi.ai.domain.AgentKbInvoke;

import java.util.List;

/**
 * agent_kb_invokeMapper接口
 *
 * @author Jerry
 * @date 2026-07-07
 */
public interface AgentKbInvokeMapper
{
    /**
     * 查询agent_kb_invoke
     *
     * @param id agent_kb_invoke主键
     * @return agent_kb_invoke
     */
    public AgentKbInvoke selectAgentKbInvokeById(Long id);

    /**
     * 查询agent_kb_invoke列表
     *
     * @param agentKbInvoke agent_kb_invoke
     * @return agent_kb_invoke集合
     */
    public List<AgentKbInvoke> selectAgentKbInvokeList(AgentKbInvoke agentKbInvoke);

    /**
     * 新增agent_kb_invoke
     *
     * @param agentKbInvoke agent_kb_invoke
     * @return 结果
     */
    public int insertAgentKbInvoke(AgentKbInvoke agentKbInvoke);

    /**
     * 修改agent_kb_invoke
     *
     * @param agentKbInvoke agent_kb_invoke
     * @return 结果
     */
    public int updateAgentKbInvoke(AgentKbInvoke agentKbInvoke);

    /**
     * 删除agent_kb_invoke
     *
     * @param id agent_kb_invoke主键
     * @return 结果
     */
    public int deleteAgentKbInvokeById(Long id);

    /**
     * 批量删除agent_kb_invoke
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAgentKbInvokeByIds(Long[] ids);
}
