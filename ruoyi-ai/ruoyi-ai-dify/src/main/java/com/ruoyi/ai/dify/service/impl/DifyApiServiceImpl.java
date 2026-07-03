package com.ruoyi.ai.dify.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.ai.dify.mapper.DifyApiMapper;
import com.ruoyi.ai.dify.domain.DifyApi;
import com.ruoyi.ai.dify.service.IDifyApiService;

/**
 * Dify APIService业务层处理
 * 
 * @author ruoyi
 * @date 2025-10-27
 */
@Service
public class DifyApiServiceImpl implements IDifyApiService 
{
    @Autowired
    private DifyApiMapper difyApiMapper;

    /**
     * 查询Dify API
     * 
     * @param id Dify API主键
     * @return Dify API
     */
    @Override
    public DifyApi selectDifyApiById(Long id)
    {
        return difyApiMapper.selectDifyApiById(id);
    }

    /**
     * 查询Dify API列表
     * 
     * @param difyApi Dify API
     * @return Dify API
     */
    @Override
    public List<DifyApi> selectDifyApiList(DifyApi difyApi)
    {
        return difyApiMapper.selectDifyApiList(difyApi);
    }

    /**
     * 新增Dify API
     * 
     * @param difyApi Dify API
     * @return 结果
     */
    @Override
    public int insertDifyApi(DifyApi difyApi)
    {
        difyApi.setCreateTime(DateUtils.getNowDate());
        return difyApiMapper.insertDifyApi(difyApi);
    }

    /**
     * 修改Dify API
     * 
     * @param difyApi Dify API
     * @return 结果
     */
    @Override
    public int updateDifyApi(DifyApi difyApi)
    {
        difyApi.setUpdateTime(DateUtils.getNowDate());
        return difyApiMapper.updateDifyApi(difyApi);
    }

    /**
     * 批量删除Dify API
     * 
     * @param ids 需要删除的Dify API主键
     * @return 结果
     */
    @Override
    public int deleteDifyApiByIds(Long[] ids)
    {
        return difyApiMapper.deleteDifyApiByIds(ids);
    }

    /**
     * 删除Dify API信息
     * 
     * @param id Dify API主键
     * @return 结果
     */
    @Override
    public int deleteDifyApiById(Long id)
    {
        return difyApiMapper.deleteDifyApiById(id);
    }
}
