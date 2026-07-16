package com.ruoyi.ai.mapper;

import com.ruoyi.ai.domain.AIAgent;
import com.ruoyi.ai.domain.AgentMcpToolMapping;
import com.ruoyi.ai.domain.AgentSkillMapping;
import com.ruoyi.ai.domain.AgentToolMapping;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI智能体Mapper接口
 * 
 * @author jerry
 * @date 2025-09-02
 */
public interface AIAgentMapper
{
    /**
     * 查询AI智能体
     * 
     * @param id AI智能体主键
     * @return AI智能体
     */
    public AIAgent selectAiAgentById(Long id);

    /**
     * 查询AI智能体列表
     * 
     * @param aiAgent AI智能体
     * @return AI智能体集合
     */
    public List<AIAgent> selectAiAgentList(AIAgent aiAgent);

    /**
     * 新增AI智能体
     * 
     * @param aiAgent AI智能体
     * @return 结果
     */
    public int insertAiAgent(AIAgent aiAgent);

    /**
     * 修改AI智能体
     * 
     * @param aiAgent AI智能体
     * @return 结果
     */
    public int updateAiAgent(AIAgent aiAgent);

    /**
     * 删除AI智能体
     * 
     * @param id AI智能体主键
     * @return 结果
     */
    public int deleteAiAgentById(Long id);

    /**
     * 批量删除AI智能体
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAiAgentByIds(Long[] ids);


    public AIAgent selectAiAgentByVisitUrl(String visitUrl);

    public AIAgent selectAiAgentWithToolAndSkillById(Long id);

    int insertAgentToolMapping(@Param("agentId") Long agentId, @Param("toolId") Long toolId);

    int batchInsertAgentToolMapping(@Param("list") List<AgentToolMapping> mappings);

    void deleteAgentToolMappingByAgentId(@Param("agentId") Long agentId);

    int insertAgentSkillMapping(@Param("agentId") Long agentId, @Param("skillId") Long skillId);

    int batchInsertAgentSkillMapping(@Param("list") List<AgentSkillMapping> mappings);

    void deleteAgentSkillMappingByAgentId(@Param("agentId") Long agentId);

    int insertAgentMcpToolMapping(@Param("agentId") Long agentId, @Param("mcpToolId") Long mcpToolId);

    int batchInsertAgentMcpToolMapping(@Param("list") List<AgentMcpToolMapping> mappings);

    void deleteAgentMcpToolMappingByAgentId(@Param("agentId") Long agentId);
}
