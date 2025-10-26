package com.ruoyi.ai.mapper;

import com.ruoyi.ai.domain.KnowledgeBase;

import java.util.List;

/**
 * 知识库Mapper接口
 * 
 * @author jerry
 * @date 2025-08-25
 */
public interface KnowledgeBaseMapper 
{
    /**
     * 查询知识库
     * 
     * @param id 知识库主键
     * @return 知识库
     */
    public KnowledgeBase selectKnowledgeBaseById(Long id);

    /**
     * 查询知识库列表
     * 
     * @param knowledgeBase 知识库
     * @return 知识库集合
     */
    public List<KnowledgeBase> selectKnowledgeBaseList(KnowledgeBase knowledgeBase);

    /**
     * 新增知识库
     * 
     * @param knowledgeBase 知识库
     * @return 结果
     */
    public int insertKnowledgeBase(KnowledgeBase knowledgeBase);

    /**
     * 修改知识库
     * 
     * @param knowledgeBase 知识库
     * @return 结果
     */
    public int updateKnowledgeBase(KnowledgeBase knowledgeBase);

    /**
     * 删除知识库
     * 
     * @param id 知识库主键
     * @return 结果
     */
    public int deleteKnowledgeBaseById(Long id);

    /**
     * 批量删除知识库
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteKnowledgeBaseByIds(Long[] ids);


    public List<KnowledgeBase> selectByIds(List<Long> ids);
}
