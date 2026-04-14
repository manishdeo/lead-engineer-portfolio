# 🤖 AI & GenAI Interview Questions (100+)

---

### Q1: What is RAG? Why use it over fine-tuning?
**A:** Retrieval-Augmented Generation — retrieve relevant documents, inject into LLM prompt. Use RAG when: data changes frequently, need citations, don't want training costs. Use fine-tuning when: need specific style/format, domain-specific terminology, consistent behavior.

### Q2: How do you evaluate RAG system quality?
**A:** Metrics: Faithfulness (grounded in context?), Answer Relevance (addresses query?), Context Precision (right docs retrieved?), Context Recall (all relevant docs found?). Tools: RAGAS framework, LangSmith.

### Q3: How do you handle hallucinations?
**A:** Ground in retrieved context, low temperature, "only answer from provided context" instruction, citation tracking, confidence scoring, human-in-the-loop for critical decisions.

### Q4: What is prompt injection? How do you prevent it?
**A:** User crafts input to override system prompt. Prevention: input sanitization, separate system/user messages, output filtering, guardrails (NeMo Guardrails), content moderation API.

### Q5: How do you choose between different LLM providers?
**A:** Consider: cost per token, latency, context window, quality for your use case, data privacy requirements, rate limits. Benchmark on your specific tasks. Use LLM router for cost optimization.
