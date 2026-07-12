package com.ruoyi.ai.controller;

import com.ruoyi.ai.dify.domain.DifyApi;
import com.ruoyi.ai.dify.service.IDifyApiService;
import com.ruoyi.ai.domain.AIAgent;
import com.ruoyi.ai.domain.KnowledgeBase;
import com.ruoyi.ai.domain.Model;
import com.ruoyi.ai.service.IAIAgentService;
import com.ruoyi.ai.service.IKnowledgeBaseService;
import com.ruoyi.ai.service.IModelService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.DictUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.utils.uuid.IdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI智能体Controller
 *
 * @author jerry
 * @date 2025-09-02
 */
@RestController
@RequestMapping("/ai/agent")
public class AIAgentController extends BaseController {

    @Autowired
    private IAIAgentService aiAgentService;

    @Autowired
    private IKnowledgeBaseService knowledgeBaseService;

    @Autowired
    private IModelService modelService;

    @Autowired
    private IDifyApiService difyApiService;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private Environment environment;

    /**
     * 查询AI智能体列表
     */
    @PreAuthorize("@ss.hasPermi('ai:agent:list')")
    @GetMapping("/list")
    public TableDataInfo list(AIAgent aiAgent) {
        startPage();
        Model model = new Model();
        String dictLabel = DictUtils.getDictValue("ai_model_type", "LLM");
        model.setType(Integer.valueOf(dictLabel));
        List<Model> models = modelService.selectModelList(model);

        List<KnowledgeBase> knowledgeBases = knowledgeBaseService.selectKnowledgeBaseList(null);

        List<AIAgent> list = aiAgentService.selectAiAgentList(aiAgent);

        list.forEach(agent -> {
            Long modelId = agent.getModelId();
            models.stream().filter(x -> x.getId().equals(modelId)).findFirst()
                    .ifPresent(matched -> agent.setModelName(matched.getName()));

            String kbIds = agent.getKbIds();
            if (StringUtils.isNotBlank(kbIds)) {
                List<String> matchedKbNames = knowledgeBases.stream().filter(x -> kbIds.contains(String.valueOf(x.getId())))
                        .map(KnowledgeBase::getName).collect(Collectors.toList());
                agent.setKbNames(String.join(",", matchedKbNames));
            }
        });
        return getDataTable(list);
    }

    /**
     * 导出AI智能体列表
     */
    @PreAuthorize("@ss.hasPermi('ai:agent:export')")
    @Log(title = "AI智能体", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AIAgent aiAgent) {
        List<AIAgent> list = aiAgentService.selectAiAgentList(aiAgent);
        ExcelUtil<AIAgent> util = new ExcelUtil<AIAgent>(AIAgent.class);
        util.exportExcel(response, list, "AI智能体数据");
    }

    /**
     * 获取AI智能体详细信息
     */
    @PreAuthorize("@ss.hasPermi('ai:agent:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(aiAgentService.selectAiAgentWithToolById(id));
    }

    /**
     * 新增AI智能体
     */
    @PreAuthorize("@ss.hasPermi('ai:agent:add')")
    @Log(title = "AI智能体", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AIAgent aiAgent) {
        constructInfo(aiAgent);
        aiAgent.setCreateBy(getUsername());
        return toAjax(aiAgentService.insertAiAgent(aiAgent));
    }

    private void constructInfo(AIAgent aiAgent) {
        String difyType = DictUtils.getDictValue("ai_agent_type", "dify");
        String originType = DictUtils.getDictValue("ai_agent_type", "origin");
        if (StringUtils.equals(difyType, aiAgent.getAgentType())) {
            DifyApi difyApi = difyApiService.selectDifyApiById(aiAgent.getDifyApiId());
            aiAgent.setDifyApiName(difyApi.getName());
            if (aiAgent.getVisitUrl() == null || "".equals(aiAgent.getVisitUrl())) {
                String seed = IdUtils.fastSimpleUUID();
                String visitUrl = "/dify-chat/" + seed;
                aiAgent.setVisitUrl(visitUrl);
            }
        } else if (StringUtils.equals(originType, aiAgent.getAgentType())) {
            Model model = modelService.selectModelById(aiAgent.getModelId());
            aiAgent.setModelName(model.getName());
            if (StringUtils.isNotBlank(aiAgent.getKbIds())) {
                List<Long> ids = Arrays.stream(aiAgent.getKbIds().split(",")).map(Long::valueOf)
                        .collect(Collectors.toList());
                List<KnowledgeBase> kbs = knowledgeBaseService.selectKnowledgeBaseByIds(ids);
                List<String> kbNames = kbs.stream().map(KnowledgeBase::getName).collect(Collectors.toList());
                aiAgent.setKbNames(String.join(",", kbNames));
            }
            if (aiAgent.getVisitUrl() == null || "".equals(aiAgent.getVisitUrl())) {
                String seed = IdUtils.fastSimpleUUID();
                String visitUrl = "/ai-chat/" + seed;
                aiAgent.setVisitUrl(visitUrl);
            }
        }
    }

    /**
     * 修改AI智能体
     */
    @PreAuthorize("@ss.hasPermi('ai:agent:edit')")
    @Log(title = "AI智能体", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AIAgent aiAgent) {
        constructInfo(aiAgent);
        aiAgent.setUpdateBy(getUsername());
        return toAjax(aiAgentService.updateAiAgent(aiAgent));
    }

    /**
     * 删除AI智能体
     */
    @PreAuthorize("@ss.hasPermi('ai:agent:remove')")
    @Log(title = "AI智能体", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(aiAgentService.deleteAiAgentByIds(ids));
    }


}
