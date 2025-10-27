package com.ruoyi.ai.dify.service;

import java.util.List;
import com.ruoyi.ai.dify.domain.DifyApi;

/**
 * Dify APIService接口
 * 
 * @author ruoyi
 * @date 2025-10-27
 */
public interface IDifyApiService 
{
    /**
     * 查询Dify API
     * 
     * @param id Dify API主键
     * @return Dify API
     */
    public DifyApi selectDifyApiById(Long id);

    /**
     * 查询Dify API列表
     * 
     * @param difyApi Dify API
     * @return Dify API集合
     */
    public List<DifyApi> selectDifyApiList(DifyApi difyApi);

    /**
     * 新增Dify API
     * 
     * @param difyApi Dify API
     * @return 结果
     */
    public int insertDifyApi(DifyApi difyApi);

    /**
     * 修改Dify API
     * 
     * @param difyApi Dify API
     * @return 结果
     */
    public int updateDifyApi(DifyApi difyApi);

    /**
     * 批量删除Dify API
     * 
     * @param ids 需要删除的Dify API主键集合
     * @return 结果
     */
    public int deleteDifyApiByIds(Long[] ids);

    /**
     * 删除Dify API信息
     * 
     * @param id Dify API主键
     * @return 结果
     */
    public int deleteDifyApiById(Long id);
}
