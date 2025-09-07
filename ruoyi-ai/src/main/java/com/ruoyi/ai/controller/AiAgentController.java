package com.ruoyi.ai.controller;

import com.ruoyi.ai.domain.AiAgent;
import com.ruoyi.ai.domain.KnowledgeBase;
import com.ruoyi.ai.domain.Model;
import com.ruoyi.ai.service.IAiAgentService;
import com.ruoyi.ai.service.IKnowledgeBaseService;
import com.ruoyi.ai.service.IModelService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.DictUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.utils.uuid.IdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * AI智能体Controller
 *
 * @author jerry
 * @date 2025-09-02
 */
@RestController
@RequestMapping("/ai/agent")
public class AiAgentController extends BaseController {

  @Autowired
  private IAiAgentService aiAgentService;

  @Autowired
  private IKnowledgeBaseService knowledgeBaseService;

  @Autowired
  private IModelService modelService;

  @Autowired
  private MessageSource messageSource;

  @Autowired
  private Environment environment;

  /**
   * 查询AI智能体列表
   */
  @PreAuthorize("@ss.hasPermi('ai:agent:list')")
  @GetMapping("/list")
  public TableDataInfo list(AiAgent aiAgent) {
    startPage();
    Model model = new Model();
    String dictLabel = DictUtils.getDictValue("ai_model_type", "LLM");
    model.setType(Integer.valueOf(dictLabel));
    List<Model> models = modelService.selectModelList(model);

    List<KnowledgeBase> knowledgeBases = knowledgeBaseService.selectKnowledgeBaseList(null);

    List<AiAgent> list = aiAgentService.selectAiAgentList(aiAgent);

    list.forEach(agent -> {
      Long modelId = agent.getModelId();
      models.stream().filter(x -> x.getId().equals(modelId)).findFirst()
          .ifPresent(matched -> agent.setModelName(matched.getName()));

      Long kbId = agent.getKbId();
      knowledgeBases.stream().filter(x -> x.getId().equals(kbId)).findFirst()
          .ifPresent(matched -> agent.setKbName(matched.getName()));
    });
    return getDataTable(list);
  }

  /**
   * 导出AI智能体列表
   */
  @PreAuthorize("@ss.hasPermi('ai:agent:export')")
  @Log(title = "AI智能体", businessType = BusinessType.EXPORT)
  @PostMapping("/export")
  public void export(HttpServletResponse response, AiAgent aiAgent) {
    List<AiAgent> list = aiAgentService.selectAiAgentList(aiAgent);
    ExcelUtil<AiAgent> util = new ExcelUtil<AiAgent>(AiAgent.class);
    util.exportExcel(response, list, "AI智能体数据");
  }

  /**
   * 获取AI智能体详细信息
   */
  @PreAuthorize("@ss.hasPermi('ai:agent:query')")
  @GetMapping(value = "/{id}")
  public AjaxResult getInfo(@PathVariable("id") Long id) {
    return success(aiAgentService.selectAiAgentById(id));
  }

  /**
   * 新增AI智能体
   */
  @PreAuthorize("@ss.hasPermi('ai:agent:add')")
  @Log(title = "AI智能体", businessType = BusinessType.INSERT)
  @PostMapping
  public AjaxResult add(@RequestBody AiAgent aiAgent) {
    constructInfo(aiAgent);
    return toAjax(aiAgentService.insertAiAgent(aiAgent));
  }

  private void constructInfo(AiAgent aiAgent) {
    Model model = modelService.selectModelById(aiAgent.getModelId());
    aiAgent.setModelName(model.getName());
    if (aiAgent.getKbId() != null) {
      KnowledgeBase knowledgeBase = knowledgeBaseService.selectKnowledgeBaseById(
          aiAgent.getKbId());
      aiAgent.setKbName(knowledgeBase.getName());
    }
    if (aiAgent.getVisitUrl() == null || "".equals(aiAgent.getVisitUrl())) {
      String seed = IdUtils.fastSimpleUUID();
      String visitUrl = "/ai-chat/" + seed;
      aiAgent.setVisitUrl(visitUrl);
    }
  }

  /**
   * 修改AI智能体
   */
  @PreAuthorize("@ss.hasPermi('ai:agent:edit')")
  @Log(title = "AI智能体", businessType = BusinessType.UPDATE)
  @PutMapping
  public AjaxResult edit(@RequestBody AiAgent aiAgent) {
    constructInfo(aiAgent);
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
