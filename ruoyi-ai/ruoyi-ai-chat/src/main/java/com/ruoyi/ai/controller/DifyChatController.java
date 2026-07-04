package com.ruoyi.ai.controller;

import cn.hutool.core.thread.ThreadUtil;
import com.ruoyi.ai.controller.model.ChatReq;
import com.ruoyi.ai.dify.api.DifyChatRequest;
import com.ruoyi.ai.dify.api.DifyRequestUtil;
import com.ruoyi.ai.dify.domain.DifyApi;
import com.ruoyi.ai.dify.service.IDifyApiService;
import com.ruoyi.ai.domain.AiAgent;
import com.ruoyi.ai.service.IAiAgentService;
import com.ruoyi.ai.service.IAiChatService;
import com.ruoyi.ai.service.IChatMessageService;
import com.ruoyi.ai.util.Constants;
import com.ruoyi.common.core.domain.AjaxResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/dify-chat")
public class DifyChatController extends ChatController {

    @Resource
    HttpServletRequest request;

    @Resource
    IDifyApiService difyApiService;

    @Resource
    IAiAgentService aiAgentService;

    @Resource
    DifyRequestUtil difyRequestUtil;

    @Resource
    private IChatMessageService chatMessageService;

    public DifyChatController(IAiChatService aiChatService) {
        super(aiChatService);
    }

    @PostMapping(produces = {MediaType.TEXT_EVENT_STREAM_VALUE})
    public Flux<String> aiChat(@RequestBody @Valid ChatReq chatReq) {
        AiAgent aiAgent = aiAgentService.selectAiAgentById(chatReq.getAgentId());
        DifyApi difyApi = difyApiService.selectDifyApiById(aiAgent.getDifyApiId());
        this.preCheck(aiAgent, chatReq);
        DifyChatRequest difyChatRequest = new DifyChatRequest();
        difyChatRequest.setUser(Constants.DIFY_USER);
        difyChatRequest.setQuery(chatReq.getPrompt());
        return Flux.create(fluxSink -> {
            ThreadUtil.execute(() -> {
                try {
                    difyRequestUtil.chatStreaming(difyApi.getUrlPrefix(), difyApi.getApiKey(), difyChatRequest, response -> {
                        String event = response.getEvent();
                        if ("message".equals(event)) {
                            String answer = response.getAnswer();
                            fluxSink.next(answer);
                        }
                    });
                } catch (Exception e) {
                    fluxSink.error(e);
                } finally {
                    fluxSink.complete();
                }
            });
        });

    }


    @GetMapping("/agent-info/{visitId}")
    public AjaxResult agentInfo(@PathVariable("visitId") String visitId) {
        return success(aiAgentService.selectByVisitId("/dify-chat/" + visitId));
    }

    @PostMapping("/create-session/{clientId}")
    public AjaxResult createSession(@PathVariable("clientId") String clientId) {
        return AjaxResult.success("success", aiChatService.createSession(clientId));
    }

    @GetMapping("/list-chat-session/{clientId}/{agentId}")
    public AjaxResult listChatSession(@PathVariable("clientId") String clientId, @PathVariable("agentId") Long agentId) {
        //todo get history from dify api
        return AjaxResult.success();
    }

    @GetMapping("/list-chat-message/{sessionId}/{agentId}")
    public AjaxResult listChatHistory(@PathVariable("sessionId") String sessionId, @PathVariable("agentId") Long agentId) {
        //todo get history from dify api
        return AjaxResult.success();
    }

    @DeleteMapping("/delete-session/{sessionId}")
    public AjaxResult deleteSession(@PathVariable("sessionId") String sessionId) {
        //todo delete conversation data from dify api
        return success();
    }
}
