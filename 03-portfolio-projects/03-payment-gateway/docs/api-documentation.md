# 📖 API Documentation

## Base URL
`https://{api-id}.execute-api.{region}.amazonaws.com/prod`

## Authentication
All endpoints require API key in `x-api-key` header.

---

## POST /payments/authorize
Authorize (hold) funds on a card.

**Headers:**
- `x-api-key: <api-key>`
- `X-Idempotency-Key: <unique-key>` (required)

```json
// Request
{
  "merchantId": "m_abc123",
  "customerId": "c_xyz789",
  "amount": 9999,
  "currency": "USD",
  "cardToken": "tok_visa_4242"
}

// Response 201
{
  "paymentId": "pay_a1b2c3d4e5f6",
  "status": "AUTHORIZED",
  "authCode": "AUTH_X7K9M2P1",
  "amount": 9999,
  "currency": "USD",
  "captureDeadline": "2025-01-22T10:30:00.000Z"
}
```

---

## POST /payments/{paymentId}/capture
Capture (charge) authorized funds. Supports partial capture.

```json
// Request (optional amount for partial capture)
{ "amount": 5000 }

// Response 200
{
  "paymentId": "pay_a1b2c3d4e5f6",
  "status": "CAPTURED",
  "capturedAmount": 5000,
  "currency": "USD"
}
```

---

## POST /payments/{paymentId}/refund
Refund a captured payment. Supports partial refund.

```json
// Request
{ "amount": 2500, "reason": "Customer requested" }

// Response 200
{
  "paymentId": "pay_a1b2c3d4e5f6",
  "refundId": "ref_k8m2n4p6q8",
  "status": "PARTIALLY_REFUNDED",
  "refundAmount": 2500,
  "totalRefunded": 2500,
  "currency": "USD"
}
```

---

## Error Responses
```json
{
  "error": {
    "code": "PAYMENT_NOT_FOUND",
    "message": "Payment not found"
  }
}
```

| Code | HTTP | Description |
|------|------|-------------|
| MISSING_IDEMPOTENCY_KEY | 400 | X-Idempotency-Key header required |
| PAYMENT_NOT_FOUND | 404 | Payment ID doesn't exist |
| INVALID_STATUS | 400 | Payment not in correct state for operation |
| AMOUNT_EXCEEDED | 400 | Amount exceeds authorized/captured amount |
| CAPTURE_EXPIRED | 400 | Capture window has passed |
| CONFLICT | 409 | Duplicate request in progress |

---

## Webhook Events

Merchants receive POST requests to their registered webhook URL:

```json
{
  "event": "payment.captured",
  "paymentId": "pay_a1b2c3d4e5f6",
  "timestamp": "2025-01-15T10:30:00.000Z",
  "data": { ... }
}
```

Events: `payment.authorized`, `payment.captured`, `payment.refunded`, `payment.failed`
