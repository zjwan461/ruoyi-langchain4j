package com.ruoyi.ai.service.impl;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.ruoyi.ai.domain.AiAgent;
import com.ruoyi.ai.domain.KnowledgeBase;
import com.ruoyi.ai.domain.Model;
import com.ruoyi.ai.enums.ModelProvider;
import com.ruoyi.ai.enums.ModelType;
import com.ruoyi.ai.service.IAiChatService;
import com.ruoyi.ai.service.IKnowledgeBaseService;
import com.ruoyi.ai.service.IModelService;
import com.ruoyi.ai.service.LangChain4jService;
import com.ruoyi.common.exception.ServiceException;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AiChatServiceImpl implements IAiChatService {

    @Resource
    private LangChain4jService langChain4jService;

    @Resource
    private IModelService modelService;

    @Resource
    private IKnowledgeBaseService knowledgeBaseService;

    @Resource
    private ModelBuilder modelBuilder;

    private static final TimedCache<String, ChatMemory> chatMemories = new TimedCache<>(
            TimeUnit.DAYS.toMillis(1));

    public static final String MEMORY_CACHE_KEY_PREFIX = "ai:agent:memory:";

    @Override
    public Flux<String> chat(AiAgent aiAgent, String prompt, String sessionId) {
        Model model = modelService.selectModelById(aiAgent.getModelId());

        StreamingChatModel llm = getLLM(model);

        String promptTemplate = aiAgent.getPromptTemplate();
        promptTemplate = promptTemplate.replaceAll("\\{question}", prompt);

        if (promptTemplate.contains("{data}") && aiAgent.getKbId() != null) {
            KnowledgeBase kb = knowledgeBaseService.selectKnowledgeBaseById(aiAgent.getKbId());
            EmbeddingModel embeddingModel;
            if (kb != null) {
                embeddingModel = getEmbeddingModel();
                Map<String, Object> metadata = MapUtil.<String, Object>builder()
                        .put("kbId", kb.getId()).build();
                List<EmbeddingMatch<TextSegment>> searchRes = langChain4jService.search(embeddingModel,
                        prompt,
                        3, 0.70, metadata);
                StringBuilder embBuilder = new StringBuilder();
                searchRes.stream().map(EmbeddingMatch::embedded).forEach(embedded -> {
                    String text = embedded.text();
                    embBuilder.append(text);
                });
                promptTemplate = promptTemplate.replaceAll("\\{data}", embBuilder.toString());
            }
        } else if (promptTemplate.contains("{data}") && aiAgent.getKbId() == null) {
            promptTemplate = promptTemplate.replaceAll("\\{data}", "");
        }

        ChatMemory chatMemory = null;
        if (aiAgent.getMemoryCount() != null && aiAgent.getMemoryCount() > 0) {
            chatMemory = chatMemories.get(MEMORY_CACHE_KEY_PREFIX + sessionId);
            if (chatMemory == null) {
                chatMemory = MessageWindowChatMemory.builder()
                        .chatMemoryStore(new InMemoryChatMemoryStore())
                        .maxMessages(aiAgent.getMemoryCount())
                        .id(sessionId)
                        .build();
                chatMemories.put(MEMORY_CACHE_KEY_PREFIX + sessionId, chatMemory);
            }
        }
        UserMessage userMessage = UserMessage.from(promptTemplate);
        if (chatMemory != null) {
            chatMemory.add(userMessage);
        }
        final ChatRequest chatRequest = ChatRequest.builder()
                .temperature(aiAgent.getTemperature())
                .maxOutputTokens(aiAgent.getMaxOutputToken())
                .messages(
                        chatMemory != null ? chatMemory.messages() : Collections.singletonList(userMessage))
                .build();

        return Flux.create(fluxSink -> {
            ThreadUtil.execute(() -> {

                llm.chat(chatRequest, new StreamingChatResponseHandler() {

                    @Override
                    public void onPartialResponse(String partialResponse) {
                        fluxSink.next(partialResponse);
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse completeResponse) {
                        fluxSink.complete();
                    }

                    @Override
                    public void onError(Throwable error) {
                        fluxSink.error(error);
                    }
                });

            });
        });

    }

    private EmbeddingModel getEmbeddingModel() {
        Model query = new Model();
        query.setType(ModelType.EMBEDDING.getValue());
        List<Model> models = modelService.selectModelList(query);
        if (models == null) {
            throw new ServiceException("尚未配置embedding模型,无法进行知识库查询");
        }
        Model first = models.get(0);
        return modelBuilder.getEmbeddingModel(first);
    }

    private StreamingChatModel getLLM(Model model) {
        ModelProvider provider = ModelProvider.fromValue(model.getProvider());
        StreamingChatModel llm = null;
        if (provider == ModelProvider.OLLAMA) {
            llm = OllamaStreamingChatModel.builder()
                    .baseUrl(model.getBaseUrl())
                    .modelName(model.getName())
                    .build();
        } else {
            llm = OpenAiStreamingChatModel.builder()
                    .baseUrl(model.getBaseUrl())
                    .modelName(model.getName())
                    .apiKey(model.getApiKey())
                    .build();
        }
        return llm;
    }
}
