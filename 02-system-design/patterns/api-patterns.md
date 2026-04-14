# 🔌 API Design Patterns

---

## REST API Best Practices

### URL Design
```
GET    /api/v1/orders              # List orders
POST   /api/v1/orders              # Create order
GET    /api/v1/orders/{id}         # Get order
PUT    /api/v1/orders/{id}         # Full update
PATCH  /api/v1/orders/{id}         # Partial update
DELETE /api/v1/orders/{id}         # Delete order
GET    /api/v1/orders/{id}/items   # Sub-resource
```

### Pagination
```json
// Cursor-based (recommended for large datasets)
GET /api/v1/orders?cursor=eyJpZCI6MTAwfQ&limit=20

{
  "data": [...],
  "pagination": {
    "next_cursor": "eyJpZCI6MTIwfQ",
    "has_more": true
  }
}

// Offset-based (simpler, but slow for deep pages)
GET /api/v1/orders?page=5&size=20
```

### Filtering & Sorting
```
GET /api/v1/orders?status=CONFIRMED&min_amount=100&sort=-created_at
```

### Error Responses (RFC 7807 Problem Details)
```json
{
  "type": "https://api.example.com/errors/insufficient-funds",
  "title": "Insufficient Funds",
  "status": 422,
  "detail": "Account balance $50.00 is less than order total $99.99",
  "instance": "/api/v1/orders/123"
}
```

---

## API Versioning

| Strategy | Example | Pros | Cons |
|----------|---------|------|------|
| **URI path** | `/api/v2/orders` | Explicit, cacheable | URL pollution |
| **Header** | `Accept: application/vnd.api.v2+json` | Clean URLs | Hidden |
| **Query param** | `/api/orders?version=2` | Easy to test | Not RESTful |
| **Date-based** (Stripe) | `Stripe-Version: 2024-01-01` | Granular | Complex |

**Recommendation:** URI path for public APIs, header for internal.

---

## Idempotency

Critical for payment APIs and any non-safe operation.

```
Client → POST /payments { Idempotency-Key: "abc123" }
Server:
  1. Check if key exists in store
  2. If exists → return cached response
  3. If not → process, store response with key, return
```

### Implementation
```java
@PostMapping("/payments")
public ResponseEntity<?> createPayment(
    @RequestHeader("Idempotency-Key") String key,
    @RequestBody PaymentRequest request) {

    return idempotencyService.execute(key, () -> {
        return paymentService.process(request);
    });
}
```

---

## Rate Limiting

### Algorithms
| Algorithm | Behavior | Best For |
|-----------|----------|----------|
| **Token Bucket** | Allows bursts up to bucket size | Most APIs |
| **Sliding Window** | Smooth rate limiting | Strict rate control |
| **Fixed Window** | Simple, burst at boundary | Simple use cases |
| **Leaky Bucket** | Constant output rate | Traffic shaping |

### Response Headers
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 42
X-RateLimit-Reset: 1704067200
Retry-After: 30
```

---

## API Gateway Patterns

```
Client → API Gateway → Microservices
              │
    ┌─────────┼─────────┐
    │         │         │
  Auth    Rate Limit  Routing
  │         │         │
  Cache   Transform  Circuit
                     Breaker
```

### Backend for Frontend (BFF)
```
Mobile App  → Mobile BFF  → Microservices
Web App     → Web BFF     → Microservices
Partner API → Partner BFF → Microservices
```
Each BFF tailored to its client's needs.

---

## GraphQL vs REST vs gRPC

| Aspect | REST | GraphQL | gRPC |
|--------|------|---------|------|
| Protocol | HTTP/JSON | HTTP/JSON | HTTP/2 + Protobuf |
| Schema | OpenAPI (optional) | Required (SDL) | Required (.proto) |
| Over-fetching | Common | Solved | N/A |
| Streaming | SSE/WebSocket | Subscriptions | Bidirectional |
| Best for | Public APIs | Mobile/flexible clients | Internal high-perf |

---

## 2025 Trends

- **OpenAPI 3.1** — Full JSON Schema compatibility
- **AsyncAPI 3.0** — Standard for event-driven API docs
- **API-first development** — Design API before implementation
- **AI-powered APIs** — Streaming responses (SSE), function calling schemas
- **API observability** — OpenTelemetry for API metrics/traces
