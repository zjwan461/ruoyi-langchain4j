package com.ruoyi.ai.mapper;

import com.ruoyi.ai.domain.AgentTool;

import java.util.List;

/**
 * agent_toolMapper接口
 * 
 * @author Jerry
 * @date 2026-07-07
 */
public interface AgentToolMapper 
{
    /**
     * 查询agent_tool
     * 
     * @param id agent_tool主键
     * @return agent_tool
     */
    public AgentTool selectAgentToolById(Long id);

    /**
     * 查询agent_tool列表
     * 
     * @param agentTool agent_tool
     * @return agent_tool集合
     */
    public List<AgentTool> selectAgentToolList(AgentTool agentTool);

    /**
     * 新增agent_tool
     * 
     * @param agentTool agent_tool
     * @return 结果
     */
    public int insertAgentTool(AgentTool agentTool);

    /**
     * 修改agent_tool
     * 
     * @param agentTool agent_tool
     * @return 结果
     */
    public int updateAgentTool(AgentTool agentTool);

    /**
     * 删除agent_tool
     * 
     * @param id agent_tool主键
     * @return 结果
     */
    public int deleteAgentToolById(Long id);

    /**
     * 批量删除agent_tool
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAgentToolByIds(Long[] ids);
}
