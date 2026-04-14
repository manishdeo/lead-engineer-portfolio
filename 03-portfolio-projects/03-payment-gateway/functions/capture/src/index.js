const { db } = require('../../shared/src/dynamodb');
const { success, error, validateRequired } = require('../../shared/src/response');

/**
 * Capture Payment — Transfer the authorized funds.
 *
 * POST /payments/{paymentId}/capture
 * Body: { amount } (optional — for partial capture)
 *
 * Can only capture an AUTHORIZED payment within the capture window.
 */
exports.handler = async (event) => {
  try {
    const paymentId = event.pathParameters?.paymentId;
    const body = JSON.parse(event.body || '{}');

    if (!paymentId) {
      return error('paymentId is required', 400);
    }

    // Get payment
    const payment = await db.getItem(`PAY#${paymentId}`, 'META');
    if (!payment) {
      return error('Payment not found', 404, 'PAYMENT_NOT_FOUND');
    }

    if (payment.status !== 'AUTHORIZED') {
      return error(`Cannot capture payment in ${payment.status} status`, 400, 'INVALID_STATUS');
    }

    // Check capture window
    if (new Date(payment.captureDeadline) < new Date()) {
      return error('Capture window expired', 400, 'CAPTURE_EXPIRED');
    }

    // Support partial capture
    const captureAmount = body.amount || payment.amount;
    if (captureAmount > payment.amount) {
      return error('Capture amount exceeds authorized amount', 400, 'AMOUNT_EXCEEDED');
    }

    // Update payment status
    const updated = await db.updateItem(
      `PAY#${paymentId}`, 'META',
      'SET #status = :status, capturedAmount = :amount, capturedAt = :ts',
      {
        ':status': 'CAPTURED',
        ':amount': captureAmount,
        ':ts': new Date().toISOString(),
      },
      '#status = :expectedStatus',
    );

    // Ledger entries for capture
    await db.putItem({
      PK: `PAY#${paymentId}`,
      SK: 'LEDGER#capture_debit',
      type: 'CAPTURE_DEBIT',
      account: `customer:${payment.customerId}`,
      amount: captureAmount,
      currency: payment.currency,
      createdAt: new Date().toISOString(),
    });

    await db.putItem({
      PK: `PAY#${paymentId}`,
      SK: 'LEDGER#capture_credit',
      type: 'CAPTURE_CREDIT',
      account: `merchant:${payment.merchantId}`,
      amount: captureAmount,
      currency: payment.currency,
      createdAt: new Date().toISOString(),
    });

    return success({
      paymentId,
      status: 'CAPTURED',
      capturedAmount: captureAmount,
      currency: payment.currency,
    });
  } catch (err) {
    console.error('Capture failed:', err);
    return error('Capture failed', 500);
  }
};
