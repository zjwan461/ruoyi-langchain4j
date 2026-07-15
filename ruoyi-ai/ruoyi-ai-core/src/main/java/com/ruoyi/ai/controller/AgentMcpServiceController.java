package com.ruoyi.ai.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.domain.AgentMcpService;
import com.ruoyi.ai.domain.AgentMcpTool;
import com.ruoyi.ai.service.IAgentMcpServiceService;
import com.ruoyi.ai.service.LangChain4jService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.poi.ExcelUtil;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.internal.JsonSchemaElementJsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * mcpController
 *
 * @author Jerry
 * @date 2026-07-15
 */
@RestController
@RequestMapping("/ai/mcp")
public class AgentMcpServiceController extends BaseController {
    @Autowired
    private IAgentMcpServiceService agentMcpServiceService;
    @Resource
    private LangChain4jService langChain4jService;
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 查询mcp列表
     */
    @PreAuthorize("@ss.hasPermi('ai:mcp:list')")
    @GetMapping("/list")
    public TableDataInfo list(AgentMcpService agentMcpService) {
        startPage();
        List<AgentMcpService> list = agentMcpServiceService.selectAgentMcpServiceList(agentMcpService);
        return getDataTable(list);
    }

    /**
     * 导出mcp列表
     */
    @PreAuthorize("@ss.hasPermi('ai:mcp:export')")
    @Log(title = "mcp", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AgentMcpService agentMcpService) {
        List<AgentMcpService> list = agentMcpServiceService.selectAgentMcpServiceList(agentMcpService);
        ExcelUtil<AgentMcpService> util = new ExcelUtil<AgentMcpService>(AgentMcpService.class);
        util.exportExcel(response, list, "mcp数据");
    }

    /**
     * 获取mcp详细信息
     */
    @PreAuthorize("@ss.hasPermi('ai:mcp:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(agentMcpServiceService.selectAgentMcpServiceById(id));
    }

    /**
     * 新增mcp
     */
    @PreAuthorize("@ss.hasPermi('ai:mcp:add')")
    @Log(title = "mcp", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AgentMcpService agentMcpService) {
        return toAjax(agentMcpServiceService.insertAgentMcpService(agentMcpService));
    }

    /**
     * 修改mcp
     */
    @PreAuthorize("@ss.hasPermi('ai:mcp:edit')")
    @Log(title = "mcp", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AgentMcpService agentMcpService) {
        return toAjax(agentMcpServiceService.updateAgentMcpService(agentMcpService));
    }

    /**
     * 删除mcp
     */
    @PreAuthorize("@ss.hasPermi('ai:mcp:remove')")
    @Log(title = "mcp", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(agentMcpServiceService.deleteAgentMcpServiceByIds(ids));
    }

    @PreAuthorize("@ss.hasAnyPermi('ai:mcp:add')")
    @Log(title = "list-mcp-tool", businessType = BusinessType.OTHER)
    @GetMapping("/pull-mcp-tool")
    public AjaxResult pullMcpTool(String url, String transport, String authToken) {
        List<ToolSpecification> toolSpecifications = langChain4jService.listMcpTool(url, transport, authToken);
        List<AgentMcpTool> mcpTools = new ArrayList<>();
        for (ToolSpecification toolSpecification : toolSpecifications) {
            AgentMcpTool mcpTool = new AgentMcpTool();
            mcpTool.setMcpToolName(toolSpecification.name());
            Map<String, Object> paramMap = JsonSchemaElementJsonUtils.toMap(toolSpecification.parameters());
            String json = null;
            try {
                json = objectMapper.writeValueAsString(paramMap);
            } catch (JsonProcessingException e) {
                logger.error(e.getMessage(), e);
                throw new ServiceException("MCP参数提取失败");
            }
            mcpTool.setMcpToolParameters(json);
            mcpTool.setMcpToolDescription(toolSpecification.description());
            mcpTools.add(mcpTool);
        }
        return success(mcpTools);
    }

    @PreAuthorize("@ss.hasPermi('ai:mcp:list')")
    @GetMapping("/list-mcp-tool/{mcpServiceId}")
    public AjaxResult listMcpTool(@PathVariable Long mcpServiceId) {
        List<AgentMcpTool> list = agentMcpServiceService.selectAgentMcpToolList(mcpServiceId);
        return success(list);
    }
}
