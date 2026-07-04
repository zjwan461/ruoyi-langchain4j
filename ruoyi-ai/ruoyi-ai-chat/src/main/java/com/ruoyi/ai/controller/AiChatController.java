package com.ruoyi.ai.controller;

import com.ruoyi.ai.controller.model.ChatReq;
import com.ruoyi.ai.domain.AiAgent;
import com.ruoyi.ai.service.IAiAgentService;
import com.ruoyi.ai.service.IAiChatService;
import com.ruoyi.ai.service.IChatMessageService;
import com.ruoyi.common.core.domain.AjaxResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/ai-chat")
public class AiChatController extends ChatController {

    @Resource
    HttpServletRequest request;

    @Resource
    IAiAgentService aiAgentService;

    @Resource
    private IChatMessageService chatMessageService;

    public AiChatController(IAiChatService aiChatService) {
        super(aiChatService);
    }

    @PostMapping(produces = {MediaType.TEXT_EVENT_STREAM_VALUE})
    public Flux<String> aiChat(@RequestBody @Valid ChatReq chatReq) {
        AiAgent aiAgent = aiAgentService.selectAiAgentById(chatReq.getAgentId());
        this.preCheck(aiAgent, chatReq);
        return aiChatService.chat(aiAgent, chatReq.getPrompt(), chatReq.getClientId(),
                chatReq.getSessionId());
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

    @GetMapping("/list-chat-message/{sessionId}/{agentId}")
    public AjaxResult listChatHistory(@PathVariable("sessionId") String sessionId, @PathVariable("agentId") Long agentId) {
        return success(aiChatService.listAgentChatMessageBySessionId(sessionId, agentId));
    }

    @DeleteMapping("/delete-session/{sessionId}")
    public AjaxResult deleteSession(@PathVariable("sessionId") String sessionId) {
        aiChatService.deleteSession(sessionId);
        return success();
    }
}
