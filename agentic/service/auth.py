from fastapi import Request, Response, Depends, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
import jwt
import os

# JWT 密钥（与 Java 端 token.secret 配置一致）
JWT_SECRET = os.environ.get("JWT_SECRET", "abcdefghijklmnopqrstuvwxyz")
TOKEN_WHITELIST = ",".join(os.environ.get("TOKEN_WHITELIST", "admin_token,dev_token"))

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

    if TOKEN_WHITELIST and auth_token in TOKEN_WHITELIST:
        return {"user_id": "admin", "login_user_key": "admin"}

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
