package com.ruoyi.ai.test;

import cn.hutool.core.util.RandomUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.component.mcp.JHttpMcpTransport;
import com.ruoyi.ai.util.Constants;
import com.ruoyi.common.utils.uuid.IdUtils;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.internal.JsonSchemaElementJsonUtils;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpCallContext;
import dev.langchain4j.mcp.client.McpHeadersSupplier;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.onnx.PoolingMode;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.skills.FileSystemSkill;
import dev.langchain4j.skills.FileSystemSkillLoader;
import dev.langchain4j.skills.Skills;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Var;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static dev.langchain4j.rag.content.ContentMetadata.EMBEDDING_ID;

@Slf4j
public class AiTest {

    public static final String LLM_BASE_URL = "https://dashscope-intl.aliyuncs.com/compatible-mode/v1";
    public static final String OLLAMA_BASE_URL = "http://10.100.217.2:11434";

    @Test
    public void chatBlock() {
        OpenAiChatModel openAiChatModel = buildBlockModel();
        ChatRequestParameters chatRequestParameters = buildParameters();
        ChatRequest request = ChatRequest.builder()
                .parameters(chatRequestParameters)
                .messages(UserMessage.from("你好"))
                .build();

        ChatResponse chatResponse = openAiChatModel.doChat(request);
        System.out.println(chatResponse.aiMessage().text());

    }

    private static OpenAiChatModel buildBlockModel() {
        return OpenAiChatModel.builder()
                .baseUrl(LLM_BASE_URL)
                .apiKey("sk-0543377485b74778b3728e250df4e7d1")
                .modelName("qwen3.7-plus")
                .build();
    }

    private static ChatRequestParameters buildParameters() {
        return OpenAiChatRequestParameters.builder()
                .modelName("qwen3.7-plus")
                .temperature(0.7)
//        .topK(40)
//        .topP(0.9)
                .build();
    }

    @Test
    public void chatStreaming() throws IOException {
        OpenAiStreamingChatModel model = buildStreamingModel();

        ChatRequestParameters parameters = buildParameters();
        String question = "帮我写一个正则用来匹配<think></think>标签内的内容，包含think本身。";

        ChatRequest chatRequest = ChatRequest.builder()
                .parameters(parameters)
                .messages(UserMessage.from(question))
                .build();

        model.chat(chatRequest, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                System.out.print(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                System.out.println();
//        System.out.println("complete: " + completeResponse.aiMessage().text());
            }

            @Override
            public void onError(Throwable error) {
                error.printStackTrace();
            }
        });

