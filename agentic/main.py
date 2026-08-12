from fastapi import FastAPI, Request, HTTPException, Depends
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
