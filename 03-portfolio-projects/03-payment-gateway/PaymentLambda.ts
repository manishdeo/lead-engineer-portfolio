import { APIGatewayProxyEvent, APIGatewayProxyResult } from 'aws-lambda';
import { DynamoDBClient } from '@aws-sdk/client-dynamodb';
import { DynamoDBDocumentClient, PutCommand, GetCommand } from '@aws-sdk/lib-dynamodb';

const client = new DynamoDBClient({});
const docClient = DynamoDBDocumentClient.from(client);
const TABLE_NAME = process.env.TABLE_NAME || 'Payments';

/**
 * Serverless Payment Gateway Lambda function.
 * Demonstrates handling idempotent requests using DynamoDB.
 */
export const handler = async (event: APIGatewayProxyEvent): Promise<APIGatewayProxyResult> => {
    try {
        const body = JSON.parse(event.body || '{}');
        const idempotencyKey = event.headers['Idempotency-Key'] || event.headers['idempotency-key'];

        if (!idempotencyKey) {
            return { statusCode: 400, body: JSON.stringify({ error: 'Idempotency-Key header is required' }) };
        }

        // 1. Check if we've seen this idempotency key before
        const getCmd = new GetCommand({
            TableName: TABLE_NAME,
            Key: { idempotencyKey }
        });
        const existingRecord = await docClient.send(getCmd);

        if (existingRecord.Item) {
            // Return the cached response to ensure idempotency
            return {
                statusCode: existingRecord.Item.statusCode,
                body: JSON.stringify(existingRecord.Item.responseBody)
            };
        }

        // 2. Process the payment (Mocked)
        const paymentResult = processPayment(body.amount, body.currency);
        
        // Prepare the response
        const responseBody = { status: paymentResult ? 'SUCCESS' : 'FAILED', transactionId: 'TXN-' + Date.now() };
        const statusCode = paymentResult ? 200 : 402;

        // 3. Store the result against the idempotency key for future retries
        const putCmd = new PutCommand({
            TableName: TABLE_NAME,
            Item: {
                idempotencyKey,
                statusCode,
                responseBody,
                ttl: Math.floor(Date.now() / 1000) + (24 * 60 * 60) // Expire after 24 hours
            }
        });
        await docClient.send(putCmd);

        return { statusCode, body: JSON.stringify(responseBody) };

    } catch (error) {
        console.error('Error processing payment:', error);
        return { statusCode: 500, body: JSON.stringify({ error: 'Internal Server Error' }) };
    }
};

function processPayment(amount: number, currency: string): boolean {
    // Integrate with Stripe/Braintree etc. here
    return true; // Mock success
}
