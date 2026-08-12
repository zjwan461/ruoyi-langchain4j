from langchain.chat_models import init_chat_model
from langchain_openai import ChatOpenAI

llm = ChatOpenAI(
    base_url="https://dashscope-intl.aliyuncs.com/compatible-mode/v1",
    api_key="sk-0xxx",
    model="qwen3.7-plus",
    extra_body={"chat_template_kwargs": {"enable_thinking": False}},
)

mixmax_llm = ChatOpenAI(
    model="MiniMax-M3",
    base_url="https://api.minimaxi.com/v1",
    api_key="sk-cp-xxx",
)