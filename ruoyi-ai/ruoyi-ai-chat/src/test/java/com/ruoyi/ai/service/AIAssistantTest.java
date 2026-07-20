package com.ruoyi.ai.service;

import com.ruoyi.ai.domain.AIAgent;
import com.ruoyi.ai.tool.JToolProvider;
import com.ruoyi.ai.tool.TimeTool;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.store.embedding.filter.comparison.IsIn;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

public class AIAssistantTest {

    public static final String LLM_BASE_URL = "https://dashscope-intl.aliyuncs.com/compatible-mode/v1";

    private static OpenAiStreamingChatModel buildStreamingModel() {
        OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder()
                .baseUrl(LLM_BASE_URL)
                .apiKey("sk-0543377485b74778b3728e250df4e7d1")
                .modelName("qwen3.7-plus")
                .build();
        return model;
    }

    @Test
    public void testToolCall() throws Exception {
        OpenAiStreamingChatModel model = buildStreamingModel();
//        ToolSpecification getWeather = ToolSpecifications.toolSpecificationFrom(AiTest.class.getDeclaredMethod("getWeather", String.class));
//        ChatRequest request = ChatRequest.builder()
//                .messages(UserMessage.from("你好，深圳的天气怎么样"))
//                .toolSpecifications(getWeather)
//                .build();

        AIAssistant assistant = AiServices.builder(AIAssistant.class)
                .streamingChatModel(model)
                .systemMessageProvider(x -> {
                    System.err.println(x);
                    return "在必要的时候调用工具";
                })
                .tools(new TimeTool())
                .build();
        TokenStream tokenStream = assistant.chatStream("今天的日期");
        tokenStream
                .onPartialResponse((String partialResponse) -> System.out.println(partialResponse))
                .onPartialThinking((PartialThinking partialThinking) -> System.out.println(partialThinking))
                .onRetrieved((List<Content> contents) -> System.out.println(contents))
                .onIntermediateResponse((ChatResponse intermediateResponse) -> System.out.println(intermediateResponse))
                // 这将在工具执行之前调用。BeforeToolExecution 包含 ToolExecutionRequest（例如工具名称、工具参数等）
                .beforeToolExecution((beforeToolExecution) -> System.out.println(beforeToolExecution))
                // 这将在工具执行之后调用。ToolExecution 包含 ToolExecutionRequest 和工具执行结果。
                .onToolExecuted((ToolExecution toolExecution) -> System.out.println(toolExecution))
                .onCompleteResponse((ChatResponse response) -> System.out.println(response))
                .onError((Throwable error) -> error.printStackTrace())
                .start();

        System.in.read();
    }

    @Test
    public void testAssistant() throws Exception {
        AIAgent aiAgent = new AIAgent();
        EmbeddingStoreContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingModel(null)
                .minScore(aiAgent.getMinScore())
                .maxResults(aiAgent.getMaxResult())
                .displayName("")
                .embeddingStore(PgVectorEmbeddingStore.builder().build())
                .filter(new IsIn("kb_id", Collections.singleton("1")))
                .build();
        AiServices.builder(AIAssistant.class)
                .streamingChatModel(buildStreamingModel())
                .systemMessageProvider(x -> "在必要的时候调用工具")
                .contentRetriever(contentRetriever)
                .toolProvider(new JToolProvider(aiAgent, false));
    }

}
