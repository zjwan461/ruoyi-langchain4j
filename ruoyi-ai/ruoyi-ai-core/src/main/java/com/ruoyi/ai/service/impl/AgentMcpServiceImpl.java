package com.ruoyi.ai.service.impl;

import com.ruoyi.ai.domain.AgentMcpService;
import com.ruoyi.ai.domain.AgentMcpTool;
import com.ruoyi.ai.mapper.AgentMcpMapper;
import com.ruoyi.ai.service.IAgentMcpService;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
public class AgentMcpServiceImpl implements IAgentMcpService
{
    @Autowired
    private AgentMcpMapper agentMcpMapper;

    /**
     * 查询mcp
     * 
     * @param id mcp主键
     * @return mcp
     */
    @Override
    public AgentMcpService selectAgentMcpServiceById(Long id)
    {
        return agentMcpMapper.selectAgentMcpServiceById(id);
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
        return agentMcpMapper.selectAgentMcpServiceList(agentMcpService);
    }

    /**
     * 新增mcp
     *
     * @param agentMcpService mcp
     * @return 结果
     */
    @CacheEvict(cacheNames = "agentMcpService", allEntries = true)
    @Transactional
    @Override
    public int insertAgentMcpService(AgentMcpService agentMcpService)
    {
        agentMcpService.setCreateTime(DateUtils.getNowDate());
        agentMcpService.setCreateBy(SecurityUtils.getUsername());
        int rows = agentMcpMapper.insertAgentMcpService(agentMcpService);
        insertAgentMcpTool(agentMcpService);
        return rows;
    }

    /**
     * 修改mcp
     *
     * @param agentMcpService mcp
     * @return 结果
     */
    @CacheEvict(cacheNames = "agentMcpService", allEntries = true)
    @Transactional
    @Override
    public int updateAgentMcpService(AgentMcpService agentMcpService)
    {
        agentMcpService.setUpdateTime(DateUtils.getNowDate());
        agentMcpService.setUpdateBy(SecurityUtils.getUsername());
        agentMcpMapper.deleteAgentMcpToolByPid(agentMcpService.getId());
        insertAgentMcpTool(agentMcpService);
        return agentMcpMapper.updateAgentMcpService(agentMcpService);
    }

    /**
     * 批量删除mcp
     *
     * @param ids 需要删除的mcp主键
     * @return 结果
     */
    @CacheEvict(cacheNames = "agentMcpService", allEntries = true)
    @Transactional
    @Override
    public int deleteAgentMcpServiceByIds(Long[] ids)
    {
        agentMcpMapper.deleteAgentMcpToolByPids(ids);
        return agentMcpMapper.deleteAgentMcpServiceByIds(ids);
    }

    /**
     * 删除mcp信息
     *
     * @param id mcp主键
     * @return 结果
     */
    @CacheEvict(cacheNames = "agentMcpService", allEntries = true)
    @Transactional
    @Override
    public int deleteAgentMcpServiceById(Long id)
    {
        agentMcpMapper.deleteAgentMcpToolByPid(id);
        return agentMcpMapper.deleteAgentMcpServiceById(id);
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
                agentMcpMapper.batchAgentMcpTool(list);
            }
        }
    }

    @Override
    public List<AgentMcpTool> selectAgentMcpToolList(Long mcpServiceId) {
        return agentMcpMapper.selectAgentMcpToolList(mcpServiceId);
    }

    @Override
    public List<AgentMcpTool> selectAgentMcpServiceByAgentId(Long agentId) {
        return agentMcpMapper.selectAgentMcpServiceByAgentId(agentId);
    }

    @Cacheable(cacheNames = "agentMcpService", key = "#ids")
    @Override
    public List<AgentMcpService> selectAgentMcpServiceByIds(Long[] ids) {
        return agentMcpMapper.selectAgentMcpServiceByIds(ids);
    }
}
