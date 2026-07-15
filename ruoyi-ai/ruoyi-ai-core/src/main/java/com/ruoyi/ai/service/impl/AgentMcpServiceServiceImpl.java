package com.ruoyi.ai.service.impl;

import com.ruoyi.ai.domain.AgentMcpService;
import com.ruoyi.ai.domain.AgentMcpTool;
import com.ruoyi.ai.mapper.AgentMcpServiceMapper;
import com.ruoyi.ai.service.IAgentMcpServiceService;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * mcpService业务层处理
 * 
 * @author Jerry
 * @date 2026-07-15
 */
@Service
public class AgentMcpServiceServiceImpl implements IAgentMcpServiceService 
{
    @Autowired
    private AgentMcpServiceMapper agentMcpServiceMapper;

    /**
     * 查询mcp
     * 
     * @param id mcp主键
     * @return mcp
     */
    @Override
    public AgentMcpService selectAgentMcpServiceById(Long id)
    {
        return agentMcpServiceMapper.selectAgentMcpServiceById(id);
    }

    /**
     * 查询mcp列表
     * 
     * @param agentMcpService mcp
     * @return mcp
     */
    @Override
    public List<AgentMcpService> selectAgentMcpServiceList(AgentMcpService agentMcpService)
    {
        return agentMcpServiceMapper.selectAgentMcpServiceList(agentMcpService);
    }

    /**
     * 新增mcp
     * 
     * @param agentMcpService mcp
     * @return 结果
     */
    @Transactional
    @Override
    public int insertAgentMcpService(AgentMcpService agentMcpService)
    {
        agentMcpService.setCreateTime(DateUtils.getNowDate());
        agentMcpService.setCreateBy(SecurityUtils.getUsername());
        int rows = agentMcpServiceMapper.insertAgentMcpService(agentMcpService);
        insertAgentMcpTool(agentMcpService);
        return rows;
    }

    /**
     * 修改mcp
     * 
     * @param agentMcpService mcp
     * @return 结果
     */
    @Transactional
    @Override
    public int updateAgentMcpService(AgentMcpService agentMcpService)
    {
        agentMcpService.setUpdateTime(DateUtils.getNowDate());
        agentMcpService.setUpdateBy(SecurityUtils.getUsername());
        agentMcpServiceMapper.deleteAgentMcpToolByPid(agentMcpService.getId());
        insertAgentMcpTool(agentMcpService);
        return agentMcpServiceMapper.updateAgentMcpService(agentMcpService);
    }

    /**
     * 批量删除mcp
     * 
     * @param ids 需要删除的mcp主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteAgentMcpServiceByIds(Long[] ids)
    {
        agentMcpServiceMapper.deleteAgentMcpToolByPids(ids);
        return agentMcpServiceMapper.deleteAgentMcpServiceByIds(ids);
    }

    /**
     * 删除mcp信息
     * 
     * @param id mcp主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteAgentMcpServiceById(Long id)
    {
        agentMcpServiceMapper.deleteAgentMcpToolByPid(id);
        return agentMcpServiceMapper.deleteAgentMcpServiceById(id);
    }

    /**
     * 新增agent_mcp_tool信息
     * 
     * @param agentMcpService mcp对象
     */
    public void insertAgentMcpTool(AgentMcpService agentMcpService)
    {
        List<AgentMcpTool> agentMcpToolList = agentMcpService.getAgentMcpToolList();
        Long id = agentMcpService.getId();
        if (StringUtils.isNotNull(agentMcpToolList))
        {
            List<AgentMcpTool> list = new ArrayList<AgentMcpTool>();
            for (AgentMcpTool agentMcpTool : agentMcpToolList)
            {
                agentMcpTool.setPid(id);
                agentMcpTool.setCreateTime(new Date());
                agentMcpTool.setCreateBy(SecurityUtils.getUsername());
                list.add(agentMcpTool);
            }
            if (list.size() > 0)
            {
                agentMcpServiceMapper.batchAgentMcpTool(list);
            }
        }
    }
}
