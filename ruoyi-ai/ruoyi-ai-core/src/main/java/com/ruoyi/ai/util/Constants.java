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

    String DIFY_USER = "RY_AI_DIFY_USER";

    int MAX_MEMORY_MESSAGE_COUNT = 50;

    int MEMORY_TTL_SECONDS = -1;

    String SSE_EVENT_MSG = "msg";

    String SSE_EVENT_TOOL_CALL = "tool_call";

    String SSE_EVENT_TOOL_CALLED = "tool_called";

    String SSE_EVENT_RETRIEVED = "retrieved";

    String SKILL_APPEND_SYSTEM_MESSAGE = """

            You have access to the following skills:
          
            %s

            When the user's request relates to one of these skills, activate it first using the `activate_skill` tool before proceeding.""";

    String AGENT_SELECTOR_TOOL = "agent_selector";

    String DEFAULT_SUB_AGENT_ATTACH_PROMPT = """

            你拥有如下子Agents, 他们的描述如下。
            
            %s
            
            如有需要，你可以根据子Agent的描述选择对应Agent的agentId, 调用**agent_selector**工具获取要调用的子Agent的完整信息,用来调用子Agent。
            宪法（必须遵循）:
                 1. 仅在需要调用子Agent时才调用agent_selector工具。
                 2. 不必等待子Agent的返回结果，子Agent会在你执行完成后才执行。
            
            """;

}
