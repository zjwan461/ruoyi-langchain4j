package com.ruoyi.ai.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.controller.model.ChatReq;
import com.ruoyi.ai.dify.api.DifyChatRequest;
import com.ruoyi.ai.dify.api.DifyConversationsResponse;
import com.ruoyi.ai.dify.api.DifyMessagesResponse;
import com.ruoyi.ai.dify.api.DifyRequestUtil;
import com.ruoyi.ai.dify.domain.DifyApi;
import com.ruoyi.ai.dify.domain.DifySessionMapping;
import com.ruoyi.ai.dify.service.IDifyApiService;
import com.ruoyi.ai.dify.service.IDifySessionMappingService;
import com.ruoyi.ai.domain.AIAgent;
import com.ruoyi.ai.domain.ChatMessage;
import com.ruoyi.ai.service.IAIAgentService;
import com.ruoyi.ai.service.IAIChatService;
import com.ruoyi.ai.util.Constants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.bean.BeanUtils;
import com.ruoyi.system.service.ISysConfigService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/dify-chat")
public class DifyChatController extends ChatController {


    @Resource
    IDifyApiService difyApiService;

    @Resource
    IAIAgentService aiAgentService;

    @Resource
    DifyRequestUtil difyRequestUtil;

    @Resource
    ISysConfigService sysConfigService;

    @Resource
    private IDifySessionMappingService difySessionMappingService;

    @Resource
    private ObjectMapper objectMapper;

    public DifyChatController(IAIChatService aiChatService) {
        super(aiChatService);
    }

