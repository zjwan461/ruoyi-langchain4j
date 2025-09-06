package com.ruoyi.ai.controller;

import com.ruoyi.ai.controller.model.ChatReq;
import com.ruoyi.ai.domain.AiAgent;
import com.ruoyi.ai.service.IAiAgentService;
import com.ruoyi.ai.service.IAiChatService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class AiChatController extends BaseController {

  @Resource
  HttpServletRequest request;

  @Resource
  IAiAgentService aiAgentService;

  @Resource
  IAiChatService aiChatService;

  @Resource
  private RedisTemplate<Object, Object> redisTemplate;

  @Resource
  private RedisScript<Long> limitScript;

  public static final String AI_AGENT_CHAT_LMT = "ai:agent:limit:";


  @PostMapping(value = "/ai-chat", produces = {MediaType.TEXT_EVENT_STREAM_VALUE})
  public Flux<String> aiChat(@RequestBody @Valid ChatReq chatReq) {
    AiAgent aiAgent = aiAgentService.selectAiAgentById(chatReq.getAgentId());
    preCheck(aiAgent, chatReq.getSessionId());
    return aiChatService.chat(aiAgent, chatReq.getPrompt(), chatReq.getSessionId());

  }

  private void preCheck(AiAgent aiAgent, String clientSessionId) {
    Integer dayLmtPerClient = aiAgent.getDayLmtPerClient();
    Long number = redisTemplate.execute(limitScript,
        Collections.singletonList(AI_AGENT_CHAT_LMT + aiAgent.getId() + ":" + clientSessionId),
        dayLmtPerClient,
        (int) TimeUnit.DAYS.toSeconds(1));
    if (StringUtils.isNull(number) || number.intValue() > dayLmtPerClient) {
      throw new ServiceException("当日请求已达上限");
    }
  }

  @GetMapping("/agent-info/{visitId}")
  public AjaxResult agentInfo(@PathVariable("visitId") String visitId) {
    return success(aiAgentService.selectByVisitId(visitId));
  }
}
