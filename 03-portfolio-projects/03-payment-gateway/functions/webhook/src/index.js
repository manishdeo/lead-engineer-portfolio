const { db } = require('../../shared/src/dynamodb');

/**
 * Webhook Delivery — Notify merchants of payment events.
 *
 * Triggered by SQS queue (fed by DynamoDB Streams).
 * Retries 3 times with exponential backoff.
 * Failed messages go to DLQ for manual review.
 */
exports.handler = async (event) => {
  const results = [];

  for (const record of event.Records) {
    const message = JSON.parse(record.body);
    const { paymentId, merchantId, eventType, webhookUrl } = message;

    const attemptNumber = (record.attributes?.ApproximateReceiveCount || 1);

    try {
      // Simulate webhook delivery to merchant
      const response = await deliverWebhook(webhookUrl, {
        event: eventType,
        paymentId,
        timestamp: new Date().toISOString(),
        data: message.data,
      });

      // Log successful delivery
      await db.putItem({
        PK: `PAY#${paymentId}`,
        SK: `WEBHOOK#attempt_${attemptNumber}`,
        merchantId,
        eventType,
        status: 'DELIVERED',
        statusCode: response.statusCode,
        attemptNumber: parseInt(attemptNumber),
        deliveredAt: new Date().toISOString(),
      });

      console.log(`Webhook delivered: paymentId=${paymentId}, attempt=${attemptNumber}`);
      results.push({ paymentId, status: 'delivered' });
    } catch (err) {
      console.error(`Webhook failed: paymentId=${paymentId}, attempt=${attemptNumber}`, err.message);

      await db.putItem({
        PK: `PAY#${paymentId}`,
        SK: `WEBHOOK#attempt_${attemptNumber}`,
        merchantId,
        eventType,
        status: 'FAILED',
        error: err.message,
        attemptNumber: parseInt(attemptNumber),
        failedAt: new Date().toISOString(),
      });

      // Throw to trigger SQS retry (up to maxReceiveCount, then DLQ)
      throw err;
    }
  }

  return { results };
};

async function deliverWebhook(url, payload) {
  // In production: use fetch/axios to POST to merchant's webhook URL
  // Simulate: 90% success rate
  if (Math.random() < 0.1) {
    throw new Error('Webhook endpoint returned 500');
  }
  return { statusCode: 200 };
}
