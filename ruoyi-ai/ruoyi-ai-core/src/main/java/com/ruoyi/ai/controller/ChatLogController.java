package com.ruoyi.ai.controller;

import com.ruoyi.ai.controller.dto.ChatLogDetail;
import com.ruoyi.ai.controller.dto.ChatLogQuery;
import com.ruoyi.ai.domain.AIAgent;
import com.ruoyi.ai.domain.AgentFlow;
import com.ruoyi.ai.domain.ChatFlowMessage;
import com.ruoyi.ai.domain.ChatMessage;
import com.ruoyi.ai.mapper.ChatFlowMessageMapper;
import com.ruoyi.ai.mapper.ChatMessageMapper;
import com.ruoyi.ai.service.IAIAgentService;
import com.ruoyi.ai.service.IAgentFlowService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话日志Controller
 * <p>
 * 提供"AI工具箱-对话日志"页面所需的接口：
 * <ul>
 *     <li>列表分页查询（按 traceId 聚合）</li>
 *     <li>查询某次对话的全部消息</li>
 *     <li>查询 Agent Flow 一次对话中参与的 Agent 列表</li>
 *     <li>下拉选项：Agent / Agent Flow</li>
 * </ul>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/ai/chatLog")
public class ChatLogController extends BaseController {

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private ChatFlowMessageMapper chatFlowMessageMapper;

    @Autowired
    private IAIAgentService aiAgentService;

    @Autowired
    private IAgentFlowService agentFlowService;

    /**
     * 查询对话日志分页（按 traceId 聚合）
     */
    @PreAuthorize("@ss.hasPermi('ai:chatLog:list')")
    @GetMapping("/list")
    public TableDataInfo list(ChatLogQuery query) {
        // 默认 type
        if (StringUtils.isEmpty(query.getType())) {
            query.setType("agent");
        }
        // 启动分页
        startPage();
        List<Map<String, Object>> list;
        if ("agentFlow".equalsIgnoreCase(query.getType())) {
            if (query.getAgentFlowId() == null) {
                return getDataTable(java.util.Collections.emptyList());
            }
            list = chatFlowMessageMapper.selectChatLogTraceList(
                    query.getAgentFlowId(), query.getSessionId(), query.getTraceId());
        } else {
            if (query.getAgentId() == null) {
                return getDataTable(java.util.Collections.emptyList());
            }
            list = chatMessageMapper.selectChatLogTraceList(
                    query.getAgentId(), query.getSessionId(), query.getTraceId());
        }
        return getDataTable(list);
    }

    /**
     * 查询某次对话的全部消息（含 invoke 数据）
     *
     * @param type    agent / agentFlow
     * @param traceId 追踪ID
     */
    @PreAuthorize("@ss.hasPermi('ai:chatLog:query')")
    @GetMapping("/detail")
    public AjaxResult detail(@RequestParam("type") String type,
                             @RequestParam("traceId") String traceId) {
        ChatLogDetail detail = new ChatLogDetail();
        detail.setType(type);
        detail.setTraceId(traceId);

        if (StringUtils.isEmpty(traceId)) {
            return success(detail);
        }

        if ("agentFlow".equalsIgnoreCase(type)) {
            List<ChatFlowMessage> messages = chatFlowMessageMapper.selectChatLogMessagesByTraceId(traceId);
            detail.setMessages(messages);
            List<Map<String, Object>> agents = chatFlowMessageMapper.selectChatLogAgentsByTraceId(traceId);
            detail.setParticipantAgents(agents);
            if (!messages.isEmpty()) {
                ChatFlowMessage first = messages.get(0);
                detail.setAgentFlowId(first.getAgentFlowId());
                detail.setSessionId(first.getSessionId());
            }
        } else {
            List<ChatMessage> messages = chatMessageMapper.selectChatLogMessagesByTraceId(traceId);
            detail.setMessages(messages);
            if (!messages.isEmpty()) {
                ChatMessage first = messages.get(0);
                detail.setAgentId(first.getAgentId());
                detail.setSessionId(first.getSessionId());
            }
        }
        return success(detail);
    }

