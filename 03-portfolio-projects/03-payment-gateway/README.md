# 💳 Cloud-Native Payment Gateway

> Serverless payment processing platform built with AWS Lambda, API Gateway, DynamoDB, SQS, and Step Functions.

[![AWS](https://img.shields.io/badge/AWS-Serverless-orange)]()
[![Node.js](https://img.shields.io/badge/Node.js-20-green)]()
[![License](https://img.shields.io/badge/license-MIT-blue)]()

---

## 🎯 Overview

A production-grade payment gateway handling **authorization, capture, refund, and webhook delivery** — fully serverless. Demonstrates idempotency, Step Functions orchestration, DynamoDB single-table design, and event-driven reconciliation — patterns critical for fintech interviews.

## 🏗️ Architecture

```
                         ┌──────────────────┐
                         │   API Gateway    │
                         │  (REST + Auth)   │
                         └────────┬─────────┘
                                  │
              ┌───────────────────┼───────────────────┐
              │                   │                     │
     ┌────────▼────────┐ ┌──────▼───────┐ ┌──────────▼──────────┐
     │  Authorize λ    │ │  Capture λ   │ │     Refund λ        │
     │  (Hold funds)   │ │  (Charge)    │ │  (Reverse charge)   │
     └────────┬────────┘ └──────┬───────┘ └──────────┬──────────┘
              │                  │                     │
              └──────────────────┼─────────────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │      DynamoDB           │
                    │  (Single-Table Design)  │
                    │  Payments + Idempotency │
                    └────────────┬────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │    DynamoDB Streams     │
                    └────────────┬────────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                    │
     ┌────────▼────────┐ ┌─────▼──────┐ ┌──────────▼──────────┐
     │  Webhook λ      │ │ Ledger λ   │ │  Reconciliation λ   │
     │  (Notify merch.)│ │ (Audit)    │ │  (Daily batch)      │
     └─────────────────┘ └────────────┘ └─────────────────────┘
                                                    │
     ┌──────────────────────────────────┐    ┌─────▼──────┐
     │  Step Functions                  │    │    SQS     │
     │  (Payment Orchestration)         │    │  (DLQ)     │
     │  Authorize → Capture → Notify    │    └────────────┘
     └──────────────────────────────────┘
```

## 🚀 Features

- **Payment Lifecycle** — Authorize → Capture → Settle (or Refund)
- **Idempotency** — Client-generated keys prevent duplicate charges
- **Step Functions** — Orchestrated payment flow with automatic retries
- **Single-Table DynamoDB** — Payments, idempotency keys, ledger in one table
- **Webhook Delivery** — Reliable merchant notifications with retry + DLQ
- **Reconciliation** — Daily batch job comparing internal ledger with mock bank
- **Double-Entry Ledger** — Every transaction balanced (debit + credit)
- **Fraud Rules** — Velocity checks, amount limits, geo-mismatch

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Compute | AWS Lambda (Node.js 20) |
| API | API Gateway (REST) |
| Database | DynamoDB (single-table) |
| Orchestration | Step Functions |
| Queue | SQS + DLQ |
| Events | DynamoDB Streams |
| IaC | AWS SAM / CloudFormation |
| Monitoring | CloudWatch + X-Ray |
| CI/CD | GitHub Actions |

## 📦 Project Structure

```
03-payment-gateway/
├── functions/
│   ├── authorize/        # Hold funds on card
│   ├── capture/          # Transfer funds
│   ├── refund/           # Reverse charge
│   ├── webhook/          # Notify merchants
│   └── reconciliation/   # Daily batch reconciliation
├── shared/               # Shared utilities (DynamoDB client, idempotency)
├── step-functions/       # Payment flow state machine
├── infrastructure/       # SAM/CloudFormation templates
├── tests/
├── docs/
├── template.yaml         # SAM template
└── package.json
```

## ⚡ Quick Start

```bash
# Install dependencies
npm install

# Local development with SAM
sam local start-api

# Deploy
sam build && sam deploy --guided

# Run tests
npm test
```

## 📊 Key Design Patterns

### Idempotency
```
Client sends: POST /payments { idempotency_key: "pay_abc123" }

Lambda checks DynamoDB:
  - Key exists + completed → return cached response
  - Key exists + in-progress → return 409 Conflict
  - Key not found → process payment, store result
```

### Single-Table DynamoDB Design
```
PK                    SK                    Type        Data
PAY#pay_123           META                  Payment     {amount, status, ...}
PAY#pay_123           IDEM#key_abc          Idempotency {response, ttl}
PAY#pay_123           LEDGER#debit          Ledger      {amount, account}
PAY#pay_123           LEDGER#credit         Ledger      {amount, account}
PAY#pay_123           WEBHOOK#attempt_1     Webhook     {url, status, response}
MERCHANT#m_456        PAY#pay_123           GSI1        {payment summary}
```

### Step Functions — Payment Flow
```json
Start → Authorize → Wait(capture_window) → Capture → Notify → End
           ↓ fail                             ↓ fail
         Cancel                          Refund → Notify → End
```

### Webhook Delivery (Reliable)
```
Payment Event → SQS → Webhook Lambda → Merchant URL
                         ↓ fail (after 3 retries)
                       DLQ → Alert → Manual review
```

## 📈 Performance

| Metric | Target |
|--------|--------|
| Authorization latency | < 200ms |
| Throughput | 10K payments/sec |
| Webhook delivery | < 5s (first attempt) |
| Availability | 99.99% |
| Idempotency window | 24 hours |

## 🔒 Security

- API Gateway with API keys + IAM authorization
- DynamoDB encryption at rest (AWS managed)
- Lambda in VPC for bank API calls
- No raw card numbers stored (tokenization)
- CloudTrail audit logging
- Least-privilege IAM roles per Lambda

## 🎯 Interview Talking Points

1. **Serverless vs Containers** — Why Lambda for payment processing (cost, scale-to-zero, managed)
2. **Idempotency** — Why it's critical for payments, implementation with DynamoDB conditional writes
3. **Single-table DynamoDB** — Access patterns drive schema, not normalization
4. **Step Functions vs Saga** — Step Functions = managed orchestration with built-in retry/error handling
5. **Webhook reliability** — SQS + DLQ + exponential backoff
6. **Double-entry ledger** — Financial accuracy, reconciliation
7. **Cold starts** — Provisioned concurrency for latency-sensitive paths
8. **Cost optimization** — Pay-per-invocation vs always-on containers

## 📖 Documentation

- [Architecture Decisions](./ARCHITECTURE.md)
- [API Documentation](./docs/api-documentation.md)

## 📄 License

MIT License
