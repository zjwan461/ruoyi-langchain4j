package com.ruoyi.ai.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.domain.AgentFlow;
import com.ruoyi.ai.domain.AgentToolInvoke;
import com.ruoyi.ai.domain.Attachment;
import com.ruoyi.ai.domain.ChatFlowMessage;
import com.ruoyi.ai.service.AgenticService;
import com.ruoyi.ai.service.IAIAgentService;
import com.ruoyi.ai.service.IAttachmentService;
import com.ruoyi.ai.service.IChatFlowMessageService;
import com.ruoyi.ai.util.AgenticRequestUtil;
import com.ruoyi.ai.util.AgenticRequestUtil.AgenticChatRequest;
import com.ruoyi.ai.util.AgenticRequestUtil.ToolCalledInfo;
import com.ruoyi.ai.util.Base64DataUriUtil;
import com.ruoyi.ai.util.Constants;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.framework.web.service.TokenService;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@Service
public class AgenticServiceImpl implements AgenticService {

    @Resource
    private AgenticRequestUtil agenticRequestUtil;

    @Resource
    private TokenService tokenService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private IAttachmentService attachmentService;

    @Resource
    private IAIAgentService aiAgentService;

    @Resource
    private IChatFlowMessageService chatFlowMessageService;

    @Override
    public Flux<String> chat(AgentFlow agentFlow, String prompt, String clientId, String sessionId,
            List<Long> attachmentIds, boolean enableWebSearch) {

        String traceId = IdUtils.fastSimpleUUID();
        // save user message
        ChatFlowMessage userMessage = new ChatFlowMessage();
        userMessage.setAgentFlowId(agentFlow.getId());
        userMessage.setClientId(clientId);
        userMessage.setSessionId(sessionId);
        userMessage.setContent(prompt);
        userMessage.setRole("user");
        userMessage.setTraceId(traceId);
        userMessage.setCreateTime(new Date());
        chatFlowMessageService.insertChatMessage(userMessage);

        StringBuilder promptBuilder = new StringBuilder(prompt);
        List<String> images = new ArrayList<>();
        if (CollUtil.isNotEmpty(attachmentIds)) {
            List<Attachment> attachments = attachmentService.selectAttachmentByIds(attachmentIds.toArray(Long[]::new));
            userMessage.setAttachments(attachments);
            for (Attachment attachment : attachments) {
                attachment.setMessageFlowId(userMessage.getId());
                attachmentService.updateAttachment(attachment);
                if (FileUtils.isImageBySuffix(attachment.getFileName())) {
                    Base64DataUriUtil.Base64ParseResult parseResult = Base64DataUriUtil
                            .parse(attachment.getFileContent());
                    images.add(parseResult.getPureBase64());
                } else {
                    promptBuilder
                            .append(String.format(Constants.DEFAULT_ATTACHMENT_FILE_PROMPT, attachment.getFileName(),
                                    attachment.getFileContent()));
                }
            }
        }

        String token = Constants.AGENTIC_TOKEN;
        return Flux.create(fluxSink -> {
            ThreadUtil.execute(() -> {
                AtomicLong currentAgentId = new AtomicLong(-1);
                AtomicReference<ChatFlowMessage> currentChatMessage = new AtomicReference<>();
                try {
                    agenticRequestUtil.chatStreaming(token,
                            AgenticChatRequest.of(agentFlow.getId(), agentFlow.getAgentFlowName(),
                                    agentFlow.getFlowJson(), promptBuilder.toString(), clientId, sessionId,
                                    enableWebSearch, images),
                            response -> {
                                if (currentAgentId.get() != response.getAgentId()) {
                                    // 表明切换智能体
                                    // 1. 先保存上一条chat flow message
                                    ChatFlowMessage lastFlowMessage = currentChatMessage.getAndSet(null);
                                    if (lastFlowMessage != null) {
                                        this.saveAIMessage(lastFlowMessage);
                                    }
                                    // 2. 修改当前agent id
                                    currentAgentId.set(response.getAgentId());
                                    // 3. 创建新chat flow message, 并设置为当前chat flow message
                                    ChatFlowMessage newFlowMessage = new ChatFlowMessage();
                                    newFlowMessage.setAgentId(response.getAgentId());
                                    newFlowMessage.setAgentFlowId(agentFlow.getId());
                                    newFlowMessage.setClientId(clientId);
                                    newFlowMessage.setSessionId(sessionId);
                                    newFlowMessage.setRole("assistant");
                                    newFlowMessage.setAgentName(response.getAgentName());
                                    newFlowMessage.setContent(response.getMsg());
                                    newFlowMessage.setTraceId(traceId);
                                    currentChatMessage.set(newFlowMessage);
                                } else {
                                    // 表明并未切换智能体，更新message chunk
                                    ChatFlowMessage chatFlowMessage = currentChatMessage.get();
                                    if (StringUtils.isNotBlank(response.getMsg())) {
                                        chatFlowMessage.setContent(chatFlowMessage.getContent() == null ? ""
                                                : chatFlowMessage.getContent() + response.getMsg());
                                    }
                                    List<AgentToolInvoke> toolCalledList = chatFlowMessage.getToolCalledList();
                                    toolCalledList = toolCalledList == null ? new ArrayList<>() : toolCalledList;
                                    ToolCalledInfo tool_called = response.getTool_called();
                                    if (tool_called != null) {
                                        AgentToolInvoke toolInvoke = new AgentToolInvoke();
                                        toolInvoke.setAgentId(response.getAgentId());
                                        toolInvoke.setSessionId(sessionId);
                                        toolInvoke.setClientId(clientId);
                                        toolInvoke.setToolName(tool_called.getToolName());
                                        toolInvoke.setToolInvokeResult(tool_called.getToolInvokeResultText());
                                        toolCalledList.add(toolInvoke);
                                        chatFlowMessage.setToolCalledList(toolCalledList);
                                    }
                                }

                                try {
                                    fluxSink.next(objectMapper.writeValueAsString(response));
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }
                            });

                } catch (Exception e) {
                    log.error("chat agentic error", e);
                    fluxSink.error(e);
                } finally {
                    fluxSink.complete();
                    ChatFlowMessage flowMessage = currentChatMessage.getAndSet(null);
                    if (flowMessage != null) {
                        this.saveAIMessage(flowMessage);
                    }
                }
            });

        });
    }

    protected void saveAIMessage(ChatFlowMessage flowMessage) {
        try {
            flowMessage.setCreateTime(new Date());
            chatFlowMessageService.insertChatMessage(flowMessage);
            // 保存工具调用
            List<AgentToolInvoke> toolCalledList = flowMessage.getToolCalledList();
            if (CollUtil.isNotEmpty(toolCalledList)) {
                for (AgentToolInvoke invoke : toolCalledList) {
                    invoke.setMessageFlowId(flowMessage.getId());
                    invoke.setCreateTime(new Date());
                    aiAgentService.insertAgentToolInvoked(invoke);
                }
            }
        } catch (Exception e) {
            log.error("save AI message error", e);
        }
    }

}
