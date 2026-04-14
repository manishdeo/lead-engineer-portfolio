import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as apigateway from 'aws-cdk-lib/aws-apigateway';
import * as dynamodb from 'aws-cdk-lib/aws-dynamodb';

/**
 * AWS CDK Stack representing a typical Event-Driven Serverless Architecture
 * This stack provisions an API Gateway, Lambda Function, and DynamoDB Table.
 */
export class ServerlessAppStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // 1. Database: DynamoDB Table for scalable, serverless NoSQL storage
    const usersTable = new dynamodb.Table(this, 'UsersTable', {
      partitionKey: { name: 'userId', type: dynamodb.AttributeType.STRING },
      billingMode: dynamodb.BillingMode.PAY_PER_REQUEST,
      removalPolicy: cdk.RemovalPolicy.DESTROY,
    });

    // 2. Compute: AWS Lambda Function handling business logic
    const userHandler = new lambda.Function(this, 'UserHandler', {
      runtime: lambda.Runtime.NODEJS_18_X,
      code: lambda.Code.fromAsset('lambda'),
      handler: 'index.handler',
      environment: {
        TABLE_NAME: usersTable.tableName,
      },
    });

    // Grant Lambda permissions to read/write to DynamoDB
    usersTable.grantReadWriteData(userHandler);

    // 3. API Routing: API Gateway to expose the Lambda as a REST API
    const api = new apigateway.RestApi(this, 'UsersApi', {
      restApiName: 'Users Service',
      description: 'API to manage users',
    });

    const usersIntegration = new apigateway.LambdaIntegration(userHandler);
    
    // Setup routes
    const usersResource = api.root.addResource('users');
    usersResource.addMethod('GET', usersIntegration);  // GET /users
    usersResource.addMethod('POST', usersIntegration); // POST /users
  }
}
