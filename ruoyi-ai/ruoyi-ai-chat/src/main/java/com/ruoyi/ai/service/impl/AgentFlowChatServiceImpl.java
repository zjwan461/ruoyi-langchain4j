package com.ruoyi.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.component.flow.FlowTreeParser;
import com.ruoyi.ai.component.flow.dto.AgentNode;
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
        if (this.treeNode == null) {
            this.treeNode = parser.parseToTree(agentFlow.getFlowJson());
            this.agentFlow = agentFlow;
        }
        this.currentNodeId = treeNode.getNode().getNodeId();
        Integer rootAgentId = treeNode.getNode().getAgentId();
        this.currentAgent = this.aiAgentService.selectAiAgentWithToolAndSkillById(Long.valueOf(rootAgentId));
        this.currentAssistant = this.buildAIAssistant(this.currentAgent, sessionId, true);
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
        String finalPromptTemplate = this.attachSubAgentPrompt(promptTemplate);
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
                    // todo save too-call kb-retrieve to mysql
                })
                .onToolExecuted(toolExecuted -> {
                    ToolExecutionRequest req = toolExecuted.request();
                    String result = toolExecuted.result();
                    log.info("调用工具完成{}, result={}", req.name(), result);
                    Map<String, Object> value = MapUtil.<String, Object>builder()
                            .put("toolName", req.name())
                            .put("toolInvokeResult", result).build();
                    this.emitFluxEvent(Constants.SSE_EVENT_TOOL_CALLED, value, fluxSink);
                    if (Constants.AGENT_SELECTOR_TOOL.equals(req.name()) && StringUtils.isNotBlank(result)) {
                        this.nextAIAssistant(result, sessionId, clientId, request, fluxSink);
                    }
                    // todo save tool call record
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
                    // todo save kb retrieve record
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
    public void nextAIAssistant(String agentSpecStr, String sessionId, String clientId, String request, FluxSink<String> fluxSink) {
        AgentSpec nextAgentSpec = null;
        try {
            nextAgentSpec = objectMapper.readValue(agentSpecStr, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Agent spec json processing error", e);
        }
        if (nextAgentSpec != null) {
            this.currentNodeId = nextAgentSpec.getNodeId();
            this.currentAgent = aiAgentService.selectAiAgentWithToolAndSkillById(nextAgentSpec.getAgentId());
            this.currentAssistant = this.buildAIAssistant(this.currentAgent, sessionId, true);
            this.invoke(sessionId, clientId, request, fluxSink);
        }
    }

    @Override
    public Flux<String> chat(AgentFlow agentFlow, String prompt, String clientId, String sessionId) {
        return Flux.create(fluxSink -> {
            this.init(sessionId, agentFlow);
            this.invoke(sessionId, clientId, prompt, fluxSink);
        });
    }

    @Override
    public String attachSubAgentPrompt(String prompt) {
        TreeNode tree = parser.findTreeNodeById(this.treeNode, this.currentNodeId);
        List<TreeNode> children = tree.getChildren();
        if (CollUtil.isNotEmpty(children)) {
            prompt += """
                    你拥有如下子Agents, 他们的描述如下。请根据需要调用子Agent完成用户需求。
                    """;
            StringBuilder promptBuilder = new StringBuilder(prompt);
            for (TreeNode child : children) {
                AgentNode node = child.getNode();
                promptBuilder.append("- agentId: ").append(node.getAgentId())
                        .append(" agentName: ").append(node.getAgentName())
                        .append(" description: ").append(node.getDescription())
                        .append(" nodeId: ").append(node.getNodeId())
                        .append("\n");
            }
            promptBuilder.append("请调用agent_selector工具，获取需要调用的子Agent详细信息。");
            prompt = promptBuilder.toString();
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
}
