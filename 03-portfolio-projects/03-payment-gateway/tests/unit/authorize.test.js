const { handler } = require('../../functions/authorize/src/index');

// Mock DynamoDB
jest.mock('../../shared/src/dynamodb', () => ({
  db: {
    getItem: jest.fn().mockResolvedValue(null),
    putItem: jest.fn().mockResolvedValue({}),
    updateItem: jest.fn().mockResolvedValue({}),
  },
}));

describe('Authorize Function', () => {
  const validEvent = {
    headers: { 'X-Idempotency-Key': 'test-key-123' },
    body: JSON.stringify({
      merchantId: 'm_test',
      customerId: 'c_test',
      amount: 1000,
      currency: 'USD',
      cardToken: 'tok_test',
    }),
  };

  it('should authorize a valid payment', async () => {
    const result = await handler(validEvent);
    const body = JSON.parse(result.body);

    expect(result.statusCode).toBe(201);
    expect(body.status).toBe('AUTHORIZED');
    expect(body.paymentId).toBeDefined();
    expect(body.authCode).toBeDefined();
    expect(body.amount).toBe(1000);
  });

  it('should reject missing idempotency key', async () => {
    const event = { ...validEvent, headers: {} };
    const result = await handler(event);

    expect(result.statusCode).toBe(400);
    expect(JSON.parse(result.body).error.code).toBe('MISSING_IDEMPOTENCY_KEY');
  });

  it('should reject amount exceeding limit', async () => {
    const event = {
      ...validEvent,
      body: JSON.stringify({
        merchantId: 'm_test',
        customerId: 'c_test',
        amount: 100000,
        currency: 'USD',
        cardToken: 'tok_test',
      }),
    };
    const result = await handler(event);

    expect(result.statusCode).toBe(400);
    expect(JSON.parse(result.body).error.code).toBe('AMOUNT_LIMIT_EXCEEDED');
  });

  it('should reject missing required fields', async () => {
    const event = {
      ...validEvent,
      body: JSON.stringify({ merchantId: 'm_test' }),
    };
    const result = await handler(event);

    expect(result.statusCode).toBe(400);
  });
});
