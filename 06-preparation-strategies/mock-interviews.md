# 🎤 Mock Interview Questions & Practice

---

## How to Practice

### Solo Practice (Daily)
1. Pick one system design + one behavioral question
2. Set a timer (35 min system design, 10 min behavioral)
3. Talk out loud (or record yourself)
4. Review against the solution

### With a Partner (Weekly)
1. Take turns as interviewer/candidate
2. Interviewer asks follow-ups and challenges assumptions
3. Debrief after each round

---

## Mock Interview #1: Backend-Heavy (Amazon Style)

### Round 1: System Design (45 min)
**"Design a notification system that sends 1B notifications/day across push, email, and SMS."**

Evaluation criteria:
- [ ] Clarified requirements (priority levels, delivery guarantees)
- [ ] Discussed rate limiting per user
- [ ] Chose appropriate queue (SQS/Kafka) with DLQ
- [ ] Handled template management
- [ ] Addressed delivery tracking and analytics
- [ ] Discussed retry strategy per channel

### Round 2: Behavioral (45 min)
1. "Tell me about a time you had to make a technical decision with incomplete information." (Are Right, A Lot)
2. "Describe a situation where you simplified a complex system." (Invent and Simplify)
3. "Tell me about a time you disagreed with your manager on a technical approach." (Have Backbone)

---

## Mock Interview #2: Full-Stack (Google Style)

### Round 1: System Design (45 min)
**"Design Google Docs — real-time collaborative text editing for 100M users."**

Key areas to cover:
- [ ] Conflict resolution (OT vs CRDT)
- [ ] WebSocket for real-time sync
- [ ] Document storage and versioning
- [ ] Cursor presence and awareness
- [ ] Offline support and sync
- [ ] Permission model

### Round 2: Coding (45 min)
**"Implement an LRU Cache with O(1) get and put operations."**

Follow-ups:
- Make it thread-safe
- Add TTL support
- Make it distributed (consistent hashing)

---

## Mock Interview #3: Cloud/Platform (Stripe Style)

### Round 1: System Design (45 min)
**"Design a webhook delivery system that guarantees at-least-once delivery to 100K merchant endpoints."**

Key areas:
- [ ] Retry with exponential backoff
- [ ] Dead letter queue for persistent failures
- [ ] Idempotency for consumers
- [ ] Ordering guarantees (per merchant)
- [ ] Monitoring and alerting
- [ ] Merchant-facing webhook status API

### Round 2: API Design (45 min)
**"Design the API for a subscription billing system."**

Evaluate:
- [ ] RESTful resource design
- [ ] Versioning strategy
- [ ] Idempotency handling
- [ ] Webhook events for state changes
- [ ] Error response format

---

## Mock Interview #4: AI/ML Engineering

### Round 1: System Design (45 min)
**"Design an AI-powered customer support chatbot that handles 10K concurrent conversations."**

Key areas:
- [ ] RAG architecture for knowledge base
- [ ] Context window management
- [ ] Streaming responses (SSE)
- [ ] Fallback to human agent
- [ ] Cost optimization (model routing)
- [ ] Conversation memory and history

### Round 2: Technical Deep Dive (45 min)
**"How would you evaluate and improve RAG quality in production?"**

Discuss:
- [ ] Metrics (faithfulness, relevance, context precision)
- [ ] Chunking strategies
- [ ] Re-ranking
- [ ] Feedback loops
- [ ] A/B testing different retrieval strategies

---

## Scoring Rubric

### System Design (out of 4)
| Score | Criteria |
|-------|----------|
| 4 | Comprehensive design, deep trade-off analysis, handles scale, mentions monitoring |
| 3 | Solid design, covers main components, discusses some trade-offs |
| 2 | Basic design, misses key components or scale considerations |
| 1 | Incomplete, no clear architecture, doesn't address requirements |

### Behavioral (out of 4)
| Score | Criteria |
|-------|----------|
| 4 | Specific STAR story, quantified impact, shows leadership, self-aware |
| 3 | Good story with clear situation and action, some impact mentioned |
| 2 | Vague story, missing specifics, unclear impact |
| 1 | No relevant example, generic answer |

---

## Daily Practice Schedule

| Day | System Design | Behavioral | Coding |
|-----|--------------|------------|--------|
| Mon | URL Shortener | Customer Obsession | LRU Cache |
| Tue | Chat System | Ownership | Rate Limiter |
| Wed | Payment System | Dive Deep | Consistent Hashing |
| Thu | Social Feed | Deliver Results | Producer-Consumer |
| Fri | Notification System | Hire & Develop | Circuit Breaker |
| Sat | Full mock interview (2 hours) | | |
| Sun | Review and reflect | | |
