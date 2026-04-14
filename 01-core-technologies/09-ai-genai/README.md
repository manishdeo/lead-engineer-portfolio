# 🤖 LLM & RAG Systems — Interview Reference

---

## RAG Architecture (Retrieval-Augmented Generation)

```
User Query → Embedding Model → Vector Search → Top-K Documents
                                                      │
                                              ┌───────▼───────┐
                                              │  LLM Prompt    │
                                              │  = System Msg  │
                                              │  + Context     │
                                              │  + User Query  │
                                              └───────┬───────┘
                                                      │
                                              ┌───────▼───────┐
                                              │   LLM (GPT-4  │
                                              │   / Claude)    │
                                              └───────┬───────┘
                                                      │
                                                  Response
```

## Key Concepts

| Concept | Description |
|---------|-------------|
| **Embeddings** | Dense vector representation of text (1536 dims for OpenAI) |
| **Vector DB** | Stores and searches embeddings (Pinecone, Weaviate, pgvector) |
| **Chunking** | Split documents into optimal-size pieces (500-1000 tokens) |
| **Top-K Retrieval** | Find K most similar documents via cosine similarity |
| **Context Window** | Max tokens LLM can process (128K for GPT-4 Turbo) |
| **Temperature** | Controls randomness (0 = deterministic, 1 = creative) |
| **Function Calling** | LLM invokes external tools/APIs |
| **Fine-tuning** | Train model on domain-specific data |
| **Prompt Engineering** | Crafting effective prompts for desired output |

## Vector Database Comparison

| DB | Type | Best For |
|----|------|----------|
| Pinecone | Managed | Production RAG, easy to use |
| Weaviate | Open-source | Hybrid search (vector + keyword) |
| pgvector | PostgreSQL extension | Already using PostgreSQL |
| Chroma | Open-source | Prototyping, local development |
| Qdrant | Open-source | High performance, filtering |

## LLM Integration Patterns

### 1. Simple RAG
Query → Retrieve → Generate. Good for Q&A over documents.

### 2. Agentic RAG
LLM decides which tools to call, iterates until answer found. Good for complex multi-step tasks.

### 3. Multi-Agent
Multiple specialized agents collaborate. Good for complex workflows (research + write + review).

## Interview Questions

**Q: How do you prevent hallucinations in RAG?**
A: Ground responses in retrieved context, use low temperature, add "only answer from provided context" instruction, implement citation/source tracking.

**Q: How do you evaluate RAG quality?**
A: Metrics — Faithfulness (grounded in context?), Relevance (answers the question?), Context Precision (right docs retrieved?). Tools: RAGAS, LangSmith.

**Q: How do you handle large documents that exceed context window?**
A: Chunking strategies — fixed-size, semantic (by paragraph/section), recursive. Overlap chunks by 10-20% to preserve context at boundaries.
