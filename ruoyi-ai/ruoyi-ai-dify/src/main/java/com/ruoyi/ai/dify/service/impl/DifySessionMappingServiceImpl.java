package com.ruoyi.ai.dify.service.impl;

import com.ruoyi.ai.dify.domain.DifySessionMapping;
import com.ruoyi.ai.dify.mapper.DifySessionMappingMapper;
import com.ruoyi.ai.dify.service.IDifySessionMappingService;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Dify 会话映射Service业务层处理
 *
 * @author ruoyi
 * @date 2025-10-27
 */
@Service
public class DifySessionMappingServiceImpl implements IDifySessionMappingService {
    @Autowired
    private DifySessionMappingMapper difySessionMappingMapper;

    /**
     * 查询Dify 会话映射
     *
     * @param sessionId 主键
     * @return Dify 会话映射
     */
    @Override
    public DifySessionMapping selectDifySessionMappingBySessionId(String sessionId) {
        return difySessionMappingMapper.selectDifySessionMappingBySessionId(sessionId);
    }

    /**
     * 查询Dify 会话映射
     *
     * @param clientId agent chat client_id
     * @return Dify 会话映射
     */
    @Override
    public List<DifySessionMapping> selectDifySessionMappingByClientId(String clientId) {
        return difySessionMappingMapper.selectDifySessionMappingByClientId(clientId);
    }

    /**
     * 查询Dify 会话映射
     *
     * @param sessionId agent chat session_id
     * @param clientId  agent chat client_id
     * @return Dify 会话映射
     */
    @Override
    public DifySessionMapping selectDifySessionMappingBySessionIdAndClientId(String sessionId, String clientId) {
        return difySessionMappingMapper.selectDifySessionMappingBySessionIdAndClientId(sessionId, clientId);
    }

    /**
     * 查询Dify 会话映射列表
     *
     * @param difySessionMapping Dify 会话映射
     * @return Dify 会话映射
     */
    @Override
    public List<DifySessionMapping> selectDifySessionMappingList(DifySessionMapping difySessionMapping) {
        return difySessionMappingMapper.selectDifySessionMappingList(difySessionMapping);
    }

    /**
     * 新增Dify 会话映射
     *
     * @param difySessionMapping Dify 会话映射
     * @return 结果
     */
    @Override
    public int insertDifySessionMapping(DifySessionMapping difySessionMapping) {
        difySessionMapping.setCreateTime(DateUtils.getNowDate());
        return difySessionMappingMapper.insertDifySessionMapping(difySessionMapping);
    }

    /**
     * 修改Dify 会话映射
     *
     * @param difySessionMapping Dify 会话映射
     * @return 结果
     */
    @Override
    public int updateDifySessionMapping(DifySessionMapping difySessionMapping) {
        difySessionMapping.setUpdateTime(DateUtils.getNowDate());
        return difySessionMappingMapper.updateDifySessionMapping(difySessionMapping);
    }

    /**
     * 批量删除Dify 会话映射
     *
     * @param sessionIds 需要删除的Dify 会话映射主键
     * @return 结果
     */
    @Override
    public int deleteDifySessionMappingBySessionIds(String[] sessionIds) {
        return difySessionMappingMapper.deleteDifySessionMappingBySessionIds(sessionIds);
    }

    /**
     * 删除Dify 会话映射信息
     *
     * @param sessionId 主键
     * @return 结果
     */
    @Override
    public int deleteDifySessionMappingBySessionId(String sessionId) {
        return difySessionMappingMapper.deleteDifySessionMappingBySessionId(sessionId);
    }

    /**
     * 删除Dify 会话映射信息
     *
     * @param clientId agent chat client_id
     * @return 结果
     */
    @Override
    public int deleteDifySessionMappingByClientIdAndEmptyConversationId(String clientId) {
        return difySessionMappingMapper.deleteDifySessionMappingByClientIdAndEmptyConversationId(clientId);
    }
}