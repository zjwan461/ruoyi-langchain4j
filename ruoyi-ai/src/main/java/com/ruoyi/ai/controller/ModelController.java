package com.ruoyi.ai.controller;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.domain.Model;
import com.ruoyi.ai.enums.ModelProvider;
import com.ruoyi.ai.enums.ModelType;
import com.ruoyi.ai.service.IModelService;
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
        return toAjax(modelService.insertModel(model));
    }

    /**
     * 修改模型管理
     */
    @PreAuthorize("@ss.hasPermi('ai:model:edit')")
    @Log(title = "模型管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Model model) {
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
        saveDir = modelScopeUtil.downloadMultiThread("zjwan461/shibing624_text2vec-base-chinese", saveDir, "[\\s\\S]*", modelDir -> {
            try {
                Model model = new Model();
                model.setName("shibing624_text2vec-base-chinese");
                model.setType(ModelType.EMBEDDING.getValue());
                model.setProvider(ModelProvider.LOCAL.getValue());
                model.setBaseUrl("#");
                model.setSaveDir(modelDir);
                model.setCreateBy("System");
                modelService.insertModel(model);

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
}
