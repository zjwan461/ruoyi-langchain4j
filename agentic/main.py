from fastapi import FastAPI, Request, Response, HTTPException, Header, Depends
from typing import Optional, List, Dict, Any
from contextlib import asynccontextmanager
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from grpc_client import init_grpc_client, close_grpc_client, get_grpc_client


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理"""
    # 启动时初始化 gRPC 客户端
    init_grpc_client(host="127.0.0.1", port=9090)
    yield
    # 关闭时清理 gRPC 客户端
    close_grpc_client()


app = FastAPI(title="Agentic Agent Service", lifespan=lifespan)

bearer_security = HTTPBearer(auto_error=False)


async def verify_token(
    request: Request,
    response: Response,
    auth_token: HTTPAuthorizationCredentials = Depends(bearer_security),
):
    pass


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
