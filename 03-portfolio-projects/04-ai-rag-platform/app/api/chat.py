"""Chat API — streaming and non-streaming RAG endpoints."""

from fastapi import APIRouter, Depends
from sse_starlette.sse import EventSourceResponse

from app.models.schemas import ChatRequest, ChatResponse
from app.core.rag_chain import rag_query, rag_query_stream

router = APIRouter()


@router.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """Non-streaming chat endpoint. Returns full response."""
    # In production: inject db_session via Depends
    result = await rag_query(
        db_session=None,  # placeholder — inject via dependency
        query=request.query,
        tenant_id=request.tenant_id,
        conversation_history=None,
    )
    return ChatResponse(
        answer=result["answer"],
        sources=result["sources"],
        conversation_id=request.conversation_id or "new",
        model_used=result["model_used"],
    )


@router.post("/chat/stream")
async def chat_stream(request: ChatRequest):
    """
    Streaming chat endpoint using Server-Sent Events (SSE).

    Client receives tokens as they're generated:
    data: {"token": "The"}
    data: {"token": " answer"}
    data: {"token": " is"}
    data: [DONE]
    """
    async def event_generator():
        async for token in rag_query_stream(
            db_session=None,
            query=request.query,
            tenant_id=request.tenant_id,
        ):
            yield {"data": token}
        yield {"data": "[DONE]"}

    return EventSourceResponse(event_generator())