    @PostMapping(produces = {MediaType.TEXT_EVENT_STREAM_VALUE})
    public Flux<String> aiChat(@RequestBody @Valid ChatReq chatReq) {
        AIAgent aiAgent = aiAgentService.selectAiAgentById(chatReq.getAgentId());
        DifyApi difyApi = difyApiService.selectDifyApiById(aiAgent.getDifyApiId());
        this.preCheck(aiAgent, chatReq);
        DifySessionMapping mapping = difySessionMappingService.selectDifySessionMappingBySessionIdAndClientId(chatReq.getSessionId(), chatReq.getClientId());
        if (mapping == null) {
            throw new ServiceException("未找到sessionId=" + chatReq.getSessionId() + ",clientId=" + chatReq.getClientId() + "的会话");
        }
        DifyChatRequest difyChatRequest = new DifyChatRequest();
        difyChatRequest.setUser(chatReq.getClientId() + ":" + Constants.DIFY_USER);
        difyChatRequest.setQuery(chatReq.getPrompt());
        difyChatRequest.setConversationId(mapping.getConversationId());
        return Flux.create(fluxSink -> {
            ThreadUtil.execute(() -> {
                try {
                    difyRequestUtil.chatStreaming(difyApi.getUrlPrefix(), difyApi.getApiKey(), difyChatRequest, response -> {
                        String event = response.getEvent();
                        if ("message".equals(event)) {
                            String answer = response.getAnswer();
                            String conversationId = response.getConversationId();
                            if (StringUtils.isNotBlank(conversationId) && StringUtils.isBlank(mapping.getConversationId())) {
                                mapping.setConversationId(conversationId);
                            }
                            Map<String, String> msg = MapUtil.<String, String>builder()
                                    .put("msg", answer)
                                    .build();
                            try {
                                fluxSink.next(objectMapper.writeValueAsString(msg));
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                } catch (Exception e) {
                    fluxSink.error(e);
                } finally {
                    fluxSink.complete();
                    difySessionMappingService.updateDifySessionMapping(mapping);
                }
            });
        });

    }


    @GetMapping("/agent-info/{visitId}")
    public AjaxResult agentInfo(@PathVariable("visitId") String visitId) {
        return success(aiAgentService.selectByVisitId("/dify-chat/" + visitId));
    }

    @PostMapping("/create-session/{clientId}/{agentId}")
    public AjaxResult createSession(@PathVariable("clientId") String clientId, @PathVariable("agentId") Long agentId) {
        String sessionId = aiChatService.createSession(clientId);
        DifySessionMapping mapping = new DifySessionMapping();
        mapping.setSessionId(sessionId);
        mapping.setClientId(clientId);
        mapping.setAgentId(agentId);
        difySessionMappingService.deleteDifySessionMappingByClientIdAndEmptyConversationId(clientId);
        difySessionMappingService.insertDifySessionMapping(mapping);
        return AjaxResult.success("success", sessionId);
    }

    @GetMapping("/list-chat-session/{clientId}/{agentId}")
    public AjaxResult listChatSession(@PathVariable("clientId") String clientId, @PathVariable("agentId") Long agentId) {
        AIAgent aiAgent = aiAgentService.selectAiAgentById(agentId);
        DifyApi difyApi = difyApiService.selectDifyApiById(aiAgent.getDifyApiId());
        List<DifySessionMapping> mappings = difySessionMappingService.selectDifySessionMappingByClientId(clientId);
        if (CollUtil.isEmpty(mappings)) {
            return success();
        }
        String value = sysConfigService.selectConfigByKey("ai.agent.sessionNum");
        Integer limit = null;
        if (StringUtils.isNotEmpty(value)) {
            limit = Integer.valueOf(value);
        }
        DifyConversationsResponse conversations = difyRequestUtil.getConversations(difyApi.getUrlPrefix(), difyApi.getApiKey(),
                clientId + ":" + Constants.DIFY_USER, null, limit);
        List<DifyConversationsResponse.Conversation> data = conversations.getData();
        List<Map<String, Object>> list = new ArrayList<>();
        for (DifyConversationsResponse.Conversation datum : data) {
            String conversationId = datum.getId();
            Optional<DifySessionMapping> first = mappings.stream().filter(mapping -> mapping.getConversationId().equals(conversationId)).findFirst();
            DifySessionMapping mapping = first.orElse(null);
            if (mapping == null) {
                continue;
            }
            Map<String, Object> res = MapUtil.<String, Object>builder()
                    .put("sessionId", mapping.getSessionId())
                    .put("title", datum.getName())
                    .build();
            list.add(res);
        }
        return AjaxResult.success(list);
    }

    @GetMapping("/list-chat-message/{sessionId}/{agentId}")
    public AjaxResult listChatHistory(@PathVariable("sessionId") String sessionId, @PathVariable("agentId") Long agentId) {
        List<ChatMessage> list = new ArrayList<>();
        DifySessionMapping mapping = difySessionMappingService.selectDifySessionMappingBySessionId(sessionId);
        if (mapping == null || StringUtils.isBlank(mapping.getConversationId())) {
            return AjaxResult.success();
        }
        AIAgent aiAgent = aiAgentService.selectAiAgentById(agentId);
        DifyApi difyApi = difyApiService.selectDifyApiById(aiAgent.getDifyApiId());
        DifyMessagesResponse messages = difyRequestUtil.getMessages(difyApi.getUrlPrefix(), difyApi.getApiKey(), mapping.getClientId() + ":" + Constants.DIFY_USER,
                mapping.getConversationId(), "", 100);
        List<DifyMessagesResponse.Message> data = messages.getData();
        for (DifyMessagesResponse.Message datum : data) {
            ChatMessage userChatMessage = new ChatMessage();
            ChatMessage assistantChatMessage = new ChatMessage();
            userChatMessage.setRole("user");
            userChatMessage.setAgentId(aiAgent.getId());
            userChatMessage.setSessionId(mapping.getSessionId());
            userChatMessage.setContent(datum.getQuery());
            userChatMessage.setClientId(mapping.getClientId());
            userChatMessage.setCreateTime(new Date(datum.getCreatedAt() * 1000));
            BeanUtils.copyProperties(userChatMessage, assistantChatMessage);
            assistantChatMessage.setRole("assistant");
            assistantChatMessage.setContent(datum.getAnswer());
            list.add(userChatMessage);
            list.add(assistantChatMessage);
        }
        return AjaxResult.success(list);
    }

    @DeleteMapping("/delete-session/{sessionId}")
    public AjaxResult deleteSession(@PathVariable("sessionId") String sessionId) {
        DifySessionMapping mapping = difySessionMappingService.selectDifySessionMappingBySessionId(sessionId);
        if (mapping != null && StringUtils.isNotBlank(mapping.getConversationId())) {
            AIAgent aiAgent = aiAgentService.selectAiAgentById(mapping.getAgentId());
            DifyApi difyApi = difyApiService.selectDifyApiById(aiAgent.getDifyApiId());
            difyRequestUtil.deleteConversation(difyApi.getUrlPrefix(), difyApi.getApiKey(), mapping.getConversationId(),
                    mapping.getClientId() + ":" + Constants.DIFY_USER);
        }
        difySessionMappingService.deleteDifySessionMappingBySessionId(sessionId);
        return success();
    }
}
