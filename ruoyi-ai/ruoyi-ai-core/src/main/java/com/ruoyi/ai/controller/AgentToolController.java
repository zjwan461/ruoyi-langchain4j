package com.ruoyi.ai.controller;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.domain.AgentTool;
import com.ruoyi.ai.service.IAgentToolService;
import com.ruoyi.ai.util.SimplePythonFuncParser;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DictUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * agent_toolController
 *
 * @author Jerry
 * @date 2026-07-07
 */
@RestController
@RequestMapping("/ai/agent_tool")
public class AgentToolController extends BaseController {
    @Autowired
    private IAgentToolService agentToolService;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 查询agent_tool列表
     */
    @PreAuthorize("@ss.hasPermi('ai:agent_tool:list')")
    @GetMapping("/list")
    public TableDataInfo list(AgentTool agentTool) {
        startPage();
        List<AgentTool> list = agentToolService.selectAgentToolList(agentTool);
        return getDataTable(list);
    }

    /**
     * 导出agent_tool列表
     */
    @PreAuthorize("@ss.hasPermi('ai:agent_tool:export')")
    @Log(title = "agent_tool", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AgentTool agentTool) {
        List<AgentTool> list = agentToolService.selectAgentToolList(agentTool);
        ExcelUtil<AgentTool> util = new ExcelUtil<AgentTool>(AgentTool.class);
        util.exportExcel(response, list, "agent_tool数据");
    }

    /**
     * 获取agent_tool详细信息
     */
    @PreAuthorize("@ss.hasPermi('ai:agent_tool:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(agentToolService.selectAgentToolById(id));
    }

    /**
     * 新增agent_tool
     */
    @PreAuthorize("@ss.hasPermi('ai:agent_tool:add')")
    @Log(title = "agent_tool", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AgentTool agentTool) {
        this.checkParams(agentTool);
        agentTool.setCreateBy(getUsername());
        return toAjax(agentToolService.insertAgentTool(agentTool));
    }

    private void checkParams(AgentTool agentTool) {
        String toolType = agentTool.getToolType();
        String sandbox = DictUtils.getDictValue("ai_tool_type", "sandbox");
        String builtin = DictUtils.getDictValue("ai_tool_type", "builtin");
        if (toolType.equals(builtin)) {
            if (StringUtils.isBlank(agentTool.getClazz())) {
                throw new ServiceException("builtin tool 类名必填");
            }
            try {
                Class.forName(agentTool.getClazz());
            } catch (ClassNotFoundException e) {
                throw new ServiceException("找不到class:" + agentTool.getClazz() + "的实现");
            }
        } else if (toolType.equals(sandbox)) {
            if (StringUtils.isBlank(agentTool.getCode())) {
                throw new ServiceException("sandbox tool 代码必填");
            }
            SimplePythonFuncParser.FuncInfo funcInfo = SimplePythonFuncParser.parsePythonCode(agentTool.getCode());
            List<SimplePythonFuncParser.Param> paramList = funcInfo.getParamList();
            try {
                if (CollUtil.isNotEmpty(paramList)) {
                    agentTool.setParameters(objectMapper.writeValueAsString(paramList));
                }
            } catch (JsonProcessingException e) {
                logger.error(e.getMessage(), e);
                throw new ServiceException("处理代码参数失败");
            }
        }
    }

    /**
     * 修改agent_tool
     */
    @PreAuthorize("@ss.hasPermi('ai:agent_tool:edit')")
    @Log(title = "agent_tool", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AgentTool agentTool) {
        this.checkParams(agentTool);
        agentTool.setUpdateBy(getUsername());
        return toAjax(agentToolService.updateAgentTool(agentTool));
    }

    /**
     * 删除agent_tool
     */
    @PreAuthorize("@ss.hasPermi('ai:agent_tool:remove')")
    @Log(title = "agent_tool", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(agentToolService.deleteAgentToolByIds(ids));
    }
}
