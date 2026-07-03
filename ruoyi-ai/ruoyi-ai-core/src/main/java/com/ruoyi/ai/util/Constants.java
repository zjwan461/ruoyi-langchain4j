package com.ruoyi.ai.util;

public interface Constants {

    String KB_ID = "kb_id";

    String ALL_ALLOW_PATTERN = "[\\s\\S]*";

    String LOCAL_EMBEDDING_MODEL_FILE = "/onnx/model.onnx";

    String LOCAL_EMBEDDING_TOKENIZER_FILE = "/onnx/tokenizer.json";

    String TEST_EMBEDDING_TEXT = "test";

    String TEST_CHAT_TEXT = "hello";

    String AI_CHAT_CLIENT_SESSION = "ai:chat:client:";

    String AI_AGENT_CHAT_LMT = "ai:agent:limit:";

    String MEMORY_CACHE_KEY_PREFIX = "ai:agent:memory:";

    String THINK_PREFIX_TAG = "<think>";

    String THINK_SUFFIX_TAG = "</think>";

    String KNOWLEDGE_BASE_TEMPLATE = "{data}";

    String USER_MSG_TEMPLATE = "{question}";

    int EMBEDDING_DIMENSION = 768;

}
