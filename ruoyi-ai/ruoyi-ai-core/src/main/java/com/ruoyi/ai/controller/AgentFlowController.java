package com.ruoyi.ai.controller;

import com.ruoyi.ai.controller.model.FlowAgent;
import com.ruoyi.ai.domain.AIAgent;
import com.ruoyi.ai.domain.AgentFlow;
import com.ruoyi.ai.service.IAIAgentService;
import com.ruoyi.ai.service.IAgentFlowService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.DictUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.utils.uuid.IdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * agent_flowController
 *
 * @author Jerry
 * @date 2026-07-17
 */
@RestController
@RequestMapping("/ai/agent_flow")
public class AgentFlowController extends BaseController {
    @Autowired
    private IAgentFlowService agentFlowService;

    @Resource
    private IAIAgentService agentService;

    /**
     * 查询agent_flow列表
     */
    @PreAuthorize("@ss.hasPermi('ai:agent_flow:list')")
    @GetMapping("/list")
    public TableDataInfo list(AgentFlow agentFlow) {
        startPage();
        List<AgentFlow> list = agentFlowService.selectAgentFlowList(agentFlow);
        return getDataTable(list);
    }

    /**
     * 导出agent_flow列表
     */
    @PreAuthorize("@ss.hasPermi('ai:agent_flow:export')")
    @Log(title = "agent_flow", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AgentFlow agentFlow) {
        List<AgentFlow> list = agentFlowService.selectAgentFlowList(agentFlow);
        ExcelUtil<AgentFlow> util = new ExcelUtil<AgentFlow>(AgentFlow.class);
        util.exportExcel(response, list, "agent_flow数据");
    }

    /**
     * 获取agent_flow详细信息
     */
    @PreAuthorize("@ss.hasPermi('ai:agent_flow:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(agentFlowService.selectAgentFlowById(id));
    }

    /**
     * 新增agent_flow
     */
    @PreAuthorize("@ss.hasPermi('ai:agent_flow:add')")
    @Log(title = "agent_flow", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AgentFlow agentFlow) {
        constructInfo(agentFlow);
        return success(agentFlowService.insertAgentFlow(agentFlow));
    }

    public void constructInfo(AgentFlow agentFlow) {
        if (agentFlow.getVisitUrl() == null || "".equals(agentFlow.getVisitUrl())) {
            String seed = IdUtils.fastSimpleUUID();
            String visitUrl = "/ai-chat-flow/" + seed;
            agentFlow.setVisitUrl(visitUrl);
        }
    }

    /**
     * 修改agent_flow
     */
    @PreAuthorize("@ss.hasPermi('ai:agent_flow:edit')")
    @Log(title = "agent_flow", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AgentFlow agentFlow) {
        constructInfo(agentFlow);
        return toAjax(agentFlowService.updateAgentFlow(agentFlow));
    }

    /**
     * 删除agent_flow
     */
    @PreAuthorize("@ss.hasPermi('ai:agent_flow:remove')")
    @Log(title = "agent_flow", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(agentFlowService.deleteAgentFlowByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('ai:agent_flow:add')")
    @GetMapping("/list-agent")
    public AjaxResult listAgent() {
        AIAgent aiAgent = new AIAgent();
        aiAgent.setAgentType(DictUtils.getDictValue("ai_agent_type", "origin"));
        List<AIAgent> aiAgents = agentService.selectAiAgentList(aiAgent);
        List<FlowAgent> list = aiAgents.stream().map(x -> FlowAgent.builder()
                .id(x.getId())
                .name(x.getName())
                .description(x.getDescription())
                .showReasoning(Boolean.TRUE).build()).toList();
        return success(list);
    }
}
