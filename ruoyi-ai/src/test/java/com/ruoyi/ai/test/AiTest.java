package com.ruoyi.ai.test;

import com.ruoyi.common.utils.uuid.IdUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.onnx.PoolingMode;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import static dev.langchain4j.rag.content.ContentMetadata.EMBEDDING_ID;

public class AiTest {

  public static final String LLM_BASE_URL = "http://10.100.216.70:8000/v1";
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
        .modelName("qwen3-8b")
        .build();
  }

  private static ChatRequestParameters buildParameters() {
    return OpenAiChatRequestParameters.builder()
        .modelName("qwen3-8b")
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
        .modelName("qwen3-8b")
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
  public void testRag() {
    Document document = FileSystemDocumentLoader.loadDocument(
        "C:\\Users\\user\\Desktop\\FPS轉數快查詢歷史交易查詢API設計.md");
//    System.out.println(document);
    OllamaEmbeddingModel model = this.buildEmbeddingModel();
    DocumentSplitter documentSplitter = DocumentSplitters.recursive(300, 10);
    List<TextSegment> textSegments = documentSplitter.split(document);
    EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
    embeddingStore = PgVectorEmbeddingStore.builder()
        .host("10.100.217.4")
        .port(5432)
        .database("jllama")
        .user("root")
        .password("root")
        .table("embedding")
        .dimension(model.dimension())
        .build();
//    EmbeddingStoreIngestor.ingest(document, embeddingStore);

//    Response<List<Embedding>> resp = model.embedAll(
//        Collections.singletonList(document.toTextSegment()));

//    List<Embedding> embeddings = resp.content();
//    embeddingStore.addAll(embeddings);
//    TextSegment textSegment = document.toTextSegment();
//    Response<Embedding> resp = model.embed(textSegment);
//    Embedding embedding = resp.content();
    //embedding存储必须带上embedding和原始文档,否则在查询知识库的时候无法获取到embedded
//    embeddingStore.add(embedding, textSegment);
    //

//    Response<List<Embedding>> resp = model.embedAll(textSegments);
//    List<Embedding> embeddings = resp.content();
//    embeddingStore.addAll(embeddings, textSegments);

//    embeddingStore.serializeToFile("C:\\Users\\user\\Desktop\\FPS轉數快查詢歷史交易查詢API設計.bin");

    System.out.println("success");
    EmbeddingStoreContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
        .embeddingStore(embeddingStore)
        .embeddingModel(model)
        .filter(new IsEqualTo("kb_id", Long.valueOf(1l)))
        .maxResults(3)
        .minScore(0.6)
        .build();

    List<Content> contents = retriever.retrieve(Query.from("在scb這一層"));
    System.out.println(contents.size());

    Object embId = contents.get(0).metadata().get(EMBEDDING_ID);
    System.err.println(embId);
//    contents.forEach(System.out::println);

//    OpenAiChatModel chatModel = buildBlockModel();
//    ChatRequestParameters chatRequestParameters = buildParameters();
//    ChatRequest request = ChatRequest.builder()
//        .parameters(chatRequestParameters)
//        .messages(UserMessage.from("你好"))
//        .build();
//    ChatResponse chatResponse = chatModel.doChat(request);
//    System.err.println(chatResponse);

  }


  public OllamaEmbeddingModel buildEmbeddingModel() {
    return OllamaEmbeddingModel.builder().baseUrl(OLLAMA_BASE_URL)
        .modelName("nomic-embed-text:latest")
        .build();
  }


  @Test
  public void testAiModel() throws Exception {
    Document document = FileSystemDocumentLoader.loadDocument(
        "C:\\Users\\user\\Desktop\\FPS轉數快查詢歷史交易查詢API設計.md");

//    document = UrlDocumentLoader.load(
//        "http://localhost:8080/profile/upload/2025/08/25/FPS轉數快查詢歷史交易查詢API設計_20250825174322A001.md",
//        new TextDocumentParser());
    DocumentSplitter splitter = DocumentSplitters.recursive(500, 10);

    List<TextSegment> textSegments = splitter.split(document);

    System.out.println(textSegments);

//    ObjectMapper objectMapper = new ObjectMapper();
//    System.out.println(objectMapper.writeValueAsString(textSegments));

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

    Response<Embedding> response = embeddingModel.embed("新反数码诈骗措施");
    Embedding embedding = response.content();

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
            .filter(new IsEqualTo("kb_id","1"))
            .minScore(0.7)
            .build();

    EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
    System.out.println(result.matches());

  }
}
