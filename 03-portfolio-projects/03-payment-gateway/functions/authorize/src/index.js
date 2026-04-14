const { v4: uuidv4 } = require('uuid');
const { db } = require('../../shared/src/dynamodb');
const { withIdempotency } = require('../../shared/src/idempotency');
const { success, error, validateRequired } = require('../../shared/src/response');

/**
 * Authorize Payment — Hold funds on the customer's card.
 *
 * POST /payments/authorize
 * Headers: X-Idempotency-Key: <client-generated-key>
 * Body: { merchantId, customerId, amount, currency, cardToken }
 *
 * This does NOT transfer money — it only places a hold.
 * The hold expires after the capture window (default: 7 days).
 */
exports.handler = async (event) => {
  try {
    const body = JSON.parse(event.body || '{}');
    const idempotencyKey = event.headers['x-idempotency-key'] || event.headers['X-Idempotency-Key'];

    if (!idempotencyKey) {
      return error('X-Idempotency-Key header is required', 400, 'MISSING_IDEMPOTENCY_KEY');
    }

    validateRequired(body, ['merchantId', 'customerId', 'amount', 'currency', 'cardToken']);

    // Fraud checks
    if (body.amount > 50000) {
      return error('Amount exceeds authorization limit', 400, 'AMOUNT_LIMIT_EXCEEDED');
    }

    const paymentId = `pay_${uuidv4().replace(/-/g, '').substring(0, 16)}`;

    const result = await withIdempotency(paymentId, idempotencyKey, async () => {
      // Simulate card network authorization
      const authCode = `AUTH_${uuidv4().substring(0, 8).toUpperCase()}`;

      const payment = {
        PK: `PAY#${paymentId}`,
        SK: 'META',
        paymentId,
        merchantId: body.merchantId,
        customerId: body.customerId,
        amount: body.amount,
        currency: body.currency,
        status: 'AUTHORIZED',
        authCode,
        captureDeadline: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
        createdAt: new Date().toISOString(),
        GSI1PK: `MERCHANT#${body.merchantId}`,
        GSI1SK: `PAY#${paymentId}`,
      };

      await db.putItem(payment);

      // Create ledger entries (double-entry)
      await db.putItem({
        PK: `PAY#${paymentId}`,
        SK: 'LEDGER#hold_debit',
        type: 'HOLD_DEBIT',
        account: `customer:${body.customerId}`,
        amount: body.amount,
        currency: body.currency,
        createdAt: new Date().toISOString(),
      });

      await db.putItem({
        PK: `PAY#${paymentId}`,
        SK: 'LEDGER#hold_credit',
        type: 'HOLD_CREDIT',
        account: `merchant_pending:${body.merchantId}`,
        amount: body.amount,
        currency: body.currency,
        createdAt: new Date().toISOString(),
      });

      return {
        paymentId,
        status: 'AUTHORIZED',
        authCode,
        amount: body.amount,
        currency: body.currency,
        captureDeadline: payment.captureDeadline,
      };
    });

    if (result.cached) {
      return success(result.response, 200); // Idempotent replay
    }

    return success(result.response, 201);
  } catch (err) {
    console.error('Authorization failed:', err);
    if (err.statusCode) {
      return error(err.message, err.statusCode, err.name);
    }
    return error('Authorization failed', 500);
  }
};
