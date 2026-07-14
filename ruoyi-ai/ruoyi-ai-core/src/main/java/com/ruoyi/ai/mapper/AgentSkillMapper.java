package com.ruoyi.ai.mapper;

import com.ruoyi.ai.domain.AgentSkill;

import java.util.List;

/**
 * agent_skillMapper接口
 * 
 * @author Jerry
 * @date 2026-07-13
 */
public interface AgentSkillMapper 
{
    /**
     * 查询agent_skill
     * 
     * @param id agent_skill主键
     * @return agent_skill
     */
    public AgentSkill selectAgentSkillById(Long id);

    /**
     * 查询agent_skill列表
     * 
     * @param agentSkill agent_skill
     * @return agent_skill集合
     */
    public List<AgentSkill> selectAgentSkillList(AgentSkill agentSkill);

    /**
     * 新增agent_skill
     * 
     * @param agentSkill agent_skill
     * @return 结果
     */
    public int insertAgentSkill(AgentSkill agentSkill);

    /**
     * 修改agent_skill
     * 
     * @param agentSkill agent_skill
     * @return 结果
     */
    public int updateAgentSkill(AgentSkill agentSkill);

    /**
     * 删除agent_skill
     * 
     * @param id agent_skill主键
     * @return 结果
     */
    public int deleteAgentSkillById(Long id);

    /**
     * 批量删除agent_skill
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAgentSkillByIds(Long[] ids);


    List<AgentSkill> selectAgentSkillByNames(List<String> names);

    public int deleteAgentSkillByNames(List<String> names);

    public int batchInsertAgentSkill(List<AgentSkill> agentSkills);

    List<AgentSkill> selectAgentSkillByAgentId(Long agentId);
}
