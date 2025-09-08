package com.ruoyi.ai.service.impl;

import com.ruoyi.ai.domain.Model;
import com.ruoyi.ai.enums.ModelProvider;
import com.ruoyi.ai.enums.ModelType;
import com.ruoyi.ai.mapper.ModelMapper;
import com.ruoyi.ai.service.IModelService;
import com.ruoyi.ai.service.LangChain4jService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 模型管理Service业务层处理
 *
 * @author jerry
 * @date 2025-08-25
 */
@Service
public class ModelServiceImpl implements IModelService {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private LangChain4jService langChain4jService;


    /**
     * 查询模型管理
     *
     * @param id 模型管理主键
     * @return 模型管理
     */
    @Override
    public Model selectModelById(Long id) {
        return modelMapper.selectModelById(id);
    }

    /**
     * 查询模型管理列表
     *
     * @param model 模型管理
     * @return 模型管理
     */
    @Override
    public List<Model> selectModelList(Model model) {
        return modelMapper.selectModelList(model);
    }

    /**
     * 新增模型管理
     *
     * @param model 模型管理
     * @return 结果
     */
    @Override
    public int insertModel(Model model) {
        checkModelConfig(model);
        model.setCreateTime(DateUtils.getNowDate());
        return modelMapper.insertModel(model);
    }

    private void checkModelConfig(Model model) {
        ModelProvider modelProvider = ModelProvider.fromValue(model.getProvider());
        if (modelProvider == ModelProvider.LOCAL) {
            // if (!langChain4jService.checkLocalEmbeddingModel(model.getSaveDir())) {
            //     throw new ServiceException("模型验证失败");
            // }
            return;
        }
        if (!langChain4jService.checkModelConfig(model.getBaseUrl(), model.getApiKey(), model.getName(),
                modelProvider, ModelType.fromValue(model.getType()))) {
            throw new ServiceException("模型验证失败");
        }
    }

    /**
     * 修改模型管理
     *
     * @param model 模型管理
     * @return 结果
     */
    @Override
    public int updateModel(Model model) {
        checkModelConfig(model);
        model.setUpdateTime(DateUtils.getNowDate());
        return modelMapper.updateModel(model);
    }

    /**
     * 批量删除模型管理
     *
     * @param ids 需要删除的模型管理主键
     * @return 结果
     */
    @Override
    public int deleteModelByIds(Long[] ids) {
        return modelMapper.deleteModelByIds(ids);
    }

    /**
     * 删除模型管理信息
     *
     * @param id 模型管理主键
     * @return 结果
     */
    @Override
    public int deleteModelById(Long id) {
        return modelMapper.deleteModelById(id);
    }
}
