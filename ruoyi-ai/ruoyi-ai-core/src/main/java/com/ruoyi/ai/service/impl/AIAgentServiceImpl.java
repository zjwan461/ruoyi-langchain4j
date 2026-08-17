package com.ruoyi.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.ruoyi.ai.domain.*;
import com.ruoyi.ai.mapper.AIAgentMapper;
import com.ruoyi.ai.mapper.AgentKbInvokeMapper;
import com.ruoyi.ai.mapper.AgentToolInvokeMapper;
import com.ruoyi.ai.service.IAIAgentService;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * AI智能体Service业务层处理
 *
 * @author jerry
 * @date 2025-09-02
 */
@Service
public class AIAgentServiceImpl implements IAIAgentService {
    @Autowired
    private AIAgentMapper aiAgentMapper;

    @Resource
    private AgentKbInvokeMapper agentKbInvokeMapper;

    @Resource
    private AgentToolInvokeMapper agentToolInvokeMapper;

    /**
     * 查询AI智能体
     *
     * @param id AI智能体主键
     * @return AI智能体
     */
    @Cacheable(cacheNames = "agent", key = "#id")
    @Override
    public AIAgent selectAiAgentById(Long id) {
        return aiAgentMapper.selectAiAgentById(id);
    }

    /**
     * 查询AI智能体,含tool
     *
     * @param id AI智能体主键
     * @return AI智能体
     */
    @Cacheable(cacheNames = "agentWithData", key = "#id")
    @Override
    public AIAgent selectAiAgentWithToolAndSkillById(Long id) {
        return aiAgentMapper.selectAiAgentWithToolAndSkillById(id);
    }

    /**
     * 查询AI智能体列表
     *
     * @param aiAgent AI智能体
     * @return AI智能体
     */
    @Override
    public List<AIAgent> selectAiAgentList(AIAgent aiAgent) {
        return aiAgentMapper.selectAiAgentList(aiAgent);
    }

    /**
     * 新增AI智能体
     *
     * @param aiAgent AI智能体
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int insertAiAgent(AIAgent aiAgent) {
        aiAgent.setCreateTime(DateUtils.getNowDate());
        aiAgent.setCreateBy(SecurityUtils.getUsername());
        int insertCount = aiAgentMapper.insertAiAgent(aiAgent);
        updateAgentToolMapping(aiAgent);
        updateAgentSkillMapping(aiAgent);
        updateAgentMcpToolMapping(aiAgent);
        return insertCount;
    }

    /**
     * 修改AI智能体
     *
     * @param aiAgent AI智能体
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int updateAiAgent(AIAgent aiAgent) {
        aiAgent.setUpdateTime(DateUtils.getNowDate());
        aiAgent.setUpdateBy(SecurityUtils.getUsername());
        int updateCount = aiAgentMapper.updateAiAgent(aiAgent);
        updateAgentToolMapping(aiAgent);
        updateAgentSkillMapping(aiAgent);
        updateAgentMcpToolMapping(aiAgent);
        return updateCount;
    }

    private void updateAgentToolMapping(AIAgent aiAgent) {
        if (StringUtils.isNotBlank(aiAgent.getToolIds())) {
            List<AgentToolMapping> mappings = Arrays.stream(aiAgent.getToolIds().split(",")).map(x -> {
                AgentToolMapping mapping = new AgentToolMapping();
                mapping.setAgentId(aiAgent.getId());
                mapping.setToolId(Long.valueOf(x));
                return mapping;
            }).toList();
            if (CollUtil.isNotEmpty(mappings)) {
                aiAgentMapper.deleteAgentToolMappingByAgentId(aiAgent.getId());
                this.batchInsertAgentToolMapping(mappings);
            }
        } else {
            aiAgentMapper.deleteAgentToolMappingByAgentId(aiAgent.getId());
        }
    }

    private void updateAgentSkillMapping(AIAgent aiAgent) {
        if (StringUtils.isNotBlank(aiAgent.getSkillIds())) {
            List<AgentSkillMapping> mappings = Arrays.stream(aiAgent.getSkillIds().split(",")).map(x -> {
                AgentSkillMapping mapping = new AgentSkillMapping();
                mapping.setAgentId(aiAgent.getId());
                mapping.setSkillId(Long.valueOf(x));
                return mapping;
            }).toList();
            if (CollUtil.isNotEmpty(mappings)) {
                aiAgentMapper.deleteAgentSkillMappingByAgentId(aiAgent.getId());
                this.batchInsertAgentSkillMapping(mappings);
            }
        } else {
            aiAgentMapper.deleteAgentSkillMappingByAgentId(aiAgent.getId());
        }
    }

    private void updateAgentMcpToolMapping(AIAgent aiAgent) {
        if (StringUtils.isNotBlank(aiAgent.getMcpToolIds())) {
            List<AgentMcpToolMapping> mappings = Arrays.stream(aiAgent.getMcpToolIds().split(",")).map(x -> {
                AgentMcpToolMapping mapping = new AgentMcpToolMapping();
                mapping.setAgentId(aiAgent.getId());
                mapping.setMcpToolId(Long.valueOf(x));
                return mapping;
            }).toList();
            if (CollUtil.isNotEmpty(mappings)) {
                aiAgentMapper.deleteAgentMcpToolMappingByAgentId(aiAgent.getId());
                this.batchInsertAgentMcpToolMapping(mappings);
            }
        } else {
            aiAgentMapper.deleteAgentMcpToolMappingByAgentId(aiAgent.getId());
        }
    }

    /**
     * 批量删除AI智能体
     *
     * @param ids 需要删除的AI智能体主键
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int deleteAiAgentByIds(Long[] ids) {
        for (Long id : ids) {
            aiAgentMapper.deleteAgentToolMappingByAgentId(id);
            aiAgentMapper.deleteAgentSkillMappingByAgentId(id);
        }
        return aiAgentMapper.deleteAiAgentByIds(ids);
    }

    /**
     * 删除AI智能体信息
     *
     * @param id AI智能体主键
     * @return 结果
     */
    @Override
    public int deleteAiAgentById(Long id) {
        return aiAgentMapper.deleteAiAgentById(id);
    }

    @Override
    public AIAgent selectByVisitId(String visitId) {
        return aiAgentMapper.selectAiAgentByVisitUrl(visitId);
    }

    @Override
    public int insertAgentToolMapping(Long agentId, Long toolId) {
        return aiAgentMapper.insertAgentToolMapping(agentId, toolId);
    }

    @Override
    public int batchInsertAgentToolMapping(List<AgentToolMapping> mappings) {
        return aiAgentMapper.batchInsertAgentToolMapping(mappings);
    }

    @Override
    public int batchInsertAgentSkillMapping(List<AgentSkillMapping> mappings) {
        return aiAgentMapper.batchInsertAgentSkillMapping(mappings);
    }

    @Override
    public int insertAgentKBInvoked(AgentKbInvoke agentKbInvoke) {
        return agentKbInvokeMapper.insertAgentKbInvoke(agentKbInvoke);
    }

    @Override
    public int insertAgentToolInvoked(AgentToolInvoke agentToolInvoke) {
        return agentToolInvokeMapper.insertAgentToolInvoke(agentToolInvoke);
    }

    @Override
    public int batchInsertAgentMcpToolMapping(List<AgentMcpToolMapping> mappings) {
        return aiAgentMapper.batchInsertAgentMcpToolMapping(mappings);
    }
}
