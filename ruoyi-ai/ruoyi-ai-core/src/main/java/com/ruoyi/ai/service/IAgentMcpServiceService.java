package com.ruoyi.ai.service;

import com.ruoyi.ai.domain.AgentMcpService;

import java.util.List;

/**
 * mcpService接口
 * 
 * @author Jerry
 * @date 2026-07-15
 */
public interface IAgentMcpServiceService 
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
     * 批量删除mcp
     * 
     * @param ids 需要删除的mcp主键集合
     * @return 结果
     */
    public int deleteAgentMcpServiceByIds(Long[] ids);

    /**
     * 删除mcp信息
     * 
     * @param id mcp主键
     * @return 结果
     */
    public int deleteAgentMcpServiceById(Long id);
}
