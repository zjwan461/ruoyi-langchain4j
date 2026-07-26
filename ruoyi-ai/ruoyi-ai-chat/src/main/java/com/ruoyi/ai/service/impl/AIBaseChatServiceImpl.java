package com.ruoyi.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.component.memory.RedisMemoryChatMemoryStore;
import com.ruoyi.ai.config.AiConfig;
import com.ruoyi.ai.domain.*;
import com.ruoyi.ai.enums.ModelType;
import com.ruoyi.ai.service.*;
import com.ruoyi.ai.tool.JToolProvider;
import com.ruoyi.ai.util.Constants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.system.service.ISysConfigService;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.filter.comparison.IsIn;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

public abstract class AIBaseChatServiceImpl implements IAIChatService {

    protected final IChatMessageService chatMessageService;

    protected final RedisMemoryChatMemoryStore redisMemoryChatMemoryStore;

    protected final InMemoryChatMemoryStore inMemoryChatMemoryStore;

    protected final AiConfig aiConfig;

    protected final ObjectMapper objectMapper;

    protected final RedisTemplate<Object, Object> redisTemplate;

    protected final RedisScript<Long> limitScript;

    protected final ISysConfigService sysConfigService;

    protected final IKnowledgeBaseService knowledgeBaseService;

    protected final IModelService modelService;

    protected final ModelBuilder modelBuilder;

    protected final IAIAgentService aiAgentService;

    protected final PgVectorEmbeddingStore pgVectorEmbeddingStore;

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    public AIBaseChatServiceImpl(IChatMessageService chatMessageService,
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
        this.chatMessageService = chatMessageService;
        this.inMemoryChatMemoryStore = inMemoryChatMemoryStore;
        this.redisMemoryChatMemoryStore = redisMemoryChatMemoryStore;
        this.aiConfig = aiConfig;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.limitScript = limitScript;
        this.sysConfigService = sysConfigService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.modelService = modelService;
        this.modelBuilder = modelBuilder;
        this.aiAgentService = aiAgentService;
        this.pgVectorEmbeddingStore = pgVectorEmbeddingStore;
    }

    @Override
    public Flux<String> chat(AIAgent aiAgent, String prompt, String clientId, String sessionId) {
        throw new ServiceException("not implemented");
    }

    @Override
    public Flux<String> chat(AgentFlow agentFlow, String prompt, String clientId, String sessionId) {
        throw new ServiceException("not implemented");
    }

