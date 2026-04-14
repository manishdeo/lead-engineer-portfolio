const { DynamoDBClient } = require('@aws-sdk/client-dynamodb');
const { DynamoDBDocumentClient, GetCommand, PutCommand, UpdateCommand, QueryCommand } = require('@aws-sdk/lib-dynamodb');

const client = new DynamoDBClient({});
const docClient = DynamoDBDocumentClient.from(client);

const TABLE_NAME = process.env.TABLE_NAME || 'PaymentGateway';

const db = {
  async getItem(pk, sk) {
    const { Item } = await docClient.send(new GetCommand({
      TableName: TABLE_NAME,
      Key: { PK: pk, SK: sk },
    }));
    return Item;
  },

  async putItem(item, conditionExpression) {
    const params = {
      TableName: TABLE_NAME,
      Item: item,
    };
    if (conditionExpression) {
      params.ConditionExpression = conditionExpression;
    }
    await docClient.send(new PutCommand(params));
  },

  async updateItem(pk, sk, updateExpression, expressionValues, conditionExpression) {
    const params = {
      TableName: TABLE_NAME,
      Key: { PK: pk, SK: sk },
      UpdateExpression: updateExpression,
      ExpressionAttributeValues: expressionValues,
      ReturnValues: 'ALL_NEW',
    };
    if (conditionExpression) {
      params.ConditionExpression = conditionExpression;
    }
    const { Attributes } = await docClient.send(new UpdateCommand(params));
    return Attributes;
  },

  async queryByPK(pk, skPrefix) {
    const { Items } = await docClient.send(new QueryCommand({
      TableName: TABLE_NAME,
      KeyConditionExpression: 'PK = :pk AND begins_with(SK, :sk)',
      ExpressionAttributeValues: { ':pk': pk, ':sk': skPrefix },
    }));
    return Items || [];
  },
};

module.exports = { db, TABLE_NAME };
