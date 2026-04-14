# Order Service

> This microservice is responsible for all aspects of order management, including creation, status updates, and retrieval.

---

## 1. Responsibilities

*   **Order Creation:** Handles incoming order requests, validates them, and initiates the order processing saga.
*   **Order Lifecycle:** Manages the state of an order (e.g., `PENDING`, `PAID`, `SHIPPED`, `DELIVERED`, `CANCELLED`).
*   **Order History:** Provides endpoints for users to retrieve their past and current orders.
*   **Event Publishing:** Publishes critical business events to a Kafka topic for other services to consume.

---

## 2. API Endpoints

| Method | Endpoint                  | Description                               |
|--------|---------------------------|-------------------------------------------|
| `POST` | `/api/v1/orders`          | Creates a new order.                      |
| `GET`  | `/api/v1/orders/{orderId}`| Retrieves the details of a specific order.|
| `GET`  | `/api/v1/users/{userId}/orders` | Retrieves all orders for a given user.    |
| `PUT`  | `/api/v1/orders/{orderId}/status` | Updates the status of an order (internal).|

---

## 3. Data Model (Order Aggregate)

*   **`Order`**: The root entity.
    *   `orderId` (UUID, PK)
    *   `userId` (UUID)
    *   `status` (String)
    *   `totalAmount` (Decimal)
    *   `createdAt` (Timestamp)
    *   `updatedAt` (Timestamp)
*   **`OrderItem`**: A value object within the Order aggregate.
    *   `productId` (UUID)
    *   `quantity` (Integer)
    *   `price` (Decimal)

---

## 4. Events

This service publishes the following events to the `order-events` Kafka topic:

*   **`OrderCreatedEvent`**: Published when a new order is successfully created.
    *   **Payload:** `orderId`, `userId`, `items`, `totalAmount`
    *   **Consumers:** `Inventory Service`, `Notification Service`
*   **`OrderCancelledEvent`**: Published when an order is cancelled.
    *   **Payload:** `orderId`, `reason`
    *   **Consumers:** `Payment Service` (for refunds), `Inventory Service` (to release stock)

---

## 5. Dependencies

*   **Database:** PostgreSQL (for storing order state).
*   **Message Broker:** Kafka (for asynchronous communication with other services).
*   **Service Dependencies:**
    *   Listens to events from `Payment Service` to update order status to `PAID`.
    *   Listens to events from `Shipping Service` to update order status to `SHIPPED`.