        System.in.read();
    }

    private static OpenAiStreamingChatModel buildStreamingModel() {
        OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder()
                .baseUrl(LLM_BASE_URL)
                .modelName("qwen3.7-plus")
                .apiKey("sk-0543377485b74778b3728e250df4e7d1")
                .build();
        return model;
    }


    public static ChatMemory buildChatMemory() {
        return MessageWindowChatMemory.builder()
                .id(IdUtils.fastUUID())
                .maxMessages(10)
                .chatMemoryStore(new InMemoryChatMemoryStore())
                .build();
    }


    public static void main(String[] args) {
        System.out.println("请输入您的问题：");
        Scanner scanner = new Scanner(System.in);

        OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder()
                .baseUrl(LLM_BASE_URL)
                .modelName("qwen3-8b")
                .build();

        ChatRequestParameters chatRequestParameters = buildParameters();

        ChatMemory chatMemory = buildChatMemory();

        boolean start = true;
        while (start) {
            String question = scanner.nextLine();
            if ("quit".equals(question)) {
                start = false;
            } else {
                UserMessage qmsg = UserMessage.from(question);
                chatMemory.add(qmsg);
//      chat
                ChatRequest request = ChatRequest.builder()
                        .parameters(chatRequestParameters)
                        .messages(chatMemory.messages())
                        .build();

                model.chat(request, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        System.out.print(partialResponse);
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse completeResponse) {
                        System.out.println();
                        System.out.println("complete");
                    }

                    @Override
                    public void onError(Throwable error) {
                        error.printStackTrace();
                    }
                });
            }
        }
    }

    @Test
    public void testService() throws IOException {
        OpenAiChatModel model = buildBlockModel();
//    Assistant assistant = AiServices.create(Assistant.class, model);
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemory(buildChatMemory())
                .build();
        String resp = assistant.chat("你好");
        System.out.println(resp);

//    System.in.read();
    }

    @Test
    public void testLocalEmbedding() {
//    AllMiniLmL6V2EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
//    Response<Embedding> response = embeddingModel.embed("test");
//    System.out.println(response.content());

        String pathToModel = "F:\\workspaces\\ruoyi-langchain4j\\models\\onnx\\model.onnx";
        String pathToTokenizer = "F:\\workspaces\\ruoyi-langchain4j\\models\\onnx\\tokenizer.json";
        PoolingMode poolingMode = PoolingMode.MEAN;
        EmbeddingModel embeddingModel = new OnnxEmbeddingModel(pathToModel, pathToTokenizer, poolingMode);

        Response<Embedding> response = embeddingModel.embed("test");
        Embedding embedding = response.content();
        System.out.println(response.content().dimension());

        PgVectorEmbeddingStore embeddingStore = PgVectorEmbeddingStore.builder()
                .host("localhost")
                .port(5432)
                .database("embedding")
                .user("root")
                .password("root")
                .table("embedding")
                .dimension(embeddingModel.dimension())
                .build();
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .filter(new IsEqualTo("kb_id", "1"))
                .minScore(0.7)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
        System.out.println(result.matches());

    }

    @Test
    public void testNomicEmbedText() {
        OllamaEmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("nomic-embed-text:latest")
                .build();

        Response<Embedding> response = embeddingModel.embed("test");
        Embedding embedding = response.content();
        System.out.println(embedding.dimension());

        AllMiniLmL6V2EmbeddingModel embeddingModel2 = new AllMiniLmL6V2EmbeddingModel();
        Response<Embedding> response2 = embeddingModel2.embed("test");
        System.out.println(response2.content().dimension());
    }

    @Test
    public void testLocalEmbeddingModel() {
        String saveDir = "D:\\workspaces\\ruoyi-langchain4j\\models";
        try {
            String pathToModel = saveDir + Constants.LOCAL_EMBEDDING_MODEL_FILE;
            String pathToTokenizer = saveDir + Constants.LOCAL_EMBEDDING_TOKENIZER_FILE;
            PoolingMode poolingMode = PoolingMode.MEAN;
            EmbeddingModel embeddingModel = new OnnxEmbeddingModel(pathToModel, pathToTokenizer, poolingMode);
            Response<Embedding> response = embeddingModel.embed(Constants.TEST_EMBEDDING_TEXT);
            System.out.println(response.content().toString());
        } catch (Exception e) {
            log.error("检查本地模型失败", e);
        }
    }

    @SneakyThrows
    @Tool
    public String getWeather(String city) {
        TimeUnit.SECONDS.sleep(1);
        return "当前" + city + "的气温是" + RandomUtil.randomNumbers(50) + "摄氏度";
    }


    @Test
    public void testSkill() {
//        Skill.builder().name("")
//                .description("")
//                .content("");
        List<FileSystemSkill> skills = FileSystemSkillLoader.loadSkills(Path.of("skills"));
        System.out.println(skills);
        Skills skillList = Skills.from(skills);
        OpenAiChatModel model = buildBlockModel();
//    Assistant assistant = AiServices.create(Assistant.class, model);
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .toolProvider(skillList.toolProvider())
                .chatMemory(buildChatMemory())
                .systemMessage("You have access to the following skills:\n" + skillList.formatAvailableSkills()
                        + "\nWhen the user's request relates to one of these skills, activate it first using the `activate_skill` tool before proceeding.")
                .build();
        String resp = assistant.chat("你好,Spring的3个重点，帮我使用json格式输出");
        System.out.println(resp);
    }

    @Test
    public void testMcp() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JHttpMcpTransport mcpTransport = JHttpMcpTransport.builder()
                .sseUrl("http://localhost:8001/sse")
                .customHeaders(new McpHeadersSupplier() {
                    @Override
                    public Map<String, String> apply(McpCallContext mcpCallContext) {
                        return Map.of("Authorization", "Bearer " + IdUtils.fastSimpleUUID());
                    }
                })
                .logRequests(true) // if you want to see the traffic in the log
                .logResponses(true)
                .timeout(Duration.ofSeconds(300))
                .build();
        DefaultMcpClient client = DefaultMcpClient.builder()
                .transport(mcpTransport)
                .toolExecutionTimeout(Duration.ofSeconds(300))
                .build();

        List<ToolSpecification> toolSpecifications = client.listTools();
        for (ToolSpecification toolSpecification : toolSpecifications) {
//            System.out.println(toolSpecification);
            Map<String, Object> map = JsonSchemaElementJsonUtils.toMap(toolSpecification.parameters());
            String json = objectMapper.writeValueAsString(map);
            System.out.println(json);
            System.out.println(toolSpecification.metadata());
            System.out.println(toolSpecification.strict());
        }

//        client.executeTool()


