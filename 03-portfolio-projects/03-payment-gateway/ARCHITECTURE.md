# 🏗️ Architecture Decisions

## ADR-001: Serverless over Containers

**Context:** Payment processing needs high availability and auto-scaling.

**Decision:** AWS Lambda + API Gateway over ECS/EKS.

**Rationale:**
- Scale to zero during off-peak (cost savings)
- Auto-scales to 10K+ concurrent executions
- No infrastructure management
- Built-in HA across AZs
- Pay-per-invocation aligns with transaction-based billing

**Trade-offs:**
- Cold starts (mitigated with provisioned concurrency for authorize function)
- 15-minute execution limit (fine for payment operations)
- Vendor lock-in (acceptable for this use case)

---

## ADR-002: DynamoDB Single-Table Design

**Context:** Need to store payments, idempotency keys, ledger entries, and webhook logs.

**Decision:** Single DynamoDB table with composite keys.

**Access Patterns:**
| Pattern | PK | SK |
|---------|----|----|
| Get payment | PAY#id | META |
| Check idempotency | PAY#id | IDEM#key |
| Get ledger entries | PAY#id | LEDGER# |
| Get merchant payments | MERCHANT#id (GSI1) | PAY#id |
| Get webhooks for payment | PAY#id | WEBHOOK# |

**Why single-table:** Fewer round trips, atomic transactions across entity types, cost-efficient.

---

## ADR-003: Step Functions for Payment Orchestration

**Context:** Payment flow has multiple steps with retry and compensation logic.

**Decision:** AWS Step Functions over custom Saga implementation.

**Benefits:**
- Visual workflow designer
- Built-in retry with exponential backoff
- Error handling and compensation (catch/fallback states)
- Execution history for debugging
- No custom orchestrator code to maintain

---

## ADR-004: Idempotency via DynamoDB Conditional Writes

**Context:** Network retries can cause duplicate payment charges.

**Decision:** Store idempotency key in DynamoDB with conditional write.

**Implementation:**
```
PutItem with ConditionExpression: "attribute_not_exists(PK)"
```
- If key doesn't exist → process payment, store result
- If key exists → DynamoDB throws ConditionalCheckFailedException → return cached response
- TTL: 24 hours (auto-cleanup)

---

## ADR-005: Webhook Delivery via SQS

**Context:** Merchant webhook endpoints can be unreliable.

**Decision:** SQS queue with Lambda consumer, DLQ for failures.

**Retry strategy:**
- 3 attempts with exponential backoff (1s, 4s, 16s)
- After 3 failures → message moves to DLQ
- DLQ triggers CloudWatch alarm for manual review
- Merchants can query webhook status via API

---

## ADR-006: Double-Entry Ledger

**Context:** Financial transactions require audit trail and reconciliation.

**Decision:** Every payment creates two ledger entries (debit + credit).

```
Authorize: Debit customer_hold, Credit merchant_pending
Capture:   Debit customer_account, Credit merchant_account
Refund:    Debit merchant_account, Credit customer_account
```

Sum of all debits must equal sum of all credits. Daily reconciliation job verifies this.
