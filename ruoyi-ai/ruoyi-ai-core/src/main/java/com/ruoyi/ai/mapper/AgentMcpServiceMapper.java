package com.ruoyi.ai.mapper;

import com.ruoyi.ai.domain.AgentMcpService;
import com.ruoyi.ai.domain.AgentMcpTool;

import java.util.List;

/**
 * mcpMapper接口
 * 
 * @author Jerry
 * @date 2026-07-15
 */
public interface AgentMcpServiceMapper 
{
    /**
     * 查询mcp
     * 
     * @param id mcp主键
     * @return mcp
     */
    public AgentMcpService selectAgentMcpServiceById(Long id);

    /**
     * 查询mcp列表
     * 
     * @param agentMcpService mcp
     * @return mcp集合
     */
    public List<AgentMcpService> selectAgentMcpServiceList(AgentMcpService agentMcpService);

    /**
     * 新增mcp
     * 
     * @param agentMcpService mcp
     * @return 结果
     */
    public int insertAgentMcpService(AgentMcpService agentMcpService);

    /**
     * 修改mcp
     * 
     * @param agentMcpService mcp
     * @return 结果
     */
    public int updateAgentMcpService(AgentMcpService agentMcpService);

    /**
     * 删除mcp
     * 
     * @param id mcp主键
     * @return 结果
     */
    public int deleteAgentMcpServiceById(Long id);

    /**
     * 批量删除mcp
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAgentMcpServiceByIds(Long[] ids);

    /**
     * 批量删除agent_mcp_tool
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAgentMcpToolByPids(Long[] ids);
    
    /**
     * 批量新增agent_mcp_tool
     * 
     * @param agentMcpToolList agent_mcp_tool列表
     * @return 结果
     */
    public int batchAgentMcpTool(List<AgentMcpTool> agentMcpToolList);
    

    /**
     * 通过mcp主键删除agent_mcp_tool信息
     * 
     * @param id mcpID
     * @return 结果
     */
    public int deleteAgentMcpToolByPid(Long id);
}
