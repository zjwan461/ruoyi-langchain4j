package com.ruoyi.ai.service;

import com.ruoyi.ai.domain.*;

import java.util.List;

/**
 * AI智能体Service接口
 *
 * @author jerry
 * @date 2025-09-02
 */
public interface IAIAgentService {
    /**
     * 查询AI智能体
     *
     * @param id AI智能体主键
     * @return AI智能体
     */
    public AIAgent selectAiAgentById(Long id);

    /**
     * 查询AI智能体,含tool
     *
     * @param id AI智能体主键
     * @return AI智能体
     */
    public AIAgent selectAiAgentWithToolAndSkillById(Long id);

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
     * 批量删除AI智能体
     *
     * @param ids 需要删除的AI智能体主键集合
     * @return 结果
     */
    public int deleteAiAgentByIds(Long[] ids);

    /**
     * 删除AI智能体信息
     *
     * @param id AI智能体主键
     * @return 结果
     */
    public int deleteAiAgentById(Long id);

    public AIAgent selectByVisitId(String visitId);

    public int insertAgentToolMapping(Long agentId, Long toolId);

    public int batchInsertAgentToolMapping(List<AgentToolMapping> mappings);

    public int batchInsertAgentSkillMapping(List<AgentSkillMapping> mappings);

    public int insertAgentKBInvoked(AgentKbInvoke agentKbInvoke);

    public int insertAgentToolInvoked(AgentToolInvoke agentToolInvoke);

    public int batchInsertAgentMcpToolMapping(List<AgentMcpToolMapping> mappings);
}
