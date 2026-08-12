from fastapi import FastAPI, Request, Response, HTTPException, Header, Depends
from typing import Optional, List, Dict, Any
from contextlib import asynccontextmanager
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
import os
import jwt

from grpc_client import init_grpc_client, close_grpc_client, get_grpc_client

# JWT 密钥（与 Java 端 token.secret 配置一致）
JWT_SECRET = os.environ.get("JWT_SECRET", "abcdefghijklmnopqrstuvwxyz")

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

bearer_security = HTTPBearer(auto_error=False)


async def verify_token(
    request: Request,
    response: Response,
    auth_token: HTTPAuthorizationCredentials = Depends(bearer_security),
):
    """
    验证 JWT Token 并提取用户信息
    
    JWT claims 结构（与 Java TokenService 一致）:
    - login_user_key: 用户会话 UUID
    - sub: 用户名（即 user_id）
    """
    if auth_token is None:
        raise HTTPException(status_code=401, detail="未提供认证令牌")
    
    token = auth_token.credentials
    try:
        # 使用 HS512 算法解析 JWT（与 Java 端 SignatureAlgorithm.HS512 一致）
        claims = jwt.decode(token, JWT_SECRET, algorithms=["HS512"])
        
        # 提取用户信息
        user_id = claims.get("sub")  # 用户名（对应 Java 端 Claims.SUBJECT）
        login_user_key = claims.get("login_user_key")  # 会话 UUID
        
        if user_id is None:
            raise HTTPException(status_code=401, detail="令牌中缺少用户信息")
        
        return {"user_id": user_id, "login_user_key": login_user_key}
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=401, detail="令牌已过期")
    except jwt.InvalidTokenError as e:
        raise HTTPException(status_code=401, detail=f"无效的令牌: {str(e)}")


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
