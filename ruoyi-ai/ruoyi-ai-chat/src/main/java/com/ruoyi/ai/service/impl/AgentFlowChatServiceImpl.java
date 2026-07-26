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
import com.ruoyi.ai.dto.FlowRoundContext;
import com.ruoyi.ai.service.*;
import com.ruoyi.ai.util.Constants;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.system.service.ISysConfigService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

@Slf4j
@Component("agentFlowChatService")
@Scope("prototype")
public class AgentFlowChatServiceImpl extends AIBaseChatServiceImpl implements AgentFlowService {

    // ===================== init 之后只读，线程安全，保留成员 =====================
    private TreeNode treeNode;
    private AgentFlow agentFlow;
    private AIAgent rootAgent;
    // 节点ID -> TreeNode 缓存，避免重复递归遍历树
    private Map<String, TreeNode> nodeIdMap;

    // ===================== 【已移除】动态变量全部迁移至 FlowRoundContext =====================
    // private AIAssistant currentAssistant;
    // private AIAgent currentAgent;
    // private String currentNodeId;

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

        // 构建节点缓存Map
        this.nodeIdMap = new HashMap<>();
        buildNodeCache(this.treeNode, nodeIdMap);

        Integer rootAgentId = treeNode.getNode().getAgentId();
        this.rootAgent = this.aiAgentService.selectAiAgentWithToolAndSkillById(Long.valueOf(rootAgentId));
        this.rootAgent.setAgentNodeId(treeNode.getNode().getNodeId());
        this.rootAgent.setShowReasoning(treeNode.getNode().getShowReasoning());
        this.rootAgent.setFlowMemory(treeNode.getNode().getFlowMemory());
        this.rootAgent.setIsRootAgent(Boolean.TRUE);
    }

    /**
     * 递归构建节点ID缓存
     */
    private void buildNodeCache(TreeNode node, Map<String, TreeNode> map) {
        map.put(node.getNode().getNodeId(), node);
        if (CollUtil.isNotEmpty(node.getChildren())) {
            for (TreeNode child : node.getChildren()) {
                buildNodeCache(child, map);
            }
        }
    }

    /**
     * 单轮Agent执行
     */
    @Override
    public Mono<AgentRoundOut> invoke(String sessionId, String clientId, String request, FlowRoundContext ctx) {
        AIAgent aiAgent = ctx.getCurrentAgent();
        AIAssistant assistant = ctx.getCurrentAssistant();
        String currentNodeId = ctx.getCurrentNodeId();
        BiConsumer<String, String> safeEmitter = ctx.getSafeEmitter();
        AtomicBoolean sinkClosed = ctx.getSinkClosed();

        String invokeId = IdUtils.fastSimpleUUID();
        String promptTemplate = aiAgent.getPromptTemplate();
        if (promptTemplate.contains(Constants.USER_MSG_TEMPLATE)) {
            promptTemplate = promptTemplate.replace(Constants.USER_MSG_TEMPLATE, request);
        } else {
            promptTemplate = promptTemplate + request;
        }
        if (promptTemplate.contains(Constants.KNOWLEDGE_BASE_TEMPLATE)) {
            promptTemplate = promptTemplate.replace(Constants.KNOWLEDGE_BASE_TEMPLATE, "");
        }
        String finalPromptTemplate = this.attachSubAgentPrompt(promptTemplate, currentNodeId) + "\n\n当前invokeId=" + invokeId;

        AtomicReference<List<Map<String, Object>>> retrieveList = new AtomicReference<>();
        AtomicReference<List<Map<String, Object>>> toolCallList = new AtomicReference<>();

        AtomicReference<AgentRoundOut> outRef = new AtomicReference<>(
                AgentRoundOut.builder()
                        .needNext(false)
                        .transferRequest(request)
                        .nextAgentSpec(null)
                        .content("")
                        .build()
        );

        TokenStream tokenStream = assistant.chatStream(finalPromptTemplate);
        AtomicInteger thinkCount = new AtomicInteger(0);
        AtomicBoolean thinkFinish = new AtomicBoolean(false);

        return Mono.create(sink -> tokenStream
                .onPartialThinking(thinkingChunk -> {
                    if (aiAgent.getShowReasoning()) {
                        String text = thinkingChunk.text();
                        if (thinkCount.incrementAndGet() == 1) {
                            text = Constants.THINK_PREFIX_TAG + text;
                        }
                        emitFluxEvent(Constants.SSE_EVENT_MSG, text, aiAgent.getId(), aiAgent.getName(), safeEmitter);
                    }
                })
                .onPartialResponse(chunk -> {
                    if (aiAgent.getShowReasoning()) {
                        if (thinkCount.intValue() > 0 && !thinkFinish.get()) {
                            chunk = Constants.THINK_SUFFIX_TAG + chunk;
                            thinkFinish.set(true);
                        }
                        emitFluxEvent(Constants.SSE_EVENT_MSG, chunk, aiAgent.getId(), aiAgent.getName(), safeEmitter);
                    }
                })
                .onCompleteResponse(resp -> {
                    AgentRoundOut out = outRef.get();
                    out.setContent(resp.aiMessage().text());
                    if (out.isNeedNext() && out.getNextAgentSpec() != null) {
                        out.setTransferRequest(buildNextRequest(request, out.getContent()));
                    }
                    sink.success(out);

                    if (aiAgent.getShowReasoning()) {
                        String content = this.getAIResponse(resp);
                        ChatFlowMessage assistantMessage = this.saveChatFlowMessage(content, clientId, sessionId, aiAgent.getId(), aiAgent.getName(),
                                this.agentFlow.getId(), ChatMessageType.AI);
                        if (CollUtil.isNotEmpty(retrieveList.get())) {
                            for (Map<String, Object> item : retrieveList.get()) {
                                this.saveKBRetrieved(aiAgent, sessionId, clientId, assistantMessage.getId(),
                                        String.valueOf(item.get("fileName")), String.valueOf(item.get("text")));
                            }
                        }
                        if (CollUtil.isNotEmpty(toolCallList.get())) {
                            for (Map<String, Object> item : toolCallList.get()) {
                                this.saveToolCallRecord(aiAgent, sessionId, clientId, assistantMessage.getId(),
                                        String.valueOf(item.get("toolName")), String.valueOf(item.get("toolInvokeResult")));
                            }
                        }
                    }
                    //非root agent需要同步AI message到root memory
                    if (!aiAgent.getIsRootAgent()) {
                        String content = this.getAIResponse(resp);
                        ChatMemoryStore memoryStore = aiConfig.getAgent().getMemoryStrategy() == AiConfig.Agent.MemoryStrategy.REDIS ?
                                redisMemoryChatMemoryStore : inMemoryChatMemoryStore;
                        List<ChatMessage> rootMessages = memoryStore.getMessages(sessionId);
                        rootMessages.add(AiMessage.aiMessage(content));
                        memoryStore.updateMessages(sessionId, rootMessages);
                    }
                })
                .onToolExecuted(toolExecuted -> {
                    ToolExecutionRequest req = toolExecuted.request();
                    String result = toolExecuted.result();
                    log.info("调用工具完成{}, result={}", req.name(), result);
                    if (aiAgent.getShowReasoning()) {
                        Map<String, Object> value = MapUtil.<String, Object>builder()
                                .put("toolName", req.name())
                                .put("toolInvokeResult", result).build();
                        emitFluxEvent(Constants.SSE_EVENT_TOOL_CALLED, value, aiAgent.getId(), aiAgent.getName(), safeEmitter);
                        List<Map<String, Object>> toolCalls = toolCallList.get() == null ? new ArrayList<>() : toolCallList.get();
                        toolCalls.add(value);
                        toolCallList.set(toolCalls);
                    }

                    if (Constants.AGENT_SELECTOR_TOOL.equals(req.name()) && StringUtils.isNotBlank(result)) {
                        try {
                            AgentSpec agentSpec = objectMapper.readValue(result, new TypeReference<>() {
                            });
                            AgentRoundOut out = outRef.get();
                            out.setNeedNext(true);
                            out.setNextAgentSpec(agentSpec);
                        } catch (Exception e) {
                            log.error("解析子AgentSpec异常", e);
                            emitFluxEvent(Constants.SSE_EVENT_MSG, "子智能体配置解析异常：" + e.getMessage(), aiAgent.getId(), aiAgent.getName(), safeEmitter);
                        }
                    }
                })
                .beforeToolExecution(beforeToolExecution -> {
                    ToolExecutionRequest req = beforeToolExecution.request();
                    log.info("调用工具{}", req.name());
                    if (aiAgent.getShowReasoning()) {
                        Map<String, Object> value = MapUtil.<String, Object>builder().put("toolName", req.name()).build();
                        emitFluxEvent(Constants.SSE_EVENT_TOOL_CALL, value, aiAgent.getId(), aiAgent.getName(), safeEmitter);
                    }
                })
                .onRetrieved(contents -> {
                    log.info("知识库查询完成，共{}条", contents.size());
                    if (aiAgent.getShowReasoning()) {
                        handleRetrieveRecord(retrieveList, contents);
                        if (CollUtil.isNotEmpty(retrieveList.get())) {
                            emitFluxEvent(Constants.SSE_EVENT_RETRIEVED, retrieveList.get(), aiAgent.getId(), aiAgent.getName(), safeEmitter);
                        }
                    }
                })
                .onError(error -> {
                    if (!sinkClosed.get()) {
                        sink.error(error);
                    }
                })
                .start());
    }

    protected String buildNextRequest(String request, String content) {
        return "Parent Agent answer: " + content + "\n" + "User say: " + request;
    }

    @Override
    public Mono<AgentRoundOut> nextRound(AgentSpec agentSpec, String sessionId, String clientId,
                                         String transferRequest, FlowRoundContext oldCtx) {
        AIAgent subAgent = aiAgentService.selectAiAgentWithToolAndSkillById(agentSpec.getAgentId());
        subAgent.setShowReasoning(agentSpec.getShowReasoning());
        subAgent.setFlowMemory(agentSpec.getFlowMemory());
        subAgent.setAgentNodeId(agentSpec.getNodeId());

        AIAssistant subAssistant = buildAIAssistant(subAgent, sessionId, true, subAgent.getFlowMemory());

        FlowRoundContext newCtx = new FlowRoundContext();
        newCtx.setCurrentNodeId(agentSpec.getNodeId());
        newCtx.setCurrentAgent(subAgent);
        newCtx.setCurrentAssistant(subAssistant);
        newCtx.setRawSink(oldCtx.getRawSink());
        newCtx.setSafeEmitter(oldCtx.getSafeEmitter());
        newCtx.setSinkClosed(oldCtx.getSinkClosed());

        return invoke(sessionId, clientId, transferRequest, newCtx);
    }

    @Override
    public Flux<String> chat(AgentFlow agentFlow, String prompt, String clientId, String sessionId) {
        this.init(sessionId, agentFlow);
        return Flux.create(fluxOriginSink -> {
            // ========== 【改动】全部并发对象由父类统一创建，不再手写 ==========
            SseEmitterContext emitterCtx = createSseEmitterContext(fluxOriginSink);
            final FluxSink<String> rawSink = emitterCtx.rawSink;
            AtomicBoolean sinkClosed = emitterCtx.sinkClosed;
            BiConsumer<String, String> safeEmitter = emitterCtx.safeEmitter;

            log.info("AgentFlow chat start, sessionId={}", sessionId);
            saveChatFlowMessage(prompt, clientId, sessionId, rootAgent.getId(), rootAgent.getName(), agentFlow.getId(), ChatMessageType.USER);

            AtomicInteger roundCount = new AtomicInteger(0);

            // 构造第一轮根上下文
            String rootNodeId = treeNode.getNode().getNodeId();
            AIAssistant rootAssistant = buildAIAssistant(rootAgent, sessionId, true, true);
            FlowRoundContext rootCtx = new FlowRoundContext();
            rootCtx.setCurrentNodeId(rootNodeId);
            rootCtx.setCurrentAgent(rootAgent);
            rootCtx.setCurrentAssistant(rootAssistant);
            rootCtx.setRawSink(rawSink);
            rootCtx.setSafeEmitter(safeEmitter);
            rootCtx.setSinkClosed(sinkClosed);

            Mono<AgentRoundOut> firstRound = invoke(sessionId, clientId, prompt, rootCtx);

            firstRound.expand(out -> {
                        log.info("expand check: needNext={}, nextAgentSpec={}", out.isNeedNext(), out.getNextAgentSpec());
                        if (sinkClosed.get()) {
                            return Mono.empty();
                        }
                        if (roundCount.incrementAndGet() > 10) {
                            log.warn("Max rounds reached, stop expand");
                            return Mono.empty();
                        }
                        if (out.isNeedNext() && out.getNextAgentSpec() != null) {
                            return nextRound(out.getNextAgentSpec(), sessionId, clientId, out.getTransferRequest(), rootCtx);
                        } else {
                            return Mono.empty();
                        }
                    })
                    .doFinally(signalType -> {
                        log.info("对话链路全部结束，信号类型:{}，关闭SSE", signalType);
                        if (emitterCtx.markClosed()) {
                            rawSink.complete();
                        }
                    })
                    .subscribe(
                            null,
                            err -> {
                                log.error("AgentFlow error", err);
                                if (emitterCtx.markClosed()) {
                                    Map<String, Object> errMsg = MapUtil.<String, Object>builder()
                                            .put(Constants.SSE_EVENT_MSG, "对话异常：" + err.getMessage())
                                            .put("agentId", rootAgent.getId())
                                            .put("agentName", rootAgent.getName())
                                            .build();
                                    try {
                                        safeEmitter.accept(objectMapper.writeValueAsString(errMsg), Constants.SSE_EVENT_MSG);
                                    } catch (JsonProcessingException e) {
                                        log.error("异常消息序列化失败", e);
                                    }
                                    rawSink.error(err);
                                    rawSink.complete();
                                }
                            }
                    );
        });
    }

    @Override
    public String attachSubAgentPrompt(String prompt, String currentNodeId) {
        TreeNode tree = nodeIdMap.get(currentNodeId);
        if (tree == null) {
            return prompt;
        }
        List<TreeNode> children = tree.getChildren();
        if (CollUtil.isNotEmpty(children)) {
            StringBuilder promptBuilder = new StringBuilder();
            for (TreeNode child : children) {
                AgentNode node = child.getNode();
                promptBuilder.append("- agentId: ").append(node.getAgentId()).append("\n")
                        .append(" agentName: ").append(node.getAgentName()).append("\n")
                        .append(" description: ").append(node.getDescription()).append("\n")
                        .append(" nodeId: ").append(node.getNodeId()).append("\n\n")
                        .append(" showReasoning: ").append(node.getShowReasoning()).append("\n\n")
                        .append(" flowMemory: ").append(node.getFlowMemory()).append("\n\n");
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
            if (title != null && title.length() > 5) {
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

    protected ChatFlowMessage saveChatFlowMessage(String content, String clientId, String sessionId, Long agentId, String agentName,
                                                  Long agentFlowId, ChatMessageType chatMessageType) {
        ChatFlowMessage chatFlowMessage = new ChatFlowMessage();
        chatFlowMessage.setAgentId(agentId);
        chatFlowMessage.setContent(content);
        if (chatMessageType == ChatMessageType.AI) {
            chatFlowMessage.setRole("assistant");
        } else if (chatMessageType == ChatMessageType.USER) {
            chatFlowMessage.setRole("user");
        } else if (chatMessageType == ChatMessageType.SYSTEM) {
            chatFlowMessage.setRole("system");
        } else if (chatMessageType == ChatMessageType.TOOL_EXECUTION_RESULT) {
            chatFlowMessage.setRole("tool_execution_result");
        } else {
            chatFlowMessage.setRole(chatMessageType.name());
        }
        chatFlowMessage.setClientId(clientId);
        chatFlowMessage.setSessionId(sessionId);
        chatFlowMessage.setAgentFlowId(agentFlowId);
        chatFlowMessage.setAgentName(agentName);
        chatFlowMessage.setCreateBy("system");
        chatFlowMessageService.insertChatMessage(chatFlowMessage);
        return chatFlowMessage;
    }
}
