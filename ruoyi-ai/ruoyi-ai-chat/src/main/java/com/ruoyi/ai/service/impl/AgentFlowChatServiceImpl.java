package com.ruoyi.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.component.flow.FlowTreeParser;
import com.ruoyi.ai.component.flow.dto.AgentNode;
import com.ruoyi.ai.component.flow.dto.AgentRoundOut;
import com.ruoyi.ai.component.flow.dto.AgentSpec;
import com.ruoyi.ai.component.flow.dto.TreeNode;
import com.ruoyi.ai.component.memory.RedisMemoryChatMemoryStore;
import com.ruoyi.ai.config.AiConfig;
import com.ruoyi.ai.domain.AIAgent;
import com.ruoyi.ai.domain.AgentFlow;
import com.ruoyi.ai.domain.ChatFlowMessage;
import com.ruoyi.ai.service.*;
import com.ruoyi.ai.util.Constants;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.system.service.ISysConfigService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Getter
@Setter
@Component("agentFlowChatService")
@Scope("prototype")
public class AgentFlowChatServiceImpl extends AIBaseChatServiceImpl implements AgentFlowService {

    private AIAssistant currentAssistant;
    private AIAgent currentAgent;
    private String currentNodeId;
    private TreeNode treeNode;
    private AgentFlow agentFlow;
    private AIAgent rootAgent;

    @Resource
    private FlowTreeParser parser;
    @Resource
    private IChatFlowMessageService chatFlowMessageService;

    public AgentFlowChatServiceImpl(IChatMessageService chatMessageService,
                                    InMemoryChatMemoryStore inMemoryChatMemoryStore,
                                    RedisMemoryChatMemoryStore redisMemoryChatMemoryStore,
                                    AiConfig aiConfig,
                                    ObjectMapper objectMapper,
                                    RedisTemplate<Object, Object> redisTemplate,
                                    RedisScript<Long> limitScript,
                                    ISysConfigService sysConfigService,
                                    IKnowledgeBaseService knowledgeBaseService,
                                    IModelService modelService,
                                    ModelBuilder modelBuilder,
                                    IAIAgentService aiAgentService,
                                    PgVectorEmbeddingStore pgVectorEmbeddingStore) {
        super(chatMessageService,
                inMemoryChatMemoryStore,
                redisMemoryChatMemoryStore,
                aiConfig,
                objectMapper,
                redisTemplate,
                limitScript,
                sysConfigService,
                knowledgeBaseService,
                modelService,
                modelBuilder,
                aiAgentService,
                pgVectorEmbeddingStore);
    }


    @Override
    public void init(String sessionId, AgentFlow agentFlow) {
        this.treeNode = parser.parseToTree(agentFlow.getFlowJson());
        this.agentFlow = agentFlow;
        this.currentNodeId = treeNode.getNode().getNodeId();
        Integer rootAgentId = treeNode.getNode().getAgentId();
        this.currentAgent = this.aiAgentService.selectAiAgentWithToolAndSkillById(Long.valueOf(rootAgentId));
        this.rootAgent = currentAgent;
        this.currentAssistant = this.buildAIAssistant(this.currentAgent, sessionId, true);
    }

    /**
     * 执行一轮Agent，本轮完全结束后返回流转指令
     * 不再在内部递归/block启动下一轮
     */
    public Mono<AgentRoundOut> invoke(String sessionId, String clientId, String request, FluxSink<String> fluxSink, AIAgent aiAgent) {
        String invokeId = IdUtils.fastSimpleUUID();
        String promptTemplate = currentAgent.getPromptTemplate();
        if (promptTemplate.contains(Constants.USER_MSG_TEMPLATE)) {
            promptTemplate = promptTemplate.replace(Constants.USER_MSG_TEMPLATE, request);
        } else {
            promptTemplate = promptTemplate + request;
        }
        if (promptTemplate.contains(Constants.KNOWLEDGE_BASE_TEMPLATE) && StringUtils.isNotBlank(currentAgent.getKbIds())) {
            promptTemplate = promptTemplate.replace(Constants.KNOWLEDGE_BASE_TEMPLATE, "");
        }
        String finalPromptTemplate = this.attachSubAgentPrompt(promptTemplate) + "\n\n当前invokeId=" + invokeId;
        AtomicReference<List<Map<String, Object>>> retrieveList = new AtomicReference<>();
        AtomicReference<List<Map<String, Object>>> toolCallList = new AtomicReference<>();
        // 本轮结束向外传递的结果
        AtomicReference<AgentRoundOut> outRef = new AtomicReference<>(
                AgentRoundOut.builder()
                        .needNext(false)
                        .transferRequest(request)
                        .build()
        );

        TokenStream tokenStream = currentAssistant.chatStream(finalPromptTemplate);
        AtomicInteger thinkCount = new AtomicInteger(0);
        AtomicBoolean thinkFinish = new AtomicBoolean(false);
        return Mono.create(sink -> tokenStream.onPartialThinking(thinkingChunk -> {
                    String text = thinkingChunk.text();
                    if (thinkCount.incrementAndGet() == 1) {
                        text = Constants.THINK_PREFIX_TAG + text;
                    }
                    this.emitFluxEvent(Constants.SSE_EVENT_MSG, text, aiAgent.getId(), aiAgent.getName(), fluxSink);
                }).onPartialResponse(chunk -> {
                    if (thinkCount.intValue() > 0 && !thinkFinish.get()) {
                        chunk = Constants.THINK_SUFFIX_TAG + chunk;
                        thinkFinish.set(true);
                    }
                    this.emitFluxEvent(Constants.SSE_EVENT_MSG, chunk, aiAgent.getId(), aiAgent.getName(), fluxSink);
                }).onCompleteResponse(resp -> {
                    AgentRoundOut out = outRef.get();
                    out.setContent(resp.aiMessage().text());
                    sink.success(out);
                })
                .onToolExecuted(toolExecuted -> {
                    ToolExecutionRequest req = toolExecuted.request();
                    String result = toolExecuted.result();
                    log.info("调用工具完成{}, result={}", req.name(), result);
                    Map<String, Object> value = MapUtil.<String, Object>builder()
                            .put("toolName", req.name())
                            .put("toolInvokeResult", result).build();
                    this.emitFluxEvent(Constants.SSE_EVENT_TOOL_CALLED, value, aiAgent.getId(), aiAgent.getName(), fluxSink);

                    if (Constants.AGENT_SELECTOR_TOOL.equals(req.name()) && StringUtils.isNotBlank(result)) {
                        try {
                            AgentSpec agentSpec = objectMapper.readValue(result, new TypeReference<>() {
                            });
                            // 仅标记跳转信息，不立刻执行子Agent
                            AgentRoundOut out = outRef.get();
                            out.setNeedNext(true);
                            out.setNextAgentSpec(agentSpec);
                            // 你后续可以把transferRequest改为本轮输出resp文本传给子Agent
                            out.setTransferRequest(buildNextRequest(request, out.getContent()));
                        } catch (Exception e) {
                            log.error("解析子AgentSpec异常", e);
                            emitFluxEvent(Constants.SSE_EVENT_MSG, "子智能体配置解析异常：" + e.getMessage(), aiAgent.getId(), aiAgent.getName(), fluxSink);
                        }
                    }
                    List<Map<String, Object>> toolCalls = toolCallList.get() == null ? new ArrayList<>() : toolCallList.get();
                    toolCalls.add(value);
                    toolCallList.set(toolCalls);
                }).beforeToolExecution(beforeToolExecution -> {
                    ToolExecutionRequest req = beforeToolExecution.request();
                    log.info("调用工具{}", req.name());
                    Map<String, Object> value = MapUtil.<String, Object>builder().put("toolName", req.name()).build();
                    this.emitFluxEvent(Constants.SSE_EVENT_TOOL_CALL, value, aiAgent.getId(), aiAgent.getName(), fluxSink);
                }).onRetrieved(contents -> {
                    log.info("知识库查询完成，共{}条", contents.size());
                    retrieveList.set(contents.stream().map(content -> {
                        String text = content.textSegment().text();
                        Long kbId = content.textSegment().metadata().getLong("kb_id");
                        String fileName = content.textSegment().metadata().getString("file_name");
                        return MapUtil.<String, Object>builder()
                                .put("kbId", kbId)
                                .put("fileName", fileName)
                                .put("text", text)
                                .build();
                    }).toList());
                    if (CollUtil.isNotEmpty(retrieveList.get())) {
                        this.emitFluxEvent(Constants.SSE_EVENT_RETRIEVED, retrieveList.get(), aiAgent.getId(), aiAgent.getName(), fluxSink);
                    }
                })
                .onError(error -> {
                    fluxSink.error(error);
                    sink.error(error);
                })
                .start());
    }

    protected String buildNextRequest(String request, String content) {
        return "Last round result: " + content + "\n" + "User say: " + request;
    }

    /**
     * 根据AgentSpec切换上下文，准备下一轮执行
     */
    @Override
    public Mono<AgentRoundOut> nextRound(AgentSpec agentSpec, String sessionId, String clientId, String transferRequest, FluxSink<String> fluxSink) {
        this.currentNodeId = agentSpec.getNodeId();
        this.currentAgent = aiAgentService.selectAiAgentWithToolAndSkillById(agentSpec.getAgentId());
        this.currentAssistant = this.buildAIAssistant(this.currentAgent, sessionId, true);
        return invoke(sessionId, clientId, transferRequest, fluxSink, currentAgent);
    }

    @Override
    public Flux<String> chat(AgentFlow agentFlow, String prompt, String clientId, String sessionId) {
        this.init(sessionId, agentFlow);
        return Flux.create(fluxSink -> {
            log.info("AgentFlow chat start, sessionId={}", sessionId);
            // 第一轮 rootAgent
            Mono<AgentRoundOut> firstRound = invoke(sessionId, clientId, prompt, fluxSink, rootAgent);
            AtomicInteger roundCount = new AtomicInteger(0);
            // expand 递归执行多轮
            firstRound.expand(out -> {
                        log.info("expand check: needNext={}, nextAgentSpec={}", out.isNeedNext(), out.getNextAgentSpec());
                        if (roundCount.incrementAndGet() > 10) {
                            log.warn("Max rounds reached, stop expand");
                            return Mono.empty();
                        }
                        if (out.isNeedNext() && out.getNextAgentSpec() != null) {
                            return nextRound(out.getNextAgentSpec(), sessionId, clientId, out.getTransferRequest(), fluxSink);
                        } else {
                            // 无需继续，返回 empty，终止 expand
                            return Mono.empty();
                        }
                    })
                    // 所有轮次全部执行完毕，关闭SSE
                    .doFinally(signalType -> {
                        log.info("对话链路全部结束，信号类型:{}，关闭SSE", signalType);
                        fluxSink.complete();
                    })
                    .subscribe(
                            null, // onNext 不需要，因为 emitFluxEvent 已经推送
                            err -> {
                                log.error("AgentFlow error", err);
                                emitFluxEvent(Constants.SSE_EVENT_MSG, "对话异常：" + err.getMessage(),
                                        rootAgent.getId(), rootAgent.getName(), fluxSink);
                                fluxSink.error(err);
                            }
                    );
        });
    }


    @Override
    public String attachSubAgentPrompt(String prompt) {
        TreeNode tree = parser.findTreeNodeById(this.treeNode, this.currentNodeId);
        List<TreeNode> children = tree.getChildren();
        if (CollUtil.isNotEmpty(children)) {
            StringBuilder promptBuilder = new StringBuilder();
            for (TreeNode child : children) {
                AgentNode node = child.getNode();
                promptBuilder.append("- agentId: ").append(node.getAgentId()).append("\n")
                        .append(" agentName: ").append(node.getAgentName()).append("\n")
                        .append(" description: ").append(node.getDescription()).append("\n")
                        .append(" nodeId: ").append(node.getNodeId()).append("\n\n");
            }
            prompt += String.format(Constants.DEFAULT_SUB_AGENT_ATTACH_PROMPT, promptBuilder);
        }
        return prompt;
    }

    @Override
    public List<Map<String, String>> listClientSession(String clientId, Long agentId) {
        String value = sysConfigService.selectConfigByKey("ai.agent.sessionNum");
        Integer limit = null;
        if (StringUtils.isNotEmpty(value)) {
            limit = Integer.valueOf(value);
        }
        List<Map<String, String>> res = chatFlowMessageService.selectSessionList(clientId, agentId, limit);
        res.forEach(x -> {
            String title = x.get("title");
            if (title.length() > 5) {
                title = title.substring(0, 5) + "...";
            }
            x.put("title", title);
        });
        return res;
    }

    @Override
    public List<ChatFlowMessage> listAgentFlowChatMessageBySessionIdOrderDesc(String sessionId, Long agentId) {
        return chatFlowMessageService.selectAgentChatMessageBySessionIdOrderDesc(sessionId, agentId);
    }

    @Override
    public void deleteSession(String sessionId) {
        chatFlowMessageService.deleteBySessionId(sessionId);
    }

    protected void emitFluxEvent(String event, Object value, Long agentId, String agentName, FluxSink<String> fluxSink) {
        Map<String, Object> msg = MapUtil.<String, Object>builder()
                .put(event, value)
                .put("agentId", agentId)
                .put("agentName", agentName)
                .build();
        try {
            fluxSink.next(objectMapper.writeValueAsString(msg));
        } catch (JsonProcessingException ignored) {
        }
    }
}