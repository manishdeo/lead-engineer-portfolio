# 🛒 Design E-commerce Platform (Amazon-scale)

---

## Key Components
```
Client → CDN → API Gateway → Product Service (Elasticsearch + PostgreSQL)
                           → Cart Service (Redis)
                           → Order Service (PostgreSQL + Kafka)
                           → Payment Service (Idempotent, Saga)
                           → Inventory Service (Redis + PostgreSQL)
                           → Recommendation Service (ML + Redis)
                           → Search Service (Elasticsearch)
                           → Notification Service (Kafka → SES/SNS)
```

## Critical Design Decisions

### Product Catalog
- **Elasticsearch** for search (full-text, faceted, autocomplete)
- **PostgreSQL** as source of truth
- **Redis** for hot product cache (TTL: 5 min)
- **CDN** for product images (multiple resolutions)

### Shopping Cart
- **Redis** hash per user: `cart:{userId}` → `{productId: quantity}`
- TTL: 7 days for guest, persistent for logged-in
- Merge guest cart on login

### Inventory — Preventing Overselling
```
1. Optimistic: Check stock → Place order → Decrement (race condition risk)
2. Pessimistic: Redis DECR atomic → If >= 0, proceed → Else reject
3. Reservation: Hold stock for 10 min → Confirm on payment → Release on timeout
```
**Best:** Redis atomic decrement + PostgreSQL as source of truth + reconciliation.

### Checkout Flow (Saga)
```
Validate Cart → Reserve Inventory → Process Payment → Create Order → Send Confirmation
     ↓ fail          ↓ fail              ↓ fail
   Return         Release Stock      Refund + Release
```

## Interview Talking Points
1. **Search** — Elasticsearch inverted index, relevance scoring, autocomplete
2. **Inventory** — Race conditions, distributed locking, overselling prevention
3. **Cart** — Redis vs DB, guest merge, TTL strategy
4. **Recommendations** — Collaborative filtering, "customers also bought"
5. **Flash sales** — Queue-based, rate limiting, pre-warm cache
