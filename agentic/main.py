from fastapi import FastAPI, Request, HTTPException, Depends
from fastapi.responses import JSONResponse, StreamingResponse
from typing import List, Dict, Any, AsyncGenerator, Optional
from contextlib import asynccontextmanager
import os
from pydantic import BaseModel, Field

from grpc_client import init_grpc_client, close_grpc_client, get_grpc_client
from service.auth import verify_token
import random
import asyncio
import json
from logger_config import logger

# gRPC 服务端配置
GRPC_HOST = os.environ.get("GRPC_HOST", "127.0.0.1")
GRPC_PORT = int(os.environ.get("GRPC_PORT", "9090"))


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理"""
    # 启动时初始化 gRPC 客户端
    init_grpc_client(host=GRPC_HOST, port=GRPC_PORT)
    yield
    # 关闭时清理 gRPC 客户端
    close_grpc_client()


app = FastAPI(title="Agentic Agent Service", lifespan=lifespan)


class ChatReq(BaseModel):
    agent_flow_id: int = Field(description="智能体流程ID")
    prompt: str = Field(description="用户输入提示词")
    client_id: str = Field(description="客户端标识")
    session_id: str = Field(description="会话唯一ID")
    web_search: bool = Field(False, description="是否启用联网搜索，默认关闭")
    # 新增图片数组，不传默认空列表
    images: list[str] = Field(
        default_factory=list, description="图片url数组，多模态上传图片的base64"
    )


# 固定模拟大模型返回全文
FULL_ARTICLE = """人工智能Agent正在重塑办公与生产流程。
自主智能体能够理解复杂业务需求，自动调用工具、联网检索、多步骤规划完成任务。
结合向量知识库与联网搜索能力，Agent可以精准回答行业专业问题，大幅降低人工操作成本。
未来人机协同将成为常态，轻量化智能工作流会普及到企业每一个业务环节。"""


# SSE 异步生成器：每次随机截取1~2个字分片输出。mock AI响应。
async def stream_ai_response() -> AsyncGenerator[str, None]:
    # 把完整文本转为字符列表
    chars = list(FULL_ARTICLE)
    idx = 0
    while idx < len(chars):
        # 随机取1或2个字
        take_count = random.randint(1, 2)
        chunk = "".join(chars[idx : idx + take_count])
        idx += take_count

        chunk_obj = {"msg": chunk}
        chunk_str = json.dumps(chunk_obj)
        # SSE标准格式：data: {内容}\n\n
        yield f"data: {chunk_str}\n\n"
        # 模拟模型推理间隔，控制推送速度
        await asyncio.sleep(0.06)

    # 流式结束标记（前端识别流结束）
    yield "data: [DONE]\n\n"


@app.post("/chat-agentic")
async def chat_completion(
    request: Request,
    body: ChatReq,
    user=Depends(verify_token),
):
    logger.info(
        f"收到对话请求｜用户:{user}｜会话ID:{body.session_id}｜联网检索:{body.web_search}"
    )

    return StreamingResponse(
        stream_ai_response(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",  # 关闭Nginx缓冲，生产环境必备
        },
    )


@app.get("/models", response_model=List[Dict[str, Any]])
async def list_models(user=Depends(verify_token)):
    """
    获取模型列表（通过 gRPC 调用 Java 后端）
    """
    try:
        client = get_grpc_client()
        models = client.list_models()
        return models
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取模型列表失败: {str(e)}")


@app.get("/health", summary="健康检查接口")
async def health_check():
    """
    健康检查接口

    用于 Docker 容器健康检查（liveness probe）。
    返回当前服务自身状态及 gRPC 后端连接信息。
    无需鉴权，便于容器编排 / 负载均衡器直接探测。
    """
    grpc_status = "DOWN"
    grpc_error: str | None = None

    try:
        client = get_grpc_client()
        if client is not None and getattr(client, "channel", None) is not None:
            grpc_status = "UP"
    except Exception as e:
        grpc_error = str(e)

    payload = {
        "status": "UP",
        "service": "agentic-agent-service",
        "grpc": {
            "host": GRPC_HOST,
            "port": GRPC_PORT,
            "status": grpc_status,
        },
    }
    if grpc_error:
        payload["grpc"]["error"] = grpc_error

    # 只要 HTTP 进程能正常响应，就认为容器存活；
    # gRPC 后端状态作为附加信息返回，便于运维排查。
    return JSONResponse(status_code=200, content=payload)
