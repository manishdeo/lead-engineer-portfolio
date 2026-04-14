# 🧠 AI Engineering Patterns — Interview Reference

---

## Prompt Engineering Patterns

### 1. Zero-Shot
Direct instruction, no examples.
```
Classify this review as POSITIVE or NEGATIVE: "Great product!"
```

### 2. Few-Shot
Provide examples before the task.
```
Review: "Loved it!" → POSITIVE
Review: "Terrible quality" → NEGATIVE
Review: "Works perfectly" → ?
```

### 3. Chain-of-Thought (CoT)
Ask the model to reason step by step.
```
Solve this step by step: If a train travels 60 mph for 2.5 hours...
```

### 4. ReAct (Reasoning + Acting)
Model reasons about what tool to use, then acts.
```
Thought: I need to search for the latest stock price
Action: search("AAPL stock price")
Observation: $185.50
Thought: Now I can answer the user
Answer: Apple's stock price is $185.50
```

---

## LLM Application Architecture

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│   Client    │────▶│  API Layer   │────▶│  LLM Router │
└─────────────┘     │  (Rate Limit │     │  (Model     │
                    │   Auth, Log) │     │   Selection)│
                    └──────────────┘     └──────┬──────┘
                                                │
                    ┌───────────────────────────┼──────────┐
                    │               │           │          │
              ┌─────▼─────┐  ┌─────▼─────┐  ┌─▼────────┐│
              │  GPT-4    │  │  Claude    │  │ Local LLM ││
              │  (Complex)│  │  (Long ctx)│  │ (Private) ││
              └───────────┘  └───────────┘  └──────────┘│
                                                         │
                    ┌────────────────────────────────────┘
                    │
              ┌─────▼─────┐     ┌──────────┐
              │  Guard-   │────▶│  Output   │
              │  rails    │     │  Parser   │
              └───────────┘     └──────────┘
```

## Cost Optimization
- **Caching:** Cache identical prompts/responses (Redis)
- **Model routing:** Use cheaper models for simple tasks, expensive for complex
- **Prompt compression:** Remove unnecessary tokens
- **Batch processing:** Group requests for batch API pricing
- **Fine-tuning:** Smaller fine-tuned model can replace larger general model

## Guardrails
- Input validation (prompt injection detection)
- Output filtering (PII, harmful content)
- Token limits per user/request
- Rate limiting
- Content moderation API
