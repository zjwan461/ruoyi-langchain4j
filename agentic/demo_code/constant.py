from langchain.chat_models import init_chat_model

llm = init_chat_model(
    model="qwen3.7-plus",
    model_provider="openai",
    base_url="https://dashscope-intl.aliyuncs.com/compatible-mode/v1",
    api_key="sk-xxx",
)
