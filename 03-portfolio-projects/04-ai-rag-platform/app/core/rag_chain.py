"""Core RAG pipeline — orchestrates retrieval, re-ranking, and generation."""

from typing import AsyncGenerator
from litellm import acompletion

from app.retrieval.hybrid_search import hybrid_search
from app.retrieval.reranker import rerank
from app.core.llm_router import select_model, count_tokens
from app.config.settings import settings

SYSTEM_PROMPT = """You are a helpful AI assistant that answers questions based on the provided context documents.

Rules:
- ONLY answer based on the provided context. If the context doesn't contain the answer, say "I don't have enough information to answer this question."
- Always cite your sources using [Source: filename, page X] format.
- Be concise and accurate.
- Never make up information not present in the context."""


async def rag_query(
    db_session,
    query: str,
    tenant_id: str,
    conversation_history: list[dict] | None = None,
) -> dict:
    """
    Full RAG pipeline:
    1. Hybrid search (vector + keyword) → top 20 candidates
    2. Re-rank with cross-encoder → top 5
    3. Build prompt with context + history
    4. Generate response with selected LLM
    """
    # Step 1: Retrieve
    candidates = await hybrid_search(db_session, query, tenant_id)

    if not candidates:
        return {
            "answer": "I couldn't find any relevant documents to answer your question.",
            "sources": [],
            "model_used": "none",
        }

    # Step 2: Re-rank
    top_chunks = rerank(query, candidates)

    # Step 3: Build context
    context = "\n\n---\n\n".join([
        f"[Source: {c['filename']}, Page {c['page_number']}]\n{c['content']}"
        for c in top_chunks
    ])

    # Step 4: Select model
    context_tokens = count_tokens(context)
    model = select_model(query, context_tokens)

    # Step 5: Build messages
    messages = [{"role": "system", "content": SYSTEM_PROMPT}]

    if conversation_history:
        recent = conversation_history[-settings.max_conversation_history:]
        messages.extend(recent)

    messages.append({
        "role": "user",
        "content": f"Context:\n{context}\n\nQuestion: {query}",
    })

    # Step 6: Generate
    response = await acompletion(model=model, messages=messages, temperature=0.1)
    answer = response.choices[0].message.content

    sources = [
        {
            "document_id": c["document_id"],
            "filename": c["filename"],
            "page_number": c["page_number"],
            "relevance_score": round(c.get("rerank_score", c["combined_score"]), 3),
            "excerpt": c["content"][:200],
        }
        for c in top_chunks
    ]

    return {"answer": answer, "sources": sources, "model_used": model}


async def rag_query_stream(
    db_session,
    query: str,
    tenant_id: str,
    conversation_history: list[dict] | None = None,
) -> AsyncGenerator[str, None]:
    """
    Streaming RAG — yields tokens as they're generated.
    Uses Server-Sent Events (SSE) for real-time delivery.
    """
    candidates = await hybrid_search(db_session, query, tenant_id)
    top_chunks = rerank(query, candidates) if candidates else []

    context = "\n\n---\n\n".join([
        f"[Source: {c['filename']}, Page {c['page_number']}]\n{c['content']}"
        for c in top_chunks
    ])

    context_tokens = count_tokens(context)
    model = select_model(query, context_tokens)

    messages = [
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "user", "content": f"Context:\n{context}\n\nQuestion: {query}"},
    ]

    if conversation_history:
        messages[1:1] = conversation_history[-settings.max_conversation_history:]

    response = await acompletion(model=model, messages=messages, temperature=0.1, stream=True)

    async for chunk in response:
        delta = chunk.choices[0].delta
        if delta and delta.content:
            yield delta.content
