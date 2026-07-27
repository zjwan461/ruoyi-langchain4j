package com.ruoyi.ai.controller;

import cn.hutool.core.collection.CollUtil;
import com.ruoyi.ai.agent.AgentType;
import com.ruoyi.ai.controller.model.ChatReq;
import com.ruoyi.ai.domain.AgentMcpTool;
import com.ruoyi.ai.domain.AgentSkill;
import com.ruoyi.ai.domain.AgentTool;
import com.ruoyi.ai.domain.AIAgent;
import com.ruoyi.ai.service.*;
import com.ruoyi.ai.service.impl.AIAssistantExecutor;
import com.ruoyi.ai.util.DocumentParseClient;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.framework.config.ServerConfig;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/ai-chat")
public class AIChatController extends ChatController {


    @Resource
    IAIAgentService aiAgentService;

    @Resource
    IAgentToolService agentToolService;

    @Resource
    IAgentSkillService agentSkillService;

    @Resource
    IAgentMcpService agentMcpService;

    @Resource
    private AIAssistantExecutor aiAssistantExecutor;

    public AIChatController(IAIChatService aiChatService, ServerConfig serverConfig, DocumentParseClient documentParseClient) {
        super(aiChatService, serverConfig, documentParseClient);
    }

    protected AgentType getAgentType(AIAgent aiAgent) {
        List<AgentTool> agentTools = agentToolService.selectAgentToolByAgentId(aiAgent.getId());
        List<AgentSkill> agentSkills = agentSkillService.selectAgentSkillByAgentId(aiAgent.getId());
        List<AgentMcpTool> agentMcpTools = agentMcpService.selectAgentMcpServiceByAgentId(aiAgent.getId());
        return CollUtil.isEmpty(agentTools) && CollUtil.isEmpty(agentSkills) && CollUtil.isEmpty(agentMcpTools) ? AgentType.simple : AgentType.service;
    }

    @PostMapping(produces = {MediaType.TEXT_EVENT_STREAM_VALUE})
    public Flux<String> aiChat(@RequestBody @Valid ChatReq chatReq) {
        AIAgent aiAgent = aiAgentService.selectAiAgentWithToolAndSkillById(chatReq.getAgentId());
        this.preCheck(aiAgent, chatReq);
        AgentType agentType = this.getAgentType(aiAgent);
        if (agentType == AgentType.simple) {
            return aiChatService.chat(aiAgent, chatReq.getPrompt(), chatReq.getClientId(),
                    chatReq.getSessionId());
        } else {
            return aiAssistantExecutor.chat(aiAgent, chatReq.getPrompt(), chatReq.getClientId(),
                    chatReq.getSessionId());
        }
    }

    @GetMapping("/agent-info/{visitId}")
    public AjaxResult agentInfo(@PathVariable("visitId") String visitId) {
        return success(aiAgentService.selectByVisitId("/ai-chat/" + visitId));
    }

    @PostMapping("/create-session/{clientId}")
    public AjaxResult createSession(@PathVariable("clientId") String clientId) {
        return AjaxResult.success("success", aiChatService.createSession(clientId));
    }

    @GetMapping("/list-chat-session/{clientId}/{agentId}")
    public AjaxResult listChatSession(@PathVariable("clientId") String clientId, @PathVariable("agentId") Long agentId) {
        return success(aiChatService.listClientSession(clientId, agentId));
    }

    /**
     * 分页查询聊天历史：前端使用 pageNum / pageSize，pageNum=1 时取时间最近的一页，
     * 后续 pageNum++ 往前翻更早的历史。配合前端 "向上滚动加载更多" 场景。
     */
    @GetMapping("/list-chat-message/{sessionId}/{agentId}")
    public TableDataInfo listChatHistory(@PathVariable("sessionId") String sessionId,
                                         @PathVariable("agentId") Long agentId) {
        startPage();
        return getDataTable(aiChatService.listAgentChatMessageBySessionIdOrderDesc(sessionId, agentId));
    }

    @DeleteMapping("/delete-session/{sessionId}")
    public AjaxResult deleteSession(@PathVariable("sessionId") String sessionId) {
        aiChatService.deleteSession(sessionId);
        return success();
    }
}
