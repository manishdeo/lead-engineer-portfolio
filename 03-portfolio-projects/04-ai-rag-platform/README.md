# 🧠 AI-Powered Document Search (RAG Platform)

> Production-ready Retrieval-Augmented Generation platform with LangChain, pgvector, streaming responses, and MCP tool integration.

[![Python](https://img.shields.io/badge/Python-3.12-blue)]()
[![FastAPI](https://img.shields.io/badge/FastAPI-0.110-green)]()
[![License](https://img.shields.io/badge/license-MIT-blue)]()

---

## 🎯 Overview

A multi-tenant RAG system that lets users **chat with their documents**. Upload PDFs/docs → automatic chunking & embedding → semantic search → LLM-grounded answers with citations. Demonstrates the full AI engineering stack interviewers ask about in 2025.

## 🏗️ Architecture

```
                    ┌──────────────┐
                    │   Client     │
                    │  (React UI)  │
                    └──────┬───────┘
                           │ SSE (streaming)
                    ┌──────▼───────┐
                    │   FastAPI    │
                    │  API Layer   │
                    └──────┬───────┘
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                   │
  ┌──────▼──────┐  ┌──────▼──────┐  ┌────────▼────────┐
  │  Ingestion  │  │  Retrieval  │  │   Generation    │
  │  Pipeline   │  │  Engine     │  │   (LLM)         │
  └──────┬──────┘  └──────┬──────┘  └────────┬────────┘
         │                │                    │
  ┌──────▼──────┐  ┌──────▼──────┐  ┌────────▼────────┐
  │  Chunking   │  │  Hybrid     │  │  LLM Router     │
  │  + Embedding│  │  Search     │  │  (GPT-4/Claude) │
  └──────┬──────┘  │ Vector+BM25 │  └────────┬────────┘
         │         └──────┬──────┘           │
         │                │                   │
  ┌──────▼────────────────▼───────────────────▼──┐
  │              PostgreSQL + pgvector            │
  │         (Documents, Chunks, Embeddings)       │
  └──────────────────────┬───────────────────────┘
                         │
                  ┌──────▼──────┐
                  │    Redis    │
                  │ (Cache +    │
                  │  Sessions)  │
                  └─────────────┘
```

## 🚀 Features

- **Document Ingestion** — PDF, DOCX, TXT, Markdown with automatic chunking
- **Hybrid Search** — Vector similarity (pgvector) + BM25 keyword search
- **Re-ranking** — Cross-encoder re-ranking for better relevance
- **Streaming Responses** — Token-by-token SSE streaming
- **Citations** — Every answer grounded with source document references
- **Conversation Memory** — Multi-turn context with sliding window
- **LLM Router** — Route to GPT-4 (complex) or GPT-3.5 (simple) based on query
- **MCP Tool Integration** — Model Context Protocol for external tool calling
- **Multi-tenant** — Isolated document collections per tenant
- **Guardrails** — Prompt injection detection, PII filtering, hallucination checks

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Python 3.12 |
| Framework | FastAPI |
| LLM | OpenAI GPT-4 / Anthropic Claude (via LiteLLM) |
| Embeddings | OpenAI text-embedding-3-small |
| Vector DB | PostgreSQL + pgvector |
| Search | Hybrid (pgvector cosine + pg_trgm BM25) |
| Orchestration | LangChain 0.2 |
| Streaming | Server-Sent Events (SSE) |
| Cache | Redis (query cache + sessions) |
| Ingestion | LangChain document loaders + text splitters |
| Monitoring | OpenTelemetry + Prometheus |
| Containers | Docker, Docker Compose |

## 📦 Project Structure

```
04-ai-rag-platform/
├── app/
│   ├── api/              # FastAPI routes
│   │   ├── chat.py       # Chat endpoint (streaming)
│   │   ├── documents.py  # Document upload/management
│   │   └── health.py     # Health checks
│   ├── core/             # Business logic
│   │   ├── rag_chain.py  # RAG pipeline orchestration
│   │   └── llm_router.py # Model selection logic
│   ├── ingestion/        # Document processing
│   │   ├── chunker.py    # Text splitting strategies
│   │   └── embedder.py   # Embedding generation
│   ├── retrieval/        # Search & retrieval
│   │   ├── hybrid_search.py  # Vector + keyword search
│   │   └── reranker.py       # Cross-encoder re-ranking
│   ├── models/           # Database models
│   │   └── schemas.py    # Pydantic + SQLAlchemy models
│   ├── config/           # Configuration
│   │   └── settings.py   # Environment-based config
│   └── main.py           # FastAPI app entry point
├── tests/
├── docs/
├── docker-compose.yml
├── Dockerfile
├── requirements.txt
└── README.md
```

## ⚡ Quick Start

```bash
# Clone and setup
git clone https://github.com/<your-username>/ai-rag-platform.git
cd ai-rag-platform

# Start infrastructure
docker-compose up -d postgres redis

# Install dependencies
pip install -r requirements.txt

# Set environment variables
export OPENAI_API_KEY=<your-key>

# Run
uvicorn app.main:app --reload --port 8000
```

## 📊 Key Design Patterns

### RAG Pipeline
```
Query → Embed → Hybrid Search (Top 20) → Re-rank (Top 5) → Build Prompt → LLM → Stream Response
                                                                    │
                                                              [System Prompt]
                                                              [Context Docs]
                                                              [Chat History]
                                                              [User Query]
```

### Chunking Strategy
```
Document → Recursive Text Splitter
           chunk_size=512 tokens
           chunk_overlap=50 tokens (10%)
           separators=["\n\n", "\n", ". ", " "]
```

### LLM Router (Cost Optimization)
```python
if query_complexity(query) == "simple":
    model = "gpt-3.5-turbo"      # $0.50/1M tokens
elif query_complexity(query) == "complex":
    model = "gpt-4-turbo"        # $10/1M tokens
elif needs_long_context:
    model = "claude-3-sonnet"    # 200K context
```

### Semantic Cache
```
Query → Embed → Check Redis (cosine similarity > 0.95) → Cache Hit → Return
                                                        → Cache Miss → RAG Pipeline → Cache Result
```

## 📈 Performance

| Metric | Target |
|--------|--------|
| First token latency | < 500ms |
| Full response | < 3s (avg) |
| Retrieval latency | < 100ms |
| Ingestion throughput | 100 pages/min |
| Cache hit ratio | 30%+ |

## 🔒 Security & Guardrails

- **Prompt injection detection** — Pattern matching + classifier
- **PII filtering** — Redact sensitive data from responses
- **Hallucination check** — Verify claims against retrieved context
- **Rate limiting** — Per-tenant token budget
- **Content moderation** — Block harmful queries/responses

## 🎯 Interview Talking Points

1. **Why RAG over fine-tuning?** — Data freshness, no training cost, citations, multi-tenant
2. **Chunking strategy** — Recursive vs semantic, overlap importance, chunk size trade-offs
3. **Hybrid search** — Vector catches semantic similarity, BM25 catches exact terms
4. **Re-ranking** — Cross-encoder more accurate than bi-encoder for top-K refinement
5. **Streaming** — SSE for perceived latency reduction, backpressure handling
6. **Hallucination prevention** — Grounding, citations, confidence scoring
7. **Cost optimization** — LLM routing, semantic caching, embedding batch processing
8. **MCP (Model Context Protocol)** — Standardized tool integration for LLMs (2025 trend)
9. **Evaluation** — RAGAS metrics (faithfulness, relevance, context precision)
10. **Multi-tenancy** — Isolated vector collections, row-level security in pgvector

## 📄 License

MIT License