    /**
     * 查询 Agent Flow 一次对话中参与调用的 Agent 列表（流程图）
     *
     * @param traceId 追踪ID
     */
    @PreAuthorize("@ss.hasPermi('ai:chatLog:query')")
    @GetMapping("/agentChain")
    public AjaxResult agentChain(@RequestParam("traceId") String traceId) {
        List<Map<String, Object>> list = chatFlowMessageMapper.selectChatLogAgentsByTraceId(traceId);
        return success(list);
    }

    /**
     * 获取 Agent 下拉选项
     */
    @PreAuthorize("@ss.hasPermi('ai:chatLog:list')")
    @GetMapping("/agentOptions")
    public AjaxResult agentOptions() {
        List<AIAgent> all = aiAgentService.selectAiAgentList(new AIAgent());
        // 排除 dify 类型、仅保留 origin
        List<Map<String, Object>> data = new java.util.ArrayList<>();
        for (AIAgent a : all) {
            if (a.getAgentType() == null || !"dify".equals(a.getAgentType())) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", a.getId());
                m.put("name", a.getName());
                m.put("description", a.getDescription());
                data.add(m);
            }
        }
        return success(data);
    }

    /**
     * 获取 Agent Flow 下拉选项
     */
    @PreAuthorize("@ss.hasPermi('ai:chatLog:list')")
    @GetMapping("/agentFlowOptions")
    public AjaxResult agentFlowOptions() {
        List<AgentFlow> list = agentFlowService.selectAgentFlowList(new AgentFlow());
        List<Map<String, Object>> data = new java.util.ArrayList<>();
        for (AgentFlow f : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", f.getId());
            m.put("name", f.getAgentFlowName());
            data.add(m);
        }
        return success(data);
    }

    /**
     * 查询某 Agent 下的历史会话 sessionId 下拉
     */
    @PreAuthorize("@ss.hasPermi('ai:chatLog:list')")
    @GetMapping("/agentSessions")
    public AjaxResult agentSessions(@RequestParam("agentId") Long agentId) {
        List<Map<String, Object>> list = chatMessageMapper.selectChatLogSessionIdList(agentId);
        return success(list);
    }

    /**
     * 查询某 Agent Flow 下的历史会话 sessionId 下拉
     */
    @PreAuthorize("@ss.hasPermi('ai:chatLog:list')")
    @GetMapping("/agentFlowSessions")
    public AjaxResult agentFlowSessions(@RequestParam("agentFlowId") Long agentFlowId) {
        List<Map<String, Object>> list = chatFlowMessageMapper.selectChatLogSessionIdList(agentFlowId);
        return success(list);
    }

    /**
     * 删除单次对话（按 traceId）
     */
    @PreAuthorize("@ss.hasPermi('ai:chatLog:remove')")
    @DeleteMapping("/{traceId}")
    public AjaxResult remove(@PathVariable("traceId") String traceId,
                             @RequestParam(value = "type", required = false, defaultValue = "agent") String type) {
        int rows;
        if ("agentFlow".equalsIgnoreCase(type)) {
            rows = chatFlowMessageMapper.deleteChatLogByTraceId(traceId);
        } else {
            rows = chatMessageMapper.deleteChatLogByTraceId(traceId);
        }
        return toAjax(rows);
    }

    /**
     * 导出对话日志（占位：复用 list 查询返回）
     */
    @PreAuthorize("@ss.hasPermi('ai:chatLog:export')")
    @GetMapping("/export")
    public AjaxResult export(ChatLogQuery query) {
        startPage();
        List<Map<String, Object>> list;
        if ("agentFlow".equalsIgnoreCase(query.getType())) {
            list = chatFlowMessageMapper.selectChatLogTraceList(
                    query.getAgentFlowId(), query.getSessionId(), query.getTraceId());
        } else {
            list = chatMessageMapper.selectChatLogTraceList(
                    query.getAgentId(), query.getSessionId(), query.getTraceId());
        }
        return success(list);
    }
}
