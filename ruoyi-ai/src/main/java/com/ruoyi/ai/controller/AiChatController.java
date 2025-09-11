package com.ruoyi.ai.controller;

import com.ruoyi.ai.controller.model.ChatReq;
import com.ruoyi.ai.domain.AiAgent;
import com.ruoyi.ai.service.IAiAgentService;
import com.ruoyi.ai.service.IAiChatService;
import com.ruoyi.ai.service.IChatMessageService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/ai-chat")
public class AiChatController extends BaseController {

    @Resource
    HttpServletRequest request;

    @Resource
    IAiAgentService aiAgentService;

    @Resource
    IAiChatService aiChatService;

    @Resource
    private IChatMessageService chatMessageService;


    @PostMapping(produces = {MediaType.TEXT_EVENT_STREAM_VALUE})
    public Flux<String> aiChat(@RequestBody @Valid ChatReq chatReq) {
        AiAgent aiAgent = aiAgentService.selectAiAgentById(chatReq.getAgentId());
        preCheck(aiAgent, chatReq);
        return aiChatService.chat(aiAgent, chatReq.getPrompt(), chatReq.getClientId(),
                chatReq.getSessionId());
    }

    private void preCheck(AiAgent aiAgent, ChatReq chatReq) {
        //检查每个客户端每日请求限额是否超限
        if (!aiChatService.checkIfOverLmtRequest(aiAgent.getId(), aiAgent.getDayLmtPerClient(),
                chatReq.getClientId())) {
            throw new ServiceException("客户端当日请求已达上限");
        }
        //检查客户端id和当前激活的sessionId是否匹配
//    if (!aiChatService.checkClientSession(chatReq.getClientId(), chatReq.getSessionId())) {
//      throw new ServiceException("会话与客户端不匹配");
//    }
    }

    @GetMapping("/agent-info/{visitId}")
    public AjaxResult agentInfo(@PathVariable("visitId") String visitId) {
        return success(aiAgentService.selectByVisitId(visitId));
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
