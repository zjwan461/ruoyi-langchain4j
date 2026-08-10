from fastapi import FastAPI, Request, Response, HTTPException, Header, Depends
from typing import Optional
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

app = FastAPI(title="Agentic Agent Service")

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
