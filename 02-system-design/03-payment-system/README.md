# 💳 Design Payment System (Stripe-like)

---

## Requirements
- Process payments (authorize, capture, refund)
- Multi-currency support
- Idempotent API
- Fraud detection
- Webhook notifications
- PCI DSS compliance

## Architecture

```
Client → API Gateway → Payment API → Payment Processor
                           │              │
                     ┌─────┼─────┐   ┌───▼────┐
                     │     │     │   │ Card    │
                   Auth  Fraud  │   │ Network │
                   Svc   Svc   │   │(Visa/MC)│
                          │     │   └────────┘
                     ┌────▼────┐│
                     │ Risk    ││
                     │ Engine  ││
                     └─────────┘│
                           │
                     ┌─────▼─────┐
                     │ Ledger DB │ (Double-entry bookkeeping)
                     └───────────┘
```

## Key Design Decisions

### Idempotency
- Client sends `Idempotency-Key` header
- Server stores key → response mapping
- Duplicate request returns cached response
- TTL: 24 hours

### Payment Flow
```
1. Authorize → Hold funds on card (no transfer)
2. Capture → Transfer funds (can be partial)
3. Settle → Batch settlement with bank (T+1 or T+2)
4. Refund → Reverse the capture (full or partial)
```

### Double-Entry Ledger
Every transaction creates two entries:
```
Debit:  Customer Account  -$100
Credit: Merchant Account  +$100
```
Ensures books always balance. Critical for reconciliation.

### Fraud Detection
- Rule-based (velocity checks, geo-mismatch, amount thresholds)
- ML-based (transaction scoring model)
- 3D Secure for high-risk transactions

### Reconciliation
- Daily batch job compares internal ledger with bank statements
- Flag discrepancies for manual review
- Automated retry for failed settlements

## Interview Talking Points
1. Why idempotency is critical (network retries = duplicate charges)
2. Authorize vs Capture (two-step for flexibility)
3. Double-entry bookkeeping (financial accuracy)
4. PCI compliance (tokenization, never store raw card numbers)
5. Eventual consistency (settlement is T+1/T+2)
