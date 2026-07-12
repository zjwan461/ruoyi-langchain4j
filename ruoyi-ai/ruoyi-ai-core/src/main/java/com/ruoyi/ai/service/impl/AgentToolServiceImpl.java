package com.ruoyi.ai.service.impl;

import com.ruoyi.ai.domain.AgentTool;
import com.ruoyi.ai.mapper.AgentToolMapper;
import com.ruoyi.ai.service.IAgentToolService;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * agent_toolService业务层处理
 * 
 * @author Jerry
 * @date 2026-07-07
 */
@Service
public class AgentToolServiceImpl implements IAgentToolService 
{
    @Autowired
    private AgentToolMapper agentToolMapper;

    /**
     * 查询agent_tool
     * 
     * @param id agent_tool主键
     * @return agent_tool
     */
    @Override
    public AgentTool selectAgentToolById(Long id)
    {
        return agentToolMapper.selectAgentToolById(id);
    }

    /**
     * 查询agent_tool列表
     * 
     * @param agentTool agent_tool
     * @return agent_tool
     */
    @Override
    public List<AgentTool> selectAgentToolList(AgentTool agentTool)
    {
        return agentToolMapper.selectAgentToolList(agentTool);
    }

    /**
     * 新增agent_tool
     * 
     * @param agentTool agent_tool
     * @return 结果
     */
    @Override
    public int insertAgentTool(AgentTool agentTool)
    {
        agentTool.setCreateTime(DateUtils.getNowDate());
        return agentToolMapper.insertAgentTool(agentTool);
    }

    /**
     * 修改agent_tool
     * 
     * @param agentTool agent_tool
     * @return 结果
     */
    @Override
    public int updateAgentTool(AgentTool agentTool)
    {
        agentTool.setUpdateTime(DateUtils.getNowDate());
        return agentToolMapper.updateAgentTool(agentTool);
    }

    /**
     * 批量删除agent_tool
     * 
     * @param ids 需要删除的agent_tool主键
     * @return 结果
     */
    @Override
    public int deleteAgentToolByIds(Long[] ids)
    {
        return agentToolMapper.deleteAgentToolByIds(ids);
    }

    /**
     * 删除agent_tool信息
     * 
     * @param id agent_tool主键
     * @return 结果
     */
    @Override
    public int deleteAgentToolById(Long id)
    {
        return agentToolMapper.deleteAgentToolById(id);
    }

    @Override
    public List<AgentTool> selectAgentToolByAgentId(Long agentId) {
        return agentToolMapper.selectAgentToolByAgentId(agentId);
    }
}
