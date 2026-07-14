package com.ruoyi.ai.controller;

import com.ruoyi.ai.domain.AgentSkill;
import com.ruoyi.ai.service.IAgentSkillService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.DictUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;

/**
 * agent_skillController
 * 
 * @author Jerry
 * @date 2026-07-13
 */
@RestController
@RequestMapping("/ai/skill")
public class AgentSkillController extends BaseController
{
    @Autowired
    private IAgentSkillService agentSkillService;

    /**
     * 查询agent_skill列表
     */
    @PreAuthorize("@ss.hasPermi('ai:skill:list')")
    @GetMapping("/list")
    public TableDataInfo list(AgentSkill agentSkill)
    {
        startPage();
        List<AgentSkill> list = agentSkillService.selectAgentSkillList(agentSkill);
        return getDataTable(list);
    }

    /**
     * 导出agent_skill列表
     */
    @PreAuthorize("@ss.hasPermi('ai:skill:export')")
    @Log(title = "agent_skill", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AgentSkill agentSkill)
    {
        List<AgentSkill> list = agentSkillService.selectAgentSkillList(agentSkill);
        ExcelUtil<AgentSkill> util = new ExcelUtil<AgentSkill>(AgentSkill.class);
        util.exportExcel(response, list, "agent_skill数据");
    }

    /**
     * 获取agent_skill详细信息
     */
    @PreAuthorize("@ss.hasPermi('ai:skill:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(agentSkillService.selectAgentSkillById(id));
    }

    /**
     * 新增agent_skill
     */
    @PreAuthorize("@ss.hasPermi('ai:skill:add')")
    @Log(title = "agent_skill", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody @Valid AgentSkill agentSkill)
    {
        agentSkill.setSkillType(DictUtils.getDictValue("ai_skill_type","创建"));
        return toAjax(agentSkillService.insertAgentSkill(agentSkill));
    }

    /**
     * 修改agent_skill
     */
    @PreAuthorize("@ss.hasPermi('ai:skill:edit')")
    @Log(title = "agent_skill", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AgentSkill agentSkill)
    {
        return toAjax(agentSkillService.updateAgentSkill(agentSkill));
    }

    /**
     * 删除agent_skill
     */
    @PreAuthorize("@ss.hasPermi('ai:skill:remove')")
    @Log(title = "agent_skill", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(agentSkillService.deleteAgentSkillByIds(ids));
    }
}
