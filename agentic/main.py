from fastapi import FastAPI, Request, HTTPException, Depends
from fastapi.responses import JSONResponse
from typing import List, Dict, Any
from contextlib import asynccontextmanager
import os


from grpc_client import init_grpc_client, close_grpc_client, get_grpc_client
from service.auth import verify_token

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


@app.post("/chat-agentic")
async def chat_completion(
    request: Request,
    form_data: dict,
    user=Depends(verify_token),
):
    pass


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
