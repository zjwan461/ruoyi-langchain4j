package com.ruoyi.ai.controller;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.config.AiConfig;
import com.ruoyi.ai.domain.Model;
import com.ruoyi.ai.enums.ModelProvider;
import com.ruoyi.ai.enums.ModelType;
import com.ruoyi.ai.service.IModelService;
import com.ruoyi.ai.util.Constants;
import com.ruoyi.ai.util.ModelScopeUtil;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.framework.websocket.SocketMessage;
import com.ruoyi.framework.websocket.WebSocketUsers;
import com.ruoyi.system.service.ISysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Session;
import java.util.List;

/**
 * 模型管理Controller
 *
 * @author jerry
 * @date 2025-08-25
 */
@RestController
@RequestMapping("/ai/model")
public class ModelController extends BaseController {
    @Autowired
    private IModelService modelService;

    @Autowired
    private ISysConfigService sysConfigService;

    @Resource
    private ModelScopeUtil modelScopeUtil;

    @Resource
    private HttpServletRequest request;

    @Resource
    private TokenService tokenService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private AiConfig aiConfig;


    /**
     * 查询模型管理列表
     */
    @PreAuthorize("@ss.hasPermi('ai:model:list')")
    @GetMapping("/list")
    public TableDataInfo list(Model model) {
        startPage();
        List<Model> list = modelService.selectModelList(model);
        return getDataTable(list);
    }

    /**
     * 导出模型管理列表
     */
    @PreAuthorize("@ss.hasPermi('ai:model:export')")
    @Log(title = "模型管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Model model) {
        List<Model> list = modelService.selectModelList(model);
        ExcelUtil<Model> util = new ExcelUtil<Model>(Model.class);
        util.exportExcel(response, list, "模型管理数据");
    }

    /**
     * 获取模型管理详细信息
     */
    @PreAuthorize("@ss.hasPermi('ai:model:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(modelService.selectModelById(id));
    }

    /**
     * 新增模型管理
     */
    @PreAuthorize("@ss.hasPermi('ai:model:add')")
    @Log(title = "模型管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Model model) {
        model.setCreateBy(getUsername());
        return toAjax(modelService.insertModel(model));
    }

    /**
     * 修改模型管理
     */
    @PreAuthorize("@ss.hasPermi('ai:model:edit')")
    @Log(title = "模型管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Model model) {
        model.setUpdateBy(getUsername());
        return toAjax(modelService.updateModel(model));
    }

    /**
     * 删除模型管理
     */
    @PreAuthorize("@ss.hasPermi('ai:model:remove')")
    @Log(title = "模型管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(modelService.deleteModelByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('ai:model:list')")
    @GetMapping("/checkEmbeddingModel")
    public AjaxResult checkEmbeddingModel() {
        Model req = new Model();
        req.setType(ModelType.EMBEDDING.getValue());
        List<Model> res = modelService.selectModelList(req);
        return success(CollUtil.isNotEmpty(res) ? "success" : "fail");
    }

    @PreAuthorize("@ss.hasPermi('ai:model:list')")
    @GetMapping("/download-default-embedding")
    @Log(title = "模型管理", businessType = BusinessType.INSERT)
    public AjaxResult downloadDefaultEmbedding() {
        Model req = new Model();
        req.setProvider(ModelProvider.LOCAL.getValue());
        if (!modelService.selectModelList(req).isEmpty()) {
            throw new ServiceException("已经下载过本地embedding模型");
        }
        String saveDir = sysConfigService.selectConfigByKey("ai.model.saveDir");
        String token = tokenService.getToken(request);
        String repoId = aiConfig.getModelScope().getEmbeddingModelRepoId();
        saveDir = modelScopeUtil.downloadMultiThread(repoId, saveDir, Constants.ALL_ALLOW_PATTERN, modelDir -> {
            try {
                Model model = new Model();
                model.setName(repoId);
                model.setType(ModelType.EMBEDDING.getValue());
                model.setProvider(ModelProvider.LOCAL.getValue());
                model.setBaseUrl("#");
                model.setSaveDir(modelDir);
                model.setCreateBy("System");
                modelService.insertModel(model);

                sysConfigService.updateConfigByKeyValue("ai.model.embedding", String.valueOf(model.getId()));
                Session session = WebSocketUsers.getSessionByToken(token);
                if (session != null) {
                    WebSocketUsers.sendMessageToUserByText(session, objectMapper.writeValueAsString(SocketMessage.success("embedding模型已下载,保存位置：" + modelDir)));
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });

        return success(saveDir);
    }

    @PreAuthorize("@ss.hasPermi('ai:model:list')")
    @GetMapping("/local-embedding-model")
    public AjaxResult getLocalEmbeddingModel() {
        Model req = new Model();
        req.setProvider(ModelProvider.LOCAL.getValue());
        List<Model> list = modelService.selectModelList(req);
        if (CollUtil.isNotEmpty(list)) {
            return success(list.get(0));
        }
        return success();
    }

    @PreAuthorize("@ss.hasPermi('ai:model:list')")
    @PostMapping("/set-default-embedding/{id}")
    public AjaxResult setDefaultEmbeddingModel(@PathVariable("id") Long embeddingId) throws JsonProcessingException {
        String old = sysConfigService.selectConfigByKey("ai.model.embedding");
        sysConfigService.updateConfigByKeyValue("ai.model.embedding", String.valueOf(embeddingId));
        if (old != null && !Long.valueOf(old).equals(embeddingId)) {
            String token = tokenService.getToken(request);
            Session session = WebSocketUsers.getSessionByToken(token);
            WebSocketUsers.sendMessageToUserByText(session, objectMapper.writeValueAsString(SocketMessage.warn("系统检测到默认embedding模型已改变，向量内容需要重新向量化，否则会影响查询结果")));
        }
        return success();
    }
}
