from fastapi import Request, Response, Depends, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
import os

token_env = os.environ.get("TOKEN_WHITELIST", "admin_token,dev_token")
TOKEN_WHITELIST = [t.strip() for t in token_env.split(",") if t.strip()]

bearer_security = HTTPBearer(auto_error=False)


async def verify_token(
    request: Request,
    response: Response,
    auth_token: HTTPAuthorizationCredentials = Depends(bearer_security),
):
    """
    白名单 Token 校验
    """
    if auth_token is None:
        raise HTTPException(status_code=401, detail="未提供认证令牌")

    token = auth_token.credentials
    if TOKEN_WHITELIST and token in TOKEN_WHITELIST:
        return {"user_id": "admin", "login_user_key": "admin"}

    raise HTTPException(status_code=401, detail="无效的令牌")
