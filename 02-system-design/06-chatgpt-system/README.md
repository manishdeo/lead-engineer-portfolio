# 🤖 Design ChatGPT-like AI System

---

## Requirements
- Conversational AI with context memory
- RAG for knowledge-grounded responses
- Streaming responses (token by token)
- Multi-turn conversation management
- Function calling / tool use
- Multi-tenant (SaaS)

## Architecture

```
Client ←SSE Stream── API Gateway → Chat Service → LLM Router
                                       │              │
                                  ┌────┼────┐    ┌───▼────┐
                                  │    │    │    │ GPT-4  │
                              Context RAG  │    │ Claude │
                              Manager Svc  │    │ Local  │
                                  │    │    │    └────────┘
                              Redis  Vector │
                             (History) DB   │
                                       │    │
                                  PostgreSQL
                                  (Users, Conversations)
```

## Key Components

### Context Management
```
System Prompt + Conversation History (last N turns) + RAG Context + User Message
                                                                        │
                                                                   Token Budget
                                                                   Management
```
- Sliding window: Keep last N messages within token budget
- Summarization: Compress old messages into summary
- Token counting: tiktoken library for accurate counting

### RAG Pipeline
```
User Query → Embed → Vector Search (Top-K) → Re-rank → Inject into prompt
```

### Streaming (Server-Sent Events)
```
Client ← SSE ← API ← LLM (token by token)
```
- First token latency < 500ms
- Use SSE (not WebSocket) for unidirectional streaming
- Backpressure handling for slow clients

### Function Calling
```
User: "What's the weather in Sydney?"
LLM: {tool: "get_weather", args: {city: "Sydney"}}
System: Calls weather API → Returns result
LLM: "The weather in Sydney is 25°C and sunny."
```

## Cost Optimization
- Cache frequent queries (semantic similarity matching)
- Route simple queries to cheaper models (GPT-3.5)
- Batch embeddings generation
- Token budget management per conversation

## Interview Talking Points
1. Context window management (token budgeting)
2. Streaming architecture (SSE vs WebSocket)
3. RAG vs fine-tuning trade-offs
4. Hallucination prevention (grounding, citations)
5. Multi-tenant isolation and rate limiting
6. Cost optimization at scale
