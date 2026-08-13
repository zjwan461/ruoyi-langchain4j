package com.ruoyi.ai.controller;

import com.ruoyi.ai.controller.model.ChatReq;
import com.ruoyi.ai.domain.AgentFlow;
import com.ruoyi.ai.service.AgenticService;
import com.ruoyi.ai.service.IAIChatService;
import com.ruoyi.ai.service.IAgentFlowService;
import com.ruoyi.ai.service.IAttachmentService;
import com.ruoyi.ai.util.DocumentParseClient;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DictUtils;
import com.ruoyi.framework.config.ServerConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
@RequestMapping("/ai-flow-chat")
public class AIFlowChatController extends ChatController {

    @Resource
    private IAgentFlowService agentFlowService;

    @Resource
    private AgenticService agenticService;

    public AIFlowChatController(@Qualifier("agentFlowChatService") IAIChatService aiChatService,
            ServerConfig serverConfig,
            DocumentParseClient documentParseClient, IAttachmentService attachmentService) {
        super(aiChatService, serverConfig, documentParseClient, attachmentService);
    }

    @PostMapping(produces = { MediaType.TEXT_EVENT_STREAM_VALUE })
    public Flux<String> aiChat(@RequestBody @Valid ChatReq chatReq) {
        AgentFlow agentFlow = agentFlowService.selectAgentFlowById(chatReq.getAgentFlowId());
        this.preCheckAgentFlow(agentFlow, chatReq);
        String originFlow = DictUtils.getDictValue("agent_flow_type", "origin");
        String agenticFlow = DictUtils.getDictValue("agent_flow_type", "agentic");
        if (originFlow.equals(agentFlow.getImplType())) {
            return aiChatService.chat(agentFlow, chatReq.getPrompt(), chatReq.getClientId(), chatReq.getSessionId(),
                    chatReq.getAttachmentIds(), chatReq.isWebSearch());
        } else if (agenticFlow.equals(agentFlow.getImplType())) {
            return agenticService.chat(agentFlow, chatReq.getPrompt(), chatReq.getClientId(), chatReq.getSessionId(),
                    chatReq.getAttachmentIds(), chatReq.isWebSearch());
        } else {
            throw new ServiceException("unsupported flow impl type: " + agentFlow.getImplType());
        }
    }

    @GetMapping("/agent-info/{visitId}")
    public AjaxResult agentInfo(@PathVariable("visitId") String visitId) {
        return success(agentFlowService.selectAgentFlowByVisitId("/ai-flow-chat/" + visitId));
    }

    @PostMapping("/create-session/{clientId}")
    public AjaxResult createSession(@PathVariable("clientId") String clientId) {
        return AjaxResult.success("success", aiChatService.createSession(clientId));
    }

    @GetMapping("/list-chat-session/{clientId}/{agentId}")
    public AjaxResult listChatSession(@PathVariable("clientId") String clientId,
            @PathVariable("agentId") Long agentId) {
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
        return getDataTable(aiChatService.listAgentFlowChatMessageBySessionIdOrderDesc(sessionId, agentId));
    }

    @DeleteMapping("/delete-session/{sessionId}")
    public AjaxResult deleteSession(@PathVariable("sessionId") String sessionId) {
        aiChatService.deleteSession(sessionId);
        return success();
    }
}
