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
import com.ruoyi.ai.domain.Attachment;
import com.ruoyi.ai.domain.ChatFlowMessage;
import com.ruoyi.ai.dto.FlowRoundContext;
import com.ruoyi.ai.service.*;
import com.ruoyi.ai.util.Base64DataUriUtil;
import com.ruoyi.ai.util.Constants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.system.service.ISysConfigService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.*;
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
                                    PgVectorEmbeddingStore pgVectorEmbeddingStore,
                                    IAttachmentService attachmentService) {
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
                pgVectorEmbeddingStore,
                attachmentService);
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
     * 单轮 Agent 执行。
     * <p>
     * 工具回调 {@link Constants#AGENT_SELECTOR_TOOL agent_selector} 时：直接按
     * {@code List<AgentSpec>} 解析（多 Agent 路径），把结果有序写入
     * {@link AgentRoundOut#setNextAgentSpecs(List)}。
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
                        .nextAgentSpecs(new ArrayList<>())
                        .content("")
                        .build()
        );

        List<Content> messages = new ArrayList<>();
        messages.add(TextContent.from(finalPromptTemplate));
        if (CollUtil.isNotEmpty(ctx.getAttachments())) {
            for (Attachment attachment : ctx.getAttachments()) {
                if (FileUtils.isImageBySuffix(attachment.getFileName())) {
                    Base64DataUriUtil.Base64ParseResult parseResult = Base64DataUriUtil.parse(attachment.getFileContent());
                    messages.add(ImageContent.from(parseResult.getPureBase64(), parseResult.getMimeType()));
                } else {
                    TextContent attachContent = TextContent.from(String.format(Constants.DEFAULT_ATTACHMENT_FILE_PROMPT, attachment.getFileName(), attachment.getFileContent()));
                    messages.add(attachContent);
                }
            }
        }

        TokenStream tokenStream = assistant.chatStream(messages);
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
                    // 当本轮选择了一批后续 Agent 时，把本轮产出+用户原始请求打包进 transferRequest，
                    // 由 nextRoundBatch 继续向后累积，保持原始用户请求始终可见。
                    if (out.hasNextBatch()) {
                        out.setTransferRequest(buildNextRequest(request, out.getContent()));
                    }
                    sink.success(out);

                    if (aiAgent.getShowReasoning()) {
                        String content = this.getAIResponse(resp);
                        ChatFlowMessage assistantMessage = this.saveChatFlowMessage(content, clientId, sessionId, aiAgent.getId(), aiAgent.getName(),
                                this.agentFlow.getId(), ChatMessageType.AI, null, ctx.getTraceId());
                        if (CollUtil.isNotEmpty(retrieveList.get())) {
                            for (Map<String, Object> item : retrieveList.get()) {
                                this.saveKBRetrieved(aiAgent, sessionId, clientId, assistantMessage.getId(),
                                        String.valueOf(item.get("fileName") == null ? "" : item.get("fileName")),
                                        String.valueOf(item.get("text") == null ? "" : item.get("text")),
                                        String.valueOf(item.get("url") == null ? "" : item.get("url")));
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
                        rootMessages.add(AiMessage.aiMessage(String.format(Constants.DEFAULT_ROOT_AGENT_SYNC_CONTEXT_PROMPT, aiAgent.getName(), content)));
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
                        AgentRoundOut out = outRef.get();
                        // agent_selector 现在固定返回 JSON 数组（List<AgentSpec>）。
                        try {
                            List<AgentSpec> specs = objectMapper.readValue(
                                    result, new TypeReference<>() {
                                    });
                            handleBatchAgentSpecsResult(out, specs);
                        } catch (Exception ex) {
                            log.error("解析 agent_selector 结果异常", ex);
                            emitFluxEvent(Constants.SSE_EVENT_MSG,
                                    "子智能体配置解析异常：" + ex.getMessage(),
                                    aiAgent.getId(), aiAgent.getName(), safeEmitter);
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

    /**
     * 处理 agent_selector 工具返回的 JSON 数组结果。
     * <p>
     * 校验语义：
     * <ul>
     *     <li>{@code specs} 为 null 或空：抛 {@link ServiceException}，与"校验空列表"需求一致。</li>
     *     <li>任一元素为 null 或缺少 agentId：抛异常，fail-fast。</li>
     *     <li>长度超过 {@link Constants#MAX_FLOW_BATCH_SIZE}：抛异常，fail-fast。</li>
     *     <li>校验通过后保留入参顺序写入 {@link AgentRoundOut#setNextAgentSpecs(List)}，
     *         调度层将按此顺序串行执行。</li>
     * </ul>
     */
    private void handleBatchAgentSpecsResult(AgentRoundOut out, List<AgentSpec> specs) {
        if (specs == null || specs.isEmpty()) {
            throw new ServiceException("agent_selector 返回为空，请重新选择子Agent");
        }
        if (specs.size() > Constants.MAX_FLOW_BATCH_SIZE) {
            throw new ServiceException(
                    "agent_selector 返回数量 " + specs.size() + " 超过最大批次 " + Constants.MAX_FLOW_BATCH_SIZE);
        }
        for (int i = 0; i < specs.size(); i++) {
            AgentSpec spec = specs.get(i);
            if (spec == null || spec.getAgentId() == null) {
                throw new ServiceException("agent_selector 结果中第 " + i + " 个 AgentSpec 缺少 agentId");
            }
        }
        // 顺序就是执行顺序，不做洗牌/合并
        out.setNextAgentSpecs(new ArrayList<>(specs));
        out.setNeedNext(true);
    }

    protected String buildNextRequest(String request, String content) {
        return "Parent Agent answer: " + content + "\n" + "User say: " + request;
    }

    /**
     * 多 Agent 串行调度入口。
     * <p>
     * 关键实现细节：使用 Reactor {@code concatMap}，严格要求“上一个 Agent 的 {@code invoke}
     * 完整 onComplete 之后，下一个 Agent 的 {@code invoke} 才被订阅”。
     * 不使用 {@code flatMap} / {@code flatMapSequential}，因为它们都允许预取/并发订阅，
     * 会导致多个 Agent 的 {@code TokenStream} 同时启动，造成“他们一起执行”的表象。
     *
     * <p>
     * 上下文累积：使用一个 {@link AtomicReference}{@code <StringBuilder>} 在串行调度中
     * 把每个 Agent 的输出追加到累积 prompt 中，作为下一轮的 {@code transferRequest}，
     * 起始 {@code transferRequest} 中已包含原始用户请求，保证最后执行 Agent 也能看到全部
     * 前序 Agent 的输出 + 用户原始输入。
     */
    @Override
    public Mono<AgentRoundOut> nextRoundBatch(List<AgentSpec> specs, String sessionId, String clientId,
                                              String transferRequest, FlowRoundContext ctx) {
        // 1. 入参校验 — 与 prompt 提示的上限保持一致，fail-fast
        if (specs == null || specs.isEmpty()) {
            throw new ServiceException("agent_selector 批次为空，禁止执行");
        }
        if (specs.size() > Constants.MAX_FLOW_BATCH_SIZE) {
            throw new ServiceException(
                    "agent_selector 批次 " + specs.size() + " 超过最大上限 " + Constants.MAX_FLOW_BATCH_SIZE);
        }
        for (int i = 0; i < specs.size(); i++) {
            AgentSpec spec = specs.get(i);
            if (spec == null || spec.getAgentId() == null) {
                throw new ServiceException("agent_selector 批次第 " + i + " 个 agentId 为空");
            }
        }
        log.info("nextRoundBatch start, size={}, sessionId={}, thread={}",
                specs.size(), sessionId, Thread.currentThread().getName());

        final StringBuilder running = new StringBuilder(transferRequest == null ? "" : transferRequest);
        AtomicReference<AgentRoundOut> lastOutRef = new AtomicReference<>();

        // 2. 串行编排 — 使用 concatMap 强制后一个 Agent 在前一个 Agent onComplete 之后才订阅
        return Flux.fromIterable(specs)
                .concatMap(spec -> {
                    String requestForThisAgent = running.toString();
                    log.info("nextRoundBatch -> start agentName={}, agentId={}, thread={}",
                            spec.getAgentName(), spec.getAgentId(), Thread.currentThread().getName());
                    return runOneInBatch(spec, sessionId, clientId, requestForThisAgent, ctx)
                            .doOnNext(out -> {
                                lastOutRef.set(out);
                                // 严格按顺序累积：上一个 Agent 完成后才追加到 running，下一轮才能看到
                                running.append("\n\n")
                                        .append("Agent[").append(spec.getAgentName()).append("] answer: ")
                                        .append(out.getContent() == null ? "" : out.getContent());
                                log.info("nextRoundBatch <- finish agentName={}, agentId={}, thread={}",
                                        spec.getAgentName(), spec.getAgentId(), Thread.currentThread().getName());
                            });
                })
                .then(Mono.fromCallable(() -> {
                    AgentRoundOut out = lastOutRef.get();
                    if (out == null) {
                        return AgentRoundOut.builder().content("").needNext(false).build();
                    }
                    return out;
                }));
    }

    /**
     * 批次中的单个 Agent 执行：构建自己的 FlowRoundContext、调用 invoke。
     * <p>
     * 该方法必须只返回 {@code Mono<AgentRoundOut>}，由 {@link #nextRoundBatch} 通过
     * {@code concatMap} 保证后一个 Agent 的本调用必须等待前一个 Agent 完整 onComplete 后才被订阅。
     */
    private Mono<AgentRoundOut> runOneInBatch(AgentSpec spec, String sessionId, String clientId,
                                              String requestForThisAgent, FlowRoundContext parentCtx) {
        // 构造当前 Agent 的上下文：复用 SSE sink
        FlowRoundContext oneCtx = new FlowRoundContext();
        oneCtx.setCurrentNodeId(spec.getNodeId());
        AIAgent subAgent = aiAgentService.selectAiAgentWithToolAndSkillById(spec.getAgentId());
        subAgent.setShowReasoning(spec.getShowReasoning());
        subAgent.setFlowMemory(spec.getFlowMemory());
        subAgent.setAgentNodeId(spec.getNodeId());
        oneCtx.setCurrentAgent(subAgent);
        AIAssistant subAssistant = buildAIAssistant(subAgent, sessionId, true, subAgent.getFlowMemory());
        oneCtx.setCurrentAssistant(subAssistant);
        oneCtx.setRawSink(parentCtx.getRawSink());
        oneCtx.setSafeEmitter(parentCtx.getSafeEmitter());
        oneCtx.setSinkClosed(parentCtx.getSinkClosed());
        oneCtx.setAttachments(parentCtx.getAttachments());
        oneCtx.setTraceId(parentCtx.getTraceId());

        return invoke(sessionId, clientId, requestForThisAgent, oneCtx);
    }

    @Override
    public Flux<String> chat(AgentFlow agentFlow, String prompt, String clientId, String sessionId, List<Long> attachmentIds) {
        final String traceId = IdUtils.fastSimpleUUID();
        this.init(sessionId, agentFlow);
        return Flux.create(fluxOriginSink -> {
            // ========== 全部并发对象由父类统一创建 ==========
            SseEmitterContext emitterCtx = createSseEmitterContext(fluxOriginSink);
            final FluxSink<String> rawSink = emitterCtx.rawSink;
            AtomicBoolean sinkClosed = emitterCtx.sinkClosed;
            BiConsumer<String, String> safeEmitter = emitterCtx.safeEmitter;

            log.info("AgentFlow chat start, sessionId={}, traceId={}", sessionId, traceId);
            ChatFlowMessage chatFlowMessage = saveChatFlowMessage(prompt, clientId, sessionId, rootAgent.getId(), rootAgent.getName(), agentFlow.getId(), ChatMessageType.USER, attachmentIds, traceId);

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
            rootCtx.setAttachments(chatFlowMessage.getAttachments());
            rootCtx.setTraceId(traceId);

            Mono<AgentRoundOut> firstRound = invoke(sessionId, clientId, prompt, rootCtx);

            firstRound.expand(out -> {
                        log.info("expand check: needNext={}, nextBatchSize={}, totalRound={}",
                                out.isNeedNext(),
                                out.getNextAgentSpecs() == null ? 0 : out.getNextAgentSpecs().size(),
                                roundCount.get());
                        if (sinkClosed.get()) {
                            return Mono.empty();
                        }
                        // 总轮次上限（含 root 与所有子 Agent 轮）
                        if (roundCount.incrementAndGet() > Constants.MAX_FLOW_ROUNDS) {
                            log.warn("Max rounds reached, stop expand");
                            return Mono.empty();
                        }

                        // 仅走多 Agent 批次路径，无单 Agent 兼容分支
                        if (out.hasNextBatch()) {
                            List<AgentSpec> batch = out.getNextAgentSpecs();
                            return nextRoundBatch(batch, sessionId, clientId, out.getTransferRequest(), rootCtx);
                        }
                        return Mono.empty();
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
            // 多 Agent 提示词：把 agentIds 数组顺序=执行顺序写入，让模型明确知道必须传数组
            prompt += String.format(Constants.DEFAULT_SUB_AGENT_BATCH_ATTACH_PROMPT,
                    promptBuilder, Constants.MAX_FLOW_BATCH_SIZE);
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
                                                  Long agentFlowId, ChatMessageType chatMessageType, List<Long> attachmentIds, String traceId) {
        ChatFlowMessage chatFlowMessage = new ChatFlowMessage();
        chatFlowMessage.setAgentId(agentId);
        chatFlowMessage.setContent(content);
        chatFlowMessage.setTraceId(traceId);
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
        if (CollUtil.isNotEmpty(attachmentIds)) {
            List<Attachment> attachments = attachmentService.selectAttachmentByIds(attachmentIds.toArray(Long[]::new));
            attachments.forEach(attachment -> {
                attachment.setMessageFlowId(chatFlowMessage.getId());
                attachmentService.updateAttachment(attachment);
            });
            chatFlowMessage.setAttachments(attachments);
        }
        return chatFlowMessage;
    }
}
