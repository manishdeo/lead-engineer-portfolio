const { v4: uuidv4 } = require('uuid');
const { db } = require('../../shared/src/dynamodb');
const { success, error } = require('../../shared/src/response');

/**
 * Refund Payment — Reverse a captured payment (full or partial).
 *
 * POST /payments/{paymentId}/refund
 * Body: { amount, reason } (amount optional for full refund)
 */
exports.handler = async (event) => {
  try {
    const paymentId = event.pathParameters?.paymentId;
    const body = JSON.parse(event.body || '{}');

    const payment = await db.getItem(`PAY#${paymentId}`, 'META');
    if (!payment) {
      return error('Payment not found', 404, 'PAYMENT_NOT_FOUND');
    }

    if (payment.status !== 'CAPTURED') {
      return error(`Cannot refund payment in ${payment.status} status`, 400, 'INVALID_STATUS');
    }

    const refundAmount = body.amount || payment.capturedAmount;
    const totalRefunded = (payment.refundedAmount || 0) + refundAmount;

    if (totalRefunded > payment.capturedAmount) {
      return error('Refund amount exceeds captured amount', 400, 'REFUND_EXCEEDED');
    }

    const refundId = `ref_${uuidv4().substring(0, 12)}`;
    const isFullRefund = totalRefunded === payment.capturedAmount;

    await db.updateItem(
      `PAY#${paymentId}`, 'META',
      'SET #status = :status, refundedAmount = :refunded, lastRefundAt = :ts',
      {
        ':status': isFullRefund ? 'REFUNDED' : 'PARTIALLY_REFUNDED',
        ':refunded': totalRefunded,
        ':ts': new Date().toISOString(),
      },
    );

    // Ledger entries for refund (reverse of capture)
    await db.putItem({
      PK: `PAY#${paymentId}`,
      SK: `LEDGER#refund_debit_${refundId}`,
      type: 'REFUND_DEBIT',
      account: `merchant:${payment.merchantId}`,
      amount: refundAmount,
      currency: payment.currency,
      reason: body.reason || 'Customer requested refund',
      createdAt: new Date().toISOString(),
    });

    await db.putItem({
      PK: `PAY#${paymentId}`,
      SK: `LEDGER#refund_credit_${refundId}`,
      type: 'REFUND_CREDIT',
      account: `customer:${payment.customerId}`,
      amount: refundAmount,
      currency: payment.currency,
      createdAt: new Date().toISOString(),
    });

    return success({
      paymentId,
      refundId,
      status: isFullRefund ? 'REFUNDED' : 'PARTIALLY_REFUNDED',
      refundAmount,
      totalRefunded,
      currency: payment.currency,
    });
  } catch (err) {
    console.error('Refund failed:', err);
    return error('Refund failed', 500);
  }
};
