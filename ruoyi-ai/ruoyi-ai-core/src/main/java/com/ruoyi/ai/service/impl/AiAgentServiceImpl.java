package com.ruoyi.ai.service.impl;

import com.ruoyi.common.utils.SecurityUtils;
import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.ai.mapper.AiAgentMapper;
import com.ruoyi.ai.domain.AiAgent;
import com.ruoyi.ai.service.IAiAgentService;

/**
 * AI智能体Service业务层处理
 * 
 * @author jerry
 * @date 2025-09-02
 */
@Service
public class AiAgentServiceImpl implements IAiAgentService 
{
    @Autowired
    private AiAgentMapper aiAgentMapper;

    /**
     * 查询AI智能体
     * 
     * @param id AI智能体主键
     * @return AI智能体
     */
    @Override
    public AiAgent selectAiAgentById(Long id)
    {
        return aiAgentMapper.selectAiAgentById(id);
    }

    /**
     * 查询AI智能体列表
     * 
     * @param aiAgent AI智能体
     * @return AI智能体
     */
    @Override
    public List<AiAgent> selectAiAgentList(AiAgent aiAgent)
    {
        return aiAgentMapper.selectAiAgentList(aiAgent);
    }

    /**
     * 新增AI智能体
     * 
     * @param aiAgent AI智能体
     * @return 结果
     */
    @Override
    public int insertAiAgent(AiAgent aiAgent)
    {
        aiAgent.setCreateTime(DateUtils.getNowDate());
        aiAgent.setCreateBy(SecurityUtils.getUsername());
        return aiAgentMapper.insertAiAgent(aiAgent);
    }

    /**
     * 修改AI智能体
     * 
     * @param aiAgent AI智能体
     * @return 结果
     */
    @Override
    public int updateAiAgent(AiAgent aiAgent)
    {
        aiAgent.setUpdateTime(DateUtils.getNowDate());
        aiAgent.setUpdateBy(SecurityUtils.getUsername());
        return aiAgentMapper.updateAiAgent(aiAgent);
    }

    /**
     * 批量删除AI智能体
     * 
     * @param ids 需要删除的AI智能体主键
     * @return 结果
     */
    @Override
    public int deleteAiAgentByIds(Long[] ids)
    {
        return aiAgentMapper.deleteAiAgentByIds(ids);
    }

    /**
     * 删除AI智能体信息
     * 
     * @param id AI智能体主键
     * @return 结果
     */
    @Override
    public int deleteAiAgentById(Long id)
    {
        return aiAgentMapper.deleteAiAgentById(id);
    }

    @Override
    public AiAgent selectByVisitId(String visitId) {
        return aiAgentMapper.selectAiAgentByVisitUrl("/ai-chat/" + visitId);
    }
}
