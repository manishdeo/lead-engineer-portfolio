const WebSocket = require('ws');
const { Kafka } = require('kafkajs');

/**
 * Real-time Analytics Dashboard WebSocket Server.
 * Reads aggregated metrics from Kafka Streams and pushes them to connected React clients.
 */
const wss = new WebSocket.Server({ port: 8080 });

const kafka = new Kafka({
  clientId: 'dashboard-service',
  brokers: ['localhost:9092']
});

const consumer = kafka.consumer({ groupId: 'dashboard-group' });

wss.on('connection', function connection(ws) {
  console.log('React Client connected to WebSocket server.');

  ws.on('close', function close() {
    console.log('Client disconnected.');
  });
});

async function run() {
  await consumer.connect();
  
  // Subscribe to the topic where Flink/Kafka Streams writes aggregated results
  await consumer.subscribe({ topic: 'analytics-results-topic', fromBeginning: false });

  await consumer.run({
    eachMessage: async ({ topic, partition, message }) => {
      const payload = message.value.toString();
      console.log(`Received analytics update: ${payload}`);

      // Broadcast the update to all connected clients
      wss.clients.forEach(function each(client) {
        if (client.readyState === WebSocket.OPEN) {
          client.send(payload);
        }
      });
    },
  });
}

run().catch(console.error);

console.log('WebSocket Server running on ws://localhost:8080');