//        McpToolProvider mcpToolProvider = McpToolProvider.builder()
//                .mcpClients(client)
//                .build();
//        List<AiServiceTool> aiServiceTools = mcpToolProvider.provideTools(null).aiServiceTools();
//
//        OpenAiChatModel model = buildBlockModel();
//        Assistant assistant = AiServices.builder(Assistant.class)
//                .chatModel(model)
//                .systemMessage("在必要时调用工具")
//                .toolProvider(mcpToolProvider)
//                .chatMemory(buildChatMemory())
//                .build();
//
//        String resp = assistant.chat("帮我写一篇200字的短文，使用markdown格式。写完以后调用工具转成docx。完成后给我docx下载链接");
//        System.out.println(resp);
    }

    @Test
    public void testDoc() {
        Document document = FileSystemDocumentLoader.loadDocument(Path.of("C:\\Users\\jerry\\Desktop\\Spring Boot技术介绍文档.pdf"), new ApacheTikaDocumentParser());
        String text = document.text();
        Metadata metadata = document.metadata();
        System.out.println(text);
        System.out.println(metadata);
    }

    @Test
    public void testAgentic() throws IOException {
        OpenAiChatModel llm = buildBlockModel();
        OpenAiStreamingChatModel sllm = buildStreamingModel();
        UntypedAgent creativeWriter = AgenticServices.agentBuilder()
//                .chatModel(llm)
                .streamingChatModel(sllm)
                .description("Generate a story based on the given topic")
                .userMessage("""
                        Anwer me in English
                        You are a creative writer.
                        Generate a draft of a story no more than
                        3 sentences long around the given topic.
                        Return only the story and nothing else.
                        The topic is {{topic}}.
                        """)
                .inputKey(String.class, "topic")
                .returnType(TokenStream.class) // String is the default return type for untyped agents
                .outputKey("story")
                .build();

        UntypedAgent translator = AgenticServices.agentBuilder()
//                .chatModel(llm)
                .streamingChatModel(sllm)
                .description("translate the text based on user given language")
                .userMessage("""
                        You are a translator.
                        You need translate the text to Simple Chinese.
                        The text is {{story}}.
                        """)
                .inputKey(String.class, "story")
                .returnType(TokenStream.class) // String is the default return type for untyped agents
                .outputKey("story")
                .build();

        SequenceStreamingAgent answer = AgenticServices.sequenceBuilder(SequenceStreamingAgent.class)
                .subAgents(creativeWriter, translator)
                .outputKey("story")
                .build();

        TokenStream stream = answer.sequenceAgent("火影忍者", "中文");
        System.out.println(stream);
        stream.onPartialThinkingWithContext((x, y) -> {
            System.out.print(x.text());
        }).onPartialResponseWithContext((x, y) -> {
            System.out.println();
            System.out.print(x.text());
        }).onCompleteResponse(chatResponse -> {
            System.out.println();
            System.err.println(chatResponse.aiMessage().text());
        }).onError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                throwable.printStackTrace();
            }
        });
        stream.start();
        System.in.read();
    }

    @Test
    public void testAgentic2() throws IOException {
        OpenAiStreamingChatModel llm = buildStreamingModel();
        Writer writer = AgenticServices.agentBuilder(Writer.class)
                .streamingChatModel(llm)
                .description("写作Agent")
                .userMessage("根据主题创作一篇英文文章，3到5行即可{{topic}}")
                .outputKey("story")
                .build();

        Translator translator = AgenticServices.agentBuilder(Translator.class)
                .streamingChatModel(llm)
                .description("翻译Agent")
                .userMessage("将这段内容\"{{story}}\" 翻译成{{language}}")
                .outputKey("story")
                .build();

        SequenceStreamingAgent answer = AgenticServices.sequenceBuilder(SequenceStreamingAgent.class)
                .subAgents(writer, translator)
                .outputKey("story")
                .build();

        TokenStream stream = answer.sequenceAgent("火影忍者", "中文");
        System.out.println(stream);
        stream.onPartialThinkingWithContext((x, y) -> {
            System.out.print(x.text());
        }).onPartialResponseWithContext((x, y) -> {
            System.out.println();
            System.out.print(x.text());
        }).onCompleteResponse(chatResponse -> {
            System.out.println();
            System.err.println(chatResponse.aiMessage().text());
        }).onError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                throwable.printStackTrace();
            }
        });
        stream.start();
        System.in.read();
    }

    @Test
    public void testAgentic3() throws IOException {
        OpenAiStreamingChatModel llm = buildStreamingModel();
        Writer writer = AgenticServices.agentBuilder(Writer.class)
                .streamingChatModel(llm)
                .description("写作Agent")
                .userMessage("根据主题创作一篇英文文章，3到5行即可{{topic}}")
                .outputKey("story")
                .build();

        Translator translator = AgenticServices.agentBuilder(Translator.class)
                .streamingChatModel(llm)
                .description("翻译Agent")
                .userMessage("将这段内容\"{{story}}\" 翻译成{{language}}")
                .outputKey("story")
                .build();

        Supervisor supervisorAgent = AgenticServices.supervisorBuilder(Supervisor.class)
                .subAgents(writer, translator)
                .outputKey("story")
                .chatModel(buildBlockModel())
                .maxAgentsInvocations(3)
                .description("主管agent，根据需要调用子agent完成各种功能")
                .build();

        TokenStream tokenStream = supervisorAgent.invoke("你好");

    }
}
