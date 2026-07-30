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

    /**
     * 单次 agent_selector 返回的 agentIds 数组的最大长度。
     * <p>
     * 当前实现采用严格串行调度，批次越大时延越高、且会持续占用同一段 SSE；
     * 这里给一个保守上限避免模型误返回长列表导致资源耗尽。
     */
    int MAX_FLOW_BATCH_SIZE = 8;

    /**
     * 单次对话允许的 Agent 流转总轮数（含 root 与所有子节点）。
     * <p>
     * 防止配置错误或模型反复跳转导致无限循环。
     */
    int MAX_FLOW_ROUNDS = 10;

    String SKILL_APPEND_SYSTEM_MESSAGE = """

            You have access to the following skills:

            %s

            When the user's request relates to one of these skills, activate it first using the `activate_skill` tool before proceeding.""";

    String AGENT_SELECTOR_TOOL = "agent_selector";

    /**
     * agent_selector 子 Agent 选择提示词：告知模型必须一次返回有序的 agentIds 数组，
     * 顺序即串行执行顺序，最少 1 个、最多 {@link #MAX_FLOW_BATCH_SIZE} 个。
     */
    String DEFAULT_SUB_AGENT_BATCH_ATTACH_PROMPT = """

            你拥有如下子Agents, 他们的描述如下。

            %s

            当本次对话需要按顺序串行调用一个或多个子Agent时，请调用 **agent_selector** 工具，
            并在 `agentIds` 字段中按期望的执行顺序传入 agentId 数组（顺序就是执行顺序）。
            系统会严格按照你返回的数组顺序串行调度：前一个 Agent 执行完后，下一个 Agent 才会启动，
            并能看到前面 Agent 的输出和原始用户请求。agent_selector调用完成后即可退出，不得重复
            多次调用。完成后通知用户即将由子Agent为您处理后续任务即可。

            宪法（必须遵循）:
                 1. 仅在确需调用子Agent时才调用 agent_selector 工具；不需要调用子Agent时不要调用。
                 2. 本轮返回的 agentIds 必须是非空的有序数组，长度不得超过 %d。
                 3. agentIds 中的 agentId 必须来自下方子 Agent 列表；否则系统会拒绝执行。
                 4. 数组中的 agent 是顺序串行执行；不要在 agentIds 中塞入并行/插队语义。
                 5. 不支持只传单个 agentId 走老路径；agent_selector 始终按数组处理。
                 6. 调用完 agent_selector 工具后不要再调用其它工具，等待子Agent执行结束即可。

            """;

    String DEFAULT_ATTACHMENT_FILE_PROMPT = """
            User uploaded document name: 《%s》.
            The document content:
            
            %s
            
            """;
}
