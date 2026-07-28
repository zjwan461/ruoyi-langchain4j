package com.ruoyi.ai.controller;

import com.ruoyi.ai.controller.model.ChatReq;
import com.ruoyi.ai.domain.AIAgent;
import com.ruoyi.ai.domain.AgentFlow;
import com.ruoyi.ai.domain.Attachment;
import com.ruoyi.ai.service.IAIChatService;
import com.ruoyi.ai.service.IAttachmentService;
import com.ruoyi.ai.util.DocumentParseClient;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DictUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.framework.config.ServerConfig;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public class ChatController extends BaseController {

    protected final IAIChatService aiChatService;

    protected final ServerConfig serverConfig;

    protected final DocumentParseClient documentParseClient;

    protected final IAttachmentService attachmentService;

    public ChatController(IAIChatService aiChatService, ServerConfig serverConfig, DocumentParseClient documentParseClient,
                          IAttachmentService attachmentService) {
        this.aiChatService = aiChatService;
        this.serverConfig = serverConfig;
        this.documentParseClient = documentParseClient;
        this.attachmentService = attachmentService;
    }

    protected void preCheck(AIAgent aiAgent, ChatReq chatReq) {
        String agentEnable = DictUtils.getDictValue("ai_agent_status", "已启用");
        if (!StringUtils.equals(agentEnable, String.valueOf(aiAgent.getStatus()))) {
            throw new ServiceException("当前Agent未启用");
        }
        //检查每个客户端每日请求限额是否超限
        if (!aiChatService.checkIfOverLmtRequest(aiAgent.getId(), aiAgent.getDayLmtPerClient(), chatReq.getClientId())) {
            throw new ServiceException("客户端当日请求已达上限");
        }
        //检查客户端id和当前激活的sessionId是否匹配
//    if (!aiChatService.checkClientSession(chatReq.getClientId(), chatReq.getSessionId())) {
//      throw new ServiceException("会话与客户端不匹配");
//    }
    }

    protected void preCheckAgentFlow(AgentFlow agentFlow, ChatReq chatReq) {
        String agentEnable = DictUtils.getDictValue("ai_agent_status", "已启用");
        if (!StringUtils.equals(agentEnable, String.valueOf(agentFlow.getStatus()))) {
            throw new ServiceException("当前AgentFlow未启用");
        }
        //检查每个客户端每日请求限额是否超限
        if (!aiChatService.checkIfOverLmtRequest(agentFlow.getId(), agentFlow.getDayLmtPerClient(), chatReq.getClientId())) {
            throw new ServiceException("客户端当日请求已达上限");
        }
        if (StringUtils.isBlank(agentFlow.getFlowJson())) {
            throw new ServiceException("尚未进行智能体编排");
        }
    }


    @PostMapping("/ai-chat/attach-file")
    public AjaxResult uploadFile(MultipartFile file) throws Exception {
        try {
            // 上传文件路径
            String filePath = RuoYiConfig.getUploadPath();
            // 上传并返回新文件名称
            String fileName = FileUploadUtils.upload(filePath, file);
            fileName = fileName.replace("/profile", RuoYiConfig.getProfile());
            String url = serverConfig.getUrl() + fileName;
            String simpleName = FileUtils.getName(fileName);
            String fileExt = FileUtils.getFileExt(simpleName);
            Attachment attachment = new Attachment();
            attachment.setFileName(simpleName);
            attachment.setFileExt(fileExt);

            if (FileUtils.isImageBySuffix(simpleName)) {
                attachment.setFileContent(FileUtils.readImageToDataBase64(fileName));
            } else {
                DocumentParseClient.DocParseItem result = documentParseClient.parseFile(Path.of(fileName));
                attachment.setFileContent(result.getPage_content());
            }

            attachmentService.insertAttachment(attachment);
            AjaxResult ajax = AjaxResult.success();
            ajax.put("url", url);
            ajax.put("fileName", fileName);
            ajax.put("newFileName", simpleName);
            ajax.put("originalFilename", file.getOriginalFilename());
            ajax.put("attachment", attachment);
            return ajax;
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }
}
