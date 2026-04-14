# 💳 Stripe Interview Guide — Lead/Staff Engineer

> Focus: Payment Systems, API Design, Reliability

---

## Interview Structure
| Round | Focus |
|-------|-------|
| Phone Screen | Coding (practical, not LeetCode) |
| Onsite 1 | System Design (payment-focused) |
| Onsite 2 | Coding (bug investigation / debugging) |
| Onsite 3 | Integration Design (API design) |
| Onsite 4 | Collaboration / Manager |

## Stripe-Specific Focus Areas

### Payment Systems Knowledge
- **Idempotency** — Critical for payment APIs (duplicate charge prevention)
- **Distributed transactions** — Saga pattern for multi-step payment flows
- **Reconciliation** — Matching internal records with bank statements
- **PCI compliance** — Tokenization, encryption, audit trails
- **Webhook reliability** — At-least-once delivery, retry with backoff

### API Design (Stripe's Strength)
- RESTful, consistent, developer-friendly APIs
- Versioning strategy (date-based: `2024-01-01`)
- Error handling (structured error codes)
- Pagination (cursor-based)
- Idempotency keys

### System Design Questions
1. **Design a Payment Processing System** — Authorization, capture, refund
2. **Design a Subscription Billing System** — Recurring payments, proration
3. **Design a Fraud Detection System** — Real-time scoring, ML models
4. **Design a Webhook Delivery System** — Reliable, ordered, retryable

## Coding Style
- Stripe values **practical coding** over algorithm puzzles
- Expect debugging exercises (find the bug in this code)
- Clean, readable code > clever code
- Error handling and edge cases matter a lot

## Preparation Checklist
- [ ] Understand payment flow (auth → capture → settle)
- [ ] Study Stripe's API documentation (stripe.com/docs)
- [ ] Practice API design exercises
- [ ] Review idempotency patterns deeply
- [ ] Prepare stories about building reliable systems
- [ ] Understand PCI DSS basics
