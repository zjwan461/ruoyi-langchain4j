package com.ruoyi.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.component.flow.FlowTreeParser;
import com.ruoyi.ai.component.flow.dto.TreeNode;
import com.ruoyi.ai.component.memory.RedisMemoryChatMemoryStore;
import com.ruoyi.ai.config.AiConfig;
import com.ruoyi.ai.domain.AIAgent;
import com.ruoyi.ai.domain.AgentFlow;
import com.ruoyi.ai.domain.Model;
import com.ruoyi.ai.service.*;
import com.ruoyi.ai.util.Constants;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysConfigService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.model.chat.ChatModel;
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
@Component
@Scope("prototype")
public class AgentFlowServiceImpl extends AIBaseChatServiceImpl implements AgentFlowService {

    private AIAssistant currentAssistant;

    private AIAgent currentAgent;

    private String currentNodeId;

    private TreeNode treeNode;

    private AgentFlow agentFlow;

    @Resource
    private FlowTreeParser parser;

    public AgentFlowServiceImpl(IChatMessageService chatMessageService,
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
        if (this.treeNode == null) {
            this.treeNode = parser.parseToTree(agentFlow.getFlowJson());
            this.agentFlow = agentFlow;
        }
        this.currentNodeId = treeNode.getNode().getNodeId();
        Integer rootAgentId = treeNode.getNode().getAgentId();
        this.currentAgent = this.aiAgentService.selectAiAgentWithToolAndSkillById(Long.valueOf(rootAgentId));
        this.currentAssistant = this.buildAIAssistant(this.currentAgent, sessionId, true);
    }

    // todo build flow agent Assistant


    protected void changeAgent(AIAgent aiAgent, String sessionId, String clientId, String request, FluxSink<String> fluxSink) {
        this.currentAgent = aiAgent;
        this.currentAssistant = this.buildAIAssistant(this.currentAgent, sessionId, true);

        this.invoke(sessionId, clientId, request, fluxSink);
    }


    @Override
    public void invoke(String sessionId, String clientId, String request, FluxSink<String> fluxSink) {
        String promptTemplate = currentAgent.getPromptTemplate();
        if (promptTemplate.contains(Constants.USER_MSG_TEMPLATE)) {
            promptTemplate = promptTemplate.replace(Constants.USER_MSG_TEMPLATE, request);
        } else {
            promptTemplate = promptTemplate + request;
        }
        if (promptTemplate.contains(Constants.KNOWLEDGE_BASE_TEMPLATE) && StringUtils.isNotBlank(currentAgent.getKbIds())) {
            promptTemplate = promptTemplate.replace(Constants.KNOWLEDGE_BASE_TEMPLATE, "");
        }
        String finalPromptTemplate = promptTemplate;
        AtomicReference<List<Map<String, Object>>> retrieveList = new AtomicReference<>();
        AtomicReference<List<Map<String, Object>>> toolCallList = new AtomicReference<>();
        TokenStream tokenStream = currentAssistant.chatStream(finalPromptTemplate);
        AtomicInteger thinkCount = new AtomicInteger(0);
        AtomicBoolean thinkFinish = new AtomicBoolean(false);
        tokenStream.onPartialThinking(thinkingChunk -> {
                    String text = thinkingChunk.text();
                    if (thinkCount.incrementAndGet() == 1) {
                        text = Constants.THINK_PREFIX_TAG + text;
                    }
                    this.emitFluxEvent(Constants.SSE_EVENT_MSG, text, fluxSink);
                }).onPartialResponse(chunk -> {
                    if (thinkCount.intValue() > 0 && !thinkFinish.get()) {
                        chunk = Constants.THINK_SUFFIX_TAG + chunk;
                        thinkFinish.set(true);
                    }
                    this.emitFluxEvent(Constants.SSE_EVENT_MSG, chunk, fluxSink);
                }).onCompleteResponse(resp -> {
                    fluxSink.complete();
                })
                .onToolExecuted(toolExecuted -> {
                    String result = toolExecuted.result();
                    ToolExecutionRequest req = toolExecuted.request();
                    log.info("调用工具完成{}, result={}", req.name(), result);
                    Map<String, Object> value = MapUtil.<String, Object>builder().put("toolName", req.name()).put("toolInvokeResult", result).build();
                    this.emitFluxEvent(Constants.SSE_EVENT_TOOL_CALLED, value, fluxSink);
                    List<Map<String, Object>> toolCalls = toolCallList.get() == null ? new ArrayList<>() : toolCallList.get();
                    toolCalls.add(value);
                    toolCallList.set(toolCalls);
                }).beforeToolExecution(beforeToolExecution -> {
                    ToolExecutionRequest req = beforeToolExecution.request();
                    log.info("调用工具{}", req.name());
                    Map<String, Object> value = MapUtil.<String, Object>builder().put("toolName", req.name()).build();
                    this.emitFluxEvent(Constants.SSE_EVENT_TOOL_CALL, value, fluxSink);
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
                        this.emitFluxEvent(Constants.SSE_EVENT_RETRIEVED, retrieveList.get(), fluxSink);
                    }
                })
                .onError(fluxSink::error)
                .start();
    }

    @Override
    public void registerSubAgent() {
        // todo register sub agent for current AIAgent
    }

    @Override
    public AIAgent nextAIAssistant() {
        // todo call llm get next AI ASSISTANT, if not need return null
        Model model = modelService.selectModelById(this.currentAgent.getModelId());
        ChatModel llm = modelBuilder.getBlockingLLM(model);
        TreeNode tree = parser.findTreeNodeById(this.treeNode, this.currentNodeId);
        List<TreeNode> children = tree.getChildren();
        return null;
    }

    @Override
    public Flux<String> chat(AgentFlow agentFlow, String prompt, String clientId, String sessionId) {
        return Flux.create(fluxSink -> {
            this.init(sessionId, agentFlow);
            this.invoke(sessionId, clientId, prompt, fluxSink);
        });
    }
}
