# 🛒 Microservices E-commerce Platform

> Production-ready e-commerce platform built with Spring Boot 3, Kafka, Redis, and PostgreSQL demonstrating microservices best practices.

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)]()
[![Java](https://img.shields.io/badge/Java-21-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)]()
[![License](https://img.shields.io/badge/license-MIT-blue)]()

---

## 🎯 Overview

A fully functional e-commerce platform decomposed into **7 microservices** communicating via REST and Kafka events. Demonstrates patterns critical for Lead/Principal Engineer interviews: Saga, Circuit Breaker, CQRS, Event Sourcing, API Gateway, and Distributed Tracing.

## 🏗️ Architecture

```
                         ┌──────────────┐
                         │   Client     │
                         └──────┬───────┘
                                │
                         ┌──────▼───────┐
                         │ API Gateway  │  (Spring Cloud Gateway)
                         │ Rate Limit   │
                         │ Auth Filter  │
                         └──────┬───────┘
                                │
          ┌─────────────────────┼─────────────────────┐
          │                     │                       │
   ┌──────▼──────┐    ┌───────▼───────┐    ┌─────────▼────────┐
   │   Product   │    │    Order      │    │      User        │
   │   Service   │    │   Service     │    │    Service       │
   │             │    │  (Saga Orch.) │    │  (JWT Auth)      │
   └──────┬──────┘    └───────┬───────┘    └──────────────────┘
          │                   │
          │           ┌───────┼───────┐
          │           │               │
   ┌──────▼──────┐  ┌▼───────────┐  ┌▼──────────────┐
   │  Inventory  │  │  Payment   │  │ Notification   │
   │  Service    │  │  Service   │  │   Service      │
   └─────────────┘  └────────────┘  └────────────────┘
          │               │                │
          └───────────────┼────────────────┘
                          │
                   ┌──────▼──────┐
                   │    Kafka    │  (Event Bus)
                   └─────────────┘
```

## 🚀 Features

- **Product Catalog** — CRUD, search, categories, caching with Redis
- **Order Management** — Saga-based distributed transactions, order lifecycle
- **Payment Processing** — Idempotent payments, retry with exponential backoff
- **Inventory Management** — Real-time stock tracking, distributed locking
- **User & Auth** — JWT authentication, role-based access control
- **Notifications** — Event-driven email/SMS via Kafka consumers
- **API Gateway** — Rate limiting, authentication filter, request routing
- **Observability** — Prometheus metrics, distributed tracing, structured logging

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.2, Spring Cloud 2023 |
| API Gateway | Spring Cloud Gateway |
| Messaging | Apache Kafka |
| Databases | PostgreSQL (per-service), Redis (caching/locking) |
| Auth | Spring Security + JWT |
| Resilience | Resilience4j (Circuit Breaker, Retry, Rate Limiter) |
| Observability | Micrometer, Prometheus, Grafana, Zipkin |
| Containerization | Docker, Docker Compose |
| Orchestration | Kubernetes (EKS-ready) |
| CI/CD | GitHub Actions |
| IaC | Terraform (AWS) |

## 📦 Project Structure

```
01-ecommerce-microservices/
├── api-gateway/                 # Spring Cloud Gateway
├── product-service/             # Product catalog & search
├── order-service/               # Order management + Saga orchestrator
├── payment-service/             # Payment processing
├── inventory-service/           # Stock management
├── user-service/                # Authentication & authorization
├── notification-service/        # Event-driven notifications
├── common-lib/                  # Shared DTOs, events, exceptions
├── kubernetes/                  # K8s manifests
├── terraform/                   # AWS infrastructure
├── scripts/                     # Setup & deployment scripts
├── docker-compose.yml           # Local development
├── pom.xml                      # Parent POM
└── docs/                        # Architecture & API docs
```

## ⚡ Quick Start

### Prerequisites
- Java 21+
- Docker & Docker Compose
- Maven 3.9+

### Run Locally

```bash
# Clone
git clone https://github.com/<your-username>/ecommerce-microservices.git
cd ecommerce-microservices

# Start infrastructure (Kafka, PostgreSQL, Redis, Zipkin)
docker-compose up -d postgres kafka redis zipkin

# Build all services
./mvnw clean package -DskipTests

# Start all services
docker-compose up -d
```

### Verify

| Service | URL |
|---------|-----|
| API Gateway | http://localhost:8080 |
| Product Service | http://localhost:8081 |
| Order Service | http://localhost:8082 |
| Payment Service | http://localhost:8083 |
| Inventory Service | http://localhost:8084 |
| User Service | http://localhost:8085 |
| Kafka UI | http://localhost:9090 |
| Zipkin | http://localhost:9411 |
| Prometheus | http://localhost:9091 |
| Grafana | http://localhost:3000 |

## 🧪 Testing

```bash
# Unit + Integration tests
./mvnw test

# Specific service
./mvnw test -pl order-service
```

## 📊 Key Design Patterns

### Saga Pattern (Order Flow)
```
CreateOrder → ReserveInventory → ProcessPayment → ConfirmOrder
                  ↓ (fail)            ↓ (fail)
            ReleaseInventory     RefundPayment → CancelOrder
```

### Circuit Breaker
- Inventory & Payment calls wrapped with Resilience4j
- Fallback strategies for graceful degradation

### Event-Driven Architecture
- Kafka topics: `order-events`, `payment-events`, `inventory-events`, `notification-events`
- Eventual consistency across services

### CQRS (Order Service)
- Write model: PostgreSQL (normalized)
- Read model: Redis (denormalized for fast queries)

## 📈 Performance

| Metric | Target |
|--------|--------|
| Throughput | 10K req/sec (gateway) |
| Latency (p99) | < 100ms |
| Availability | 99.9% |
| Order Processing | < 2s end-to-end |

## 🔒 Security

- JWT-based authentication at API Gateway
- Service-to-service communication via internal network
- Rate limiting (100 req/min per user)
- Input validation on all endpoints
- SQL injection prevention via JPA parameterized queries

## 🚀 Deployment

### Kubernetes (AWS EKS)
```bash
# Apply manifests
kubectl apply -f kubernetes/

# Or use the deploy script
./scripts/deploy.sh
```

### Terraform (AWS Infrastructure)
```bash
cd terraform
terraform init
terraform plan
terraform apply
```

## 🎯 Interview Talking Points

1. **Why microservices?** — Independent scaling, team autonomy, technology flexibility
2. **Saga vs 2PC** — Chose Saga for better availability and loose coupling
3. **Why Kafka over RabbitMQ?** — Log-based, replay capability, higher throughput
4. **Database per service** — Data isolation, independent schema evolution
5. **Circuit Breaker** — Prevents cascade failures, enables graceful degradation
6. **CQRS** — Separates read/write concerns, optimizes query performance
7. **API Gateway** — Single entry point, cross-cutting concerns centralized
8. **Idempotency** — Payment deduplication via idempotency keys

## 📖 Documentation

- [Architecture Decisions](./ARCHITECTURE.md)
- [API Documentation](./docs/api-documentation.md)
- [Deployment Guide](./docs/deployment-guide.md)

## 📄 License

MIT License

## 👤 Author

**Manish Deo** — Lead Software Engineer
