from langchain.chat_models import init_chat_model
from langchain_openai import ChatOpenAI

llm = ChatOpenAI(
    base_url="https://dashscope-intl.aliyuncs.com/compatible-mode/v1",
    api_key="sk-0543377485b74778b3728e250df4e7d1",
    model="qwen3.7-plus",
    extra_body={"chat_template_kwargs": {"enable_thinking": False}},
)

mixmax_llm = ChatOpenAI(
    model="MiniMax-M3",
    base_url="https://api.minimaxi.com/v1",
    api_key="sk-cp-NJAqUyIzyZQ3w1rJNI1-ChHzpr-7VDZSAmV6jljLBMM63r74TgJ-sw129pewZUDjDm-9swwJ6pWiQplItsGxQfaSZIs34GTtNEb5usrrrWU6PDWZ3yf9iAQ",
)

res = llm.invoke("hi")
print(res)