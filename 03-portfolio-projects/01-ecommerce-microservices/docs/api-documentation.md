# 📖 API Documentation

## Base URL
- Local: `http://localhost:8080`
- Production: `https://api.ecommerce.example.com`

## Authentication
All endpoints (except `/api/auth/**`) require a JWT token in the `Authorization` header:
```
Authorization: Bearer <jwt-token>
```

---

## Auth Service

### POST /api/auth/register
Register a new user.
```json
// Request
{ "name": "John Doe", "email": "john@example.com", "password": "securepass123" }

// Response 201
{ "id": 1, "email": "john@example.com" }
```

### POST /api/auth/login
Authenticate and receive JWT.
```json
// Request
{ "email": "john@example.com", "password": "securepass123" }

// Response 200
{ "token": "eyJhbGciOiJIUzI1NiIs..." }
```

---

## Product Service

### POST /api/products
Create a product.
```json
// Request
{ "name": "Laptop", "description": "High-end laptop", "sku": "LAP-001", "price": 999.99, "category": "Electronics", "imageUrl": "https://..." }

// Response 201
{ "id": 1, "name": "Laptop", "sku": "LAP-001", "price": 999.99, ... }
```

### GET /api/products/{id}
Get product by ID. (Cached in Redis)

### GET /api/products/search?query=laptop&page=0&size=20
Search products by name/description.

### GET /api/products/category/{category}?page=0&size=20
Get products by category.

### PUT /api/products/{id}
Update product. (Evicts cache)

### DELETE /api/products/{id}
Soft-delete product. (Evicts cache)

---

## Order Service

### POST /api/orders
Create an order (triggers Saga: inventory reservation → payment → confirmation).
```json
// Request
{
  "customerId": "user-123",
  "items": [
    { "productId": 1, "quantity": 2, "price": 999.99 }
  ]
}

// Response 201
{ "id": 1, "customerId": "user-123", "totalAmount": 1999.98, "status": "PENDING", ... }
```

### GET /api/orders/{orderId}
Get order by ID.

### GET /api/orders/customer/{customerId}?page=0&size=20
Get customer order history.

---

## Payment Service

### POST /api/payments
Process payment (requires `X-Idempotency-Key` header).
```json
// Headers: X-Idempotency-Key: unique-key-123
// Request
{ "orderId": 1, "customerId": "user-123", "amount": 1999.98 }

// Response 201
{ "id": 1, "orderId": 1, "status": "COMPLETED", "transactionRef": "TXN-A1B2C3D4" }
```

### POST /api/payments/{orderId}/refund
Refund a completed payment.

---

## Inventory Service

### GET /api/inventory/{productId}/stock
Get available stock for a product.
```json
// Response 200
{ "productId": 1, "availableStock": 150 }
```

---

## Kafka Topics

| Topic | Events |
|-------|--------|
| `order-events` | OrderCreated, OrderConfirmed, OrderCancelled |
| `payment-events` | PaymentCompleted, PaymentFailed |
| `inventory-events` | InventoryReserved, InventoryReservationFailed |
| `notification-events` | OrderConfirmed, OrderCancelled |
