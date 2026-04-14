const { db } = require('./dynamodb');

/**
 * Idempotency handler using DynamoDB conditional writes.
 *
 * Flow:
 * 1. Check if idempotency key exists
 * 2. If exists + completed → return cached response
 * 3. If exists + in-progress → throw 409 Conflict
 * 4. If not found → create lock, execute handler, store result
 */
async function withIdempotency(paymentId, idempotencyKey, handler) {
  const pk = `PAY#${paymentId}`;
  const sk = `IDEM#${idempotencyKey}`;

  // Check existing
  const existing = await db.getItem(pk, sk);
  if (existing) {
    if (existing.status === 'COMPLETED') {
      return { cached: true, response: existing.response };
    }
    if (existing.status === 'IN_PROGRESS') {
      throw new ConflictError('Payment already in progress');
    }
  }

  // Acquire lock with conditional write
  const ttl = Math.floor(Date.now() / 1000) + 86400; // 24h TTL
  try {
    await db.putItem({
      PK: pk,
      SK: sk,
      status: 'IN_PROGRESS',
      createdAt: new Date().toISOString(),
      ttl,
    }, 'attribute_not_exists(PK)');
  } catch (err) {
    if (err.name === 'ConditionalCheckFailedException') {
      // Race condition: another request got there first
      const retry = await db.getItem(pk, sk);
      if (retry?.status === 'COMPLETED') {
        return { cached: true, response: retry.response };
      }
      throw new ConflictError('Payment already in progress');
    }
    throw err;
  }

  // Execute the actual payment logic
  try {
    const response = await handler();

    // Store result
    await db.updateItem(pk, sk,
      'SET #status = :status, #response = :response',
      { ':status': 'COMPLETED', ':response': response },
    );

    return { cached: false, response };
  } catch (err) {
    // Mark as failed so it can be retried
    await db.updateItem(pk, sk,
      'SET #status = :status, #error = :error',
      { ':status': 'FAILED', ':error': err.message },
    );
    throw err;
  }
}

class ConflictError extends Error {
  constructor(message) {
    super(message);
    this.name = 'ConflictError';
    this.statusCode = 409;
  }
}

module.exports = { withIdempotency, ConflictError };