    @Override
    public ChatMessage saveChatMessage(String content, String clientId, String sessionId, Long agentId,
                                       ChatMessageType chatMessageType) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setAgentId(agentId);
        chatMessage.setContent(content);
        if (chatMessageType == ChatMessageType.AI) {
            chatMessage.setRole("assistant");
        } else if (chatMessageType == ChatMessageType.USER) {
            chatMessage.setRole("user");
        } else if (chatMessageType == ChatMessageType.SYSTEM) {
            chatMessage.setRole("system");
        } else if (chatMessageType == ChatMessageType.TOOL_EXECUTION_RESULT) {
            chatMessage.setRole("tool_execution_result");
        } else {
            chatMessage.setRole(chatMessageType.name());
        }
        chatMessage.setClientId(clientId);
        chatMessage.setSessionId(sessionId);
        chatMessage.setCreateBy("system");
        chatMessageService.insertChatMessage(chatMessage);
        return chatMessage;
    }

    @Override
    public String createSession(String clientId) {
        String sessionId = IdUtils.fastSimpleUUID();
        redisTemplate.opsForValue().set(Constants.AI_CHAT_CLIENT_SESSION + clientId, sessionId);
        return sessionId;
    }

    @Override
    public boolean checkClientSession(String clientId, String sessionId) {
        return sessionId.equals(redisTemplate.opsForValue().get(Constants.AI_CHAT_CLIENT_SESSION + clientId));
    }

    @Override
    public boolean checkIfOverLmtRequest(Long agentId, Integer dayLmtPerClient, String clientId) {
        Long number = redisTemplate.execute(limitScript,
                Collections.singletonList(Constants.AI_AGENT_CHAT_LMT + agentId + ":" + clientId),
                dayLmtPerClient,
                (int) TimeUnit.DAYS.toSeconds(1));
        return number.intValue() <= dayLmtPerClient;
    }

    @Override
    public List<Map<String, String>> listClientSession(String clientId, Long agentId) {
        String value = sysConfigService.selectConfigByKey("ai.agent.sessionNum");
        Integer limit = null;
        if (StringUtils.isNotEmpty(value)) {
            limit = Integer.valueOf(value);
        }
        List<Map<String, String>> res = chatMessageService.selectSessionList(clientId, agentId, limit);
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
    public List<ChatMessage> listAgentChatMessageBySessionId(String sessionId, Long agentId) {
        return chatMessageService.selectAgentChatMessageBySessionId(sessionId, agentId);
    }

    @Override
    public List<ChatMessage> listAgentChatMessageBySessionIdOrderDesc(String sessionId, Long agentId) {
        return chatMessageService.selectAgentChatMessageBySessionIdOrderDesc(sessionId, agentId);
    }

    @Override
    public void deleteSession(String sessionId) {
        if (chatMessageService.deleteBySessionId(sessionId) > 0) {
//            chatMemories.remove(Constants.MEMORY_CACHE_KEY_PREFIX + sessionId);
            log.debug("remove session id:{}", sessionId);
        }
    }

    protected EmbeddingModel getEmbeddingModel() {
        Model query = new Model();
        query.setType(ModelType.EMBEDDING.getValue());
        List<Model> models = modelService.selectModelList(query);
        if (models == null) {
            throw new ServiceException("尚未配置embedding模型,无法进行知识库查询");
        }
        String value = sysConfigService.selectConfigByKey("ai.model.embedding");
        if (StringUtils.isBlank(value)) {
            log.warn("not configure default embedding model, will use the first one embedding model save in db");
            Model first = models.get(0);
            return modelBuilder.getEmbeddingModel(first);
        } else {
            long embId = Long.parseLong(value);
            Model model = models.stream().filter(x -> embId == x.getId()).findFirst().orElse(null);
            if (model == null) {
                throw new ServiceException("已配置的默认向量模型id未配置");
            }
            return modelBuilder.getEmbeddingModel(model);
        }

    }

    protected void emitFluxEvent(String event, Object value, FluxSink<String> fluxSink) {
        Map<String, Object> msg = MapUtil.<String, Object>builder()
                .put(event, value)
                .build();

        try {
            fluxSink.next(objectMapper.writeValueAsString(msg));
        } catch (JsonProcessingException ignored) {
        }
    }

    protected void saveToolCallRecord(AIAgent aiAgent, String sessionId, String clientId, Long messageId,
                                      String toolName, String toolInvokeResult) {
        try {
            AgentToolInvoke invoke = new AgentToolInvoke();
            invoke.setAgentId(aiAgent.getId());
            invoke.setToolName(toolName);
            invoke.setClientId(clientId);
            invoke.setSessionId(sessionId);
            invoke.setMessageId(messageId);
            invoke.setToolInvokeResult(toolInvokeResult);
            invoke.setCreateTime(new Date());
            aiAgentService.insertAgentToolInvoked(invoke);
        } catch (Exception e) {
            log.error("save tool call record error:", e);
        }
    }

    protected void saveKBRetrieved(AIAgent aiAgent, String sessionId, String clientId, Long messageId,
                                   String fileName, String text) {
        try {
            AgentKbInvoke invoke = new AgentKbInvoke();
            invoke.setAgentId(aiAgent.getId());
            invoke.setClientId(clientId);
            invoke.setSessionId(sessionId);
            invoke.setMessageId(messageId);
            invoke.setFileName(fileName);
            invoke.setText(text);
            invoke.setCreateTime(new Date());
            aiAgentService.insertAgentKBInvoked(invoke);
        } catch (Exception e) {
            log.error("save kb retrieved error:", e);
        }
    }

    protected AIAssistant buildAIAssistant(AIAgent currentAgent, String sessionId, boolean enableFlowTool, boolean shareSessionMemory) {
        Long modelId = currentAgent.getModelId();
        Model model = modelService.selectModelById(modelId);
        StreamingChatModel llm = modelBuilder.getStreamingLLM(model);
        ChatMemoryStore memoryStore = aiConfig.getAgent().getMemoryStrategy() == AiConfig.Agent.MemoryStrategy.REDIS ?
                redisMemoryChatMemoryStore : inMemoryChatMemoryStore;
        AiServices<AIAssistant> builder = AiServices.builder(AIAssistant.class);
        EmbeddingStoreContentRetriever contentRetriever = null;
        String kbIds = currentAgent.getKbIds();
        if (StringUtils.isNotBlank(kbIds)) {
            List<Long> ids = Arrays.stream(kbIds.split(",")).map(Long::valueOf).toList();
            List<KnowledgeBase> kbs = knowledgeBaseService.selectKnowledgeBaseByIds(ids);
            List<Long> kbIdList = kbs.stream().map(KnowledgeBase::getId).toList();
            contentRetriever = EmbeddingStoreContentRetriever.builder()
                    .embeddingModel(getEmbeddingModel())
                    .minScore(currentAgent.getMinScore())
                    .maxResults(currentAgent.getMaxResult())
                    .embeddingStore(pgVectorEmbeddingStore)
                    .filter(new IsIn("kb_id", kbIdList))
                    .build();
            builder.contentRetriever(contentRetriever);
        }
        // 如果Agent开启共享flow memory功能
        String memoryId = shareSessionMemory ? sessionId : sessionId + ":" + currentAgent.getAgentNodeId();
        return builder.streamingChatModel(llm)
                .systemMessageProvider(x -> {
                    if (CollUtil.isNotEmpty(currentAgent.getAgentSkills())) {
                        return currentAgent.getSystemMessage() + String.format(Constants.SKILL_APPEND_SYSTEM_MESSAGE, formatAvailableSkills(currentAgent.getAgentSkills()));
                    } else {
                        return currentAgent.getSystemMessage();
                    }
                })
                .chatMemory(MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .chatMemoryStore(memoryStore)
                        // 记忆轮次需要x2
                        .maxMessages(currentAgent.getMemoryCount() * 2)
                        .build())
                .toolProvider(new JToolProvider(currentAgent, enableFlowTool))
                .maxToolCallingRoundTrips(aiConfig.getAgent().getMaxSequentialToolsInvocations())
                .build();
    }

    protected String formatAvailableSkills(Collection<AgentSkill> skills) {
        StringBuilder sb = new StringBuilder();
        sb.append("<available_skills>\n");
        for (AgentSkill skill : skills) {
            sb.append("<skill>\n")
                    .append("<name>")
                    .append(escapeXml(skill.getSkillName()))
                    .append("</name>\n")
                    .append("<description>")
                    .append(escapeXml(skill.getSkillDescription()))
                    .append("</description>\n")
                    .append("</skill>\n");
        }
        sb.append("</available_skills>");
        return sb.toString();
    }

    private static String escapeXml(String input) {
        if (input == null) {
            return null;
        }
        return input
                .replace("&", "&amp;") // must be first
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    @Override
    public List<ChatFlowMessage> listAgentFlowChatMessageBySessionId(String sessionId, Long agentId) {
        throw new ServiceException("not implemented");
    }

    @Override
    public List<ChatFlowMessage> listAgentFlowChatMessageBySessionIdOrderDesc(String sessionId, Long agentId) {
        throw new ServiceException("not implemented");
    }

    protected String getAIResponse(ChatResponse response) {
        StringBuilder content = new StringBuilder();
        String thinking = response.aiMessage().thinking();
        if (StringUtils.isNotBlank(thinking)) {
            content.append(Constants.THINK_PREFIX_TAG)
                    .append(thinking)
                    .append(Constants.THINK_SUFFIX_TAG);
        }
        String aiText = response.aiMessage().text();
        content.append(aiText);
        return content.toString();
    }

    protected void handleRetrieveRecord(AtomicReference<List<Map<String, Object>>> retrieveList, List<Content> contents) {
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
    }


// ===================== 【新增：SSE串行发送工具封装，提供给子类调用】 =====================

    /**
     * SSE发送上下文载体，一次对话生命周期内复用
     */
    public static class SseEmitterContext {
        public final FluxSink<String> rawSink;
        public final AtomicBoolean sinkClosed;
        public final AtomicBoolean emitting;
        public final Queue<String> msgQueue;
        public final Runnable drainTask;
        public final BiConsumer<String, String> safeEmitter;

        public SseEmitterContext(FluxSink<String> rawSink) {
            this.rawSink = rawSink;
            this.sinkClosed = new AtomicBoolean(false);
            this.emitting = new AtomicBoolean(false);
            this.msgQueue = new ConcurrentLinkedQueue<>();

            // 队列排空任务（修复lambda自引用告警，使用匿名内部类）
            this.drainTask = new Runnable() {
                @Override
                public void run() {
                    if (!emitting.compareAndSet(false, true)) {
                        return;
                    }
                    try {
                        String data;
                        while (!sinkClosed.get() && (data = msgQueue.poll()) != null) {
                            rawSink.next(data);
                        }
                    } finally {
                        emitting.set(false);
                        if (!msgQueue.isEmpty() && !sinkClosed.get()) {
                            this.run();
                        }
                    }
                }
            };

            this.safeEmitter = (jsonStr, evt) -> {
                if (sinkClosed.get()) {
                    return;
                }
                msgQueue.offer(jsonStr);
                drainTask.run();
            };
        }

        /**
         * 标记链路关闭，防止重复complete/error
         */
        public boolean markClosed() {
            return sinkClosed.compareAndSet(false, true);
        }
    }

    /**
     * 工厂方法：创建SSE串行发送上下文，子类chat()直接调用
     *
     * @param rawSink Flux.create传入的原始FluxSink
     * @return SseEmitterContext 包含rawSink、safeEmitter、sinkClosed等全部并发对象
     */
    protected SseEmitterContext createSseEmitterContext(FluxSink<String> rawSink) {
        return new SseEmitterContext(rawSink);
    }

    /**
     * 通用发送事件封装（兼容子类原有调用方式）
     */
    protected void emitFluxEvent(String event, Object value, Long agentId, String agentName,
                                 BiConsumer<String, String> safeEmitter) {
        Map<String, Object> msg = MapUtil.<String, Object>builder()
                .put(event, value)
                .put("agentId", agentId)
                .put("agentName", agentName)
                .build();
        try {
            safeEmitter.accept(objectMapper.writeValueAsString(msg), event);
        } catch (JsonProcessingException e) {
            log.error("SSE事件序列化失败,event={}", event, e);
        }
    }

}
