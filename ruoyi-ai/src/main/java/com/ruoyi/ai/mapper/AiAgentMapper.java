package com.ruoyi.ai.mapper;

import java.util.List;
import com.ruoyi.ai.domain.AiAgent;

/**
 * AI智能体Mapper接口
 * 
 * @author jerry
 * @date 2025-09-02
 */
public interface AiAgentMapper 
{
    /**
     * 查询AI智能体
     * 
     * @param id AI智能体主键
     * @return AI智能体
     */
    public AiAgent selectAiAgentById(Long id);

    /**
     * 查询AI智能体列表
     * 
     * @param aiAgent AI智能体
     * @return AI智能体集合
     */
    public List<AiAgent> selectAiAgentList(AiAgent aiAgent);

    /**
     * 新增AI智能体
     * 
     * @param aiAgent AI智能体
     * @return 结果
     */
    public int insertAiAgent(AiAgent aiAgent);

    /**
     * 修改AI智能体
     * 
     * @param aiAgent AI智能体
     * @return 结果
     */
    public int updateAiAgent(AiAgent aiAgent);

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


    public AiAgent selectAiAgentByVisitUrl(String visitUrl);
}
