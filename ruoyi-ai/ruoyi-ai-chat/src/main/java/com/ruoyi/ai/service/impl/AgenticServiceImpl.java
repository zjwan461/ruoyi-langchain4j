package com.ruoyi.ai.service.impl;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.domain.AgentFlow;
import com.ruoyi.ai.domain.Attachment;
import com.ruoyi.ai.service.AgenticService;
import com.ruoyi.ai.service.IAttachmentService;
import com.ruoyi.ai.util.AgenticRequestUtil;
import com.ruoyi.ai.util.AgenticRequestUtil.AgenticChatRequest;
import com.ruoyi.ai.util.Base64DataUriUtil;
import com.ruoyi.ai.util.Constants;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.framework.web.service.TokenService;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.ThreadUtil;
import reactor.core.publisher.Flux;

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

    @Override
    public Flux<String> chat(AgentFlow agentFlow, String prompt, String clientId, String sessionId,
            List<Long> attachmentIds, boolean enableWebSearch) {

        StringBuilder promptBuilder = new StringBuilder(prompt);
        List<String> images = new ArrayList<>();
        if (CollUtil.isNotEmpty(attachmentIds)) {
            List<Attachment> attachments = attachmentService.selectAttachmentByIds(attachmentIds.toArray(Long[]::new));
            for (Attachment attachment : attachments) {
                attachment.setMessageFlowId(agentFlow.getId());
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
                try {
                    agenticRequestUtil.chatStreaming(token,
                            AgenticChatRequest.of(agentFlow.getId(), promptBuilder.toString(), clientId, sessionId,
                                    enableWebSearch, images),
                            response -> {
                                try {
                                    fluxSink.next(objectMapper.writeValueAsString(response));
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
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

}
