package com.ruoyi.ai.service;

import java.util.List;
import com.ruoyi.ai.domain.Model;

/**
 * 模型管理Service接口
 * 
 * @author jerry
 * @date 2025-08-25
 */
public interface IModelService 
{
    /**
     * 查询模型管理
     * 
     * @param id 模型管理主键
     * @return 模型管理
     */
    public Model selectModelById(Long id);

    /**
     * 查询模型管理列表
     * 
     * @param model 模型管理
     * @return 模型管理集合
     */
    public List<Model> selectModelList(Model model);

    /**
     * 新增模型管理
     * 
     * @param model 模型管理
     * @return 结果
     */
    public int insertModel(Model model);

    /**
     * 修改模型管理
     * 
     * @param model 模型管理
     * @return 结果
     */
    public int updateModel(Model model);

    /**
     * 批量删除模型管理
     * 
     * @param ids 需要删除的模型管理主键集合
     * @return 结果
     */
    public int deleteModelByIds(Long[] ids);

    /**
     * 删除模型管理信息
     * 
     * @param id 模型管理主键
     * @return 结果
     */
    public int deleteModelById(Long id);
}
