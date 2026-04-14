# 🏗️ Microservices Patterns

## Decomposition Patterns

### Database per Service
- Each microservice owns its data
- Prevents tight coupling
- Enables independent scaling

### Saga Pattern
- Manages distributed transactions
- **Choreography**: Event-driven coordination
- **Orchestration**: Central coordinator

## Communication Patterns

### API Gateway
- Single entry point for clients
- Request routing and composition
- Cross-cutting concerns (auth, logging)

### Service Mesh
- Infrastructure layer for service communication
- Traffic management, security, observability
- Examples: Istio, Linkerd

### Event Sourcing
- Store events instead of current state
- Complete audit trail
- Replay capability for debugging

## Data Management

### CQRS (Command Query Responsibility Segregation)
- Separate read and write models
- Optimized for different access patterns
- Scales reads and writes independently

### Distributed Cache
- Shared cache across services
- Reduces database load
- Improves response times

## Reliability Patterns

### Circuit Breaker
```java
@Component
public class PaymentService {
    @CircuitBreaker(name = "payment")
    public PaymentResponse processPayment(PaymentRequest request) {
        // Payment processing logic
    }
}
```

### Bulkhead
- Isolate critical resources
- Prevent resource exhaustion
- Separate thread pools per service

### Timeout and Retry
```java
@Retryable(value = {Exception.class}, maxAttempts = 3)
public String callExternalService() {
    // Service call with retry logic
}
```

## Deployment Patterns

### Blue-Green Deployment
- Two identical production environments
- Zero-downtime deployments
- Quick rollback capability

### Canary Deployment
- Gradual rollout to subset of users
- Monitor metrics before full deployment
- Risk mitigation strategy