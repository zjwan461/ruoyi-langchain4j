package com.ruoyi.ai.controller;

import com.ruoyi.ai.controller.model.ChatReq;
import com.ruoyi.ai.domain.AIAgent;
import com.ruoyi.ai.domain.AgentFlow;
import com.ruoyi.ai.service.IAIChatService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DictUtils;
import com.ruoyi.common.utils.StringUtils;

public class ChatController extends BaseController {

    protected final IAIChatService aiChatService;

    public ChatController(IAIChatService aiChatService) {
        this.aiChatService = aiChatService;
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
}
