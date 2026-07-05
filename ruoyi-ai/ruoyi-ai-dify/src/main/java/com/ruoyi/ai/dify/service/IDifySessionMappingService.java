package com.ruoyi.ai.dify.service;

import com.ruoyi.ai.dify.domain.DifySessionMapping;

import java.util.List;

/**
 * Dify 会话映射Service接口
 *
 * @author ruoyi
 * @date 2025-10-27
 */
public interface IDifySessionMappingService {
    /**
     * 查询Dify 会话映射
     *
     * @param sessionId 主键
     * @return Dify 会话映射
     */
    public DifySessionMapping selectDifySessionMappingBySessionId(String sessionId);

    /**
     * 查询Dify 会话映射
     *
     * @param clientId agent chat client_id
     * @return Dify 会话映射
     */
    public List<DifySessionMapping> selectDifySessionMappingByClientId(String clientId);

    /**
     * 查询Dify 会话映射
     *
     * @param sessionId agent chat session_id
     * @param clientId  agent chat client_id
     * @return Dify 会话映射
     */
    public DifySessionMapping selectDifySessionMappingBySessionIdAndClientId(String sessionId, String clientId);

    /**
     * 查询Dify 会话映射列表
     *
     * @param difySessionMapping Dify 会话映射
     * @return Dify 会话映射集合
     */
    public List<DifySessionMapping> selectDifySessionMappingList(DifySessionMapping difySessionMapping);

    /**
     * 新增Dify 会话映射
     *
     * @param difySessionMapping Dify 会话映射
     * @return 结果
     */
    public int insertDifySessionMapping(DifySessionMapping difySessionMapping);

    /**
     * 修改Dify 会话映射
     *
     * @param difySessionMapping Dify 会话映射
     * @return 结果
     */
    public int updateDifySessionMapping(DifySessionMapping difySessionMapping);

    /**
     * 批量删除Dify 会话映射
     *
     * @param sessionIds 需要删除的Dify 会话映射主键集合
     * @return 结果
     */
    public int deleteDifySessionMappingBySessionIds(String[] sessionIds);

    /**
     * 删除Dify 会话映射信息
     *
     * @param sessionId 主键
     * @return 结果
     */
    public int deleteDifySessionMappingBySessionId(String sessionId);

    /**
     * 删除Dify 会话映射信息
     *
     * @param clientId agent chat client_id
     * @return 结果
     */
    public int deleteDifySessionMappingByClientIdAndEmptyConversationId(String clientId);
}