/**
 * Architecture Decision Record (ADR) Example.
 * A key role of a lead engineer is to make, document, and communicate
 * significant architectural decisions. This example shows a simplified ADR
 * in comments, followed by code that reflects the chosen path.
 */

/*
 * ADR-001: Service Decomposition Strategy
 *
 * Status: Accepted
 *
 * Context:
 * Our e-commerce application is currently a monolith. While this was effective
 * for initial development, we are now facing challenges with deployment cycles,
 * team scalability, and fault isolation. The checkout and payment processing
 * logic is complex and requires frequent, independent updates.
 *
 * Decision:
 * We will adopt a hybrid "Strangler Fig" pattern. We will not rewrite the entire
 * application at once. Instead, we will incrementally extract the most critical
 * and volatile domain—Payment Processing—into a separate microservice.
 * The existing monolith will act as an API gateway for this new service initially.
 *
 * Consequences:
 * - Positive:
 *   - Allows the Payment team to deploy independently and more frequently.
 *   - Isolates critical payment logic, improving overall system resilience.
 *   - Provides a low-risk path to validate our microservices infrastructure.
 * - Negative:
 *   - Introduces network latency between the monolith and the new service.
 *   - Adds operational overhead (monitoring, deployment for the new service).
 *   - Requires careful management of distributed transactions (e.g., using a Saga pattern).
 */

// --- Code reflecting the ADR ---

// 1. The new, separate Payment Service (Microservice)
class PaymentService {
    public boolean processPayment(String orderId, double amount) {
        System.out.println("Processing payment for order " + orderId + " in the new Payment Microservice.");
        // Complex, isolated payment logic here...
        return true;
    }
}

// 2. The existing Monolith, now acting as a client to the new service
class MonolithCheckoutHandler {
    private final PaymentServiceApiClient paymentServiceApiClient;

    public MonolithCheckoutHandler() {
        this.paymentServiceApiClient = new PaymentServiceApiClient("http://payment-service.internal");
    }

    public void handleCheckout(String orderId, double amount) {
        System.out.println("Checkout initiated in the Monolith for order " + orderId);
        // ... other monolith logic (e.g., inventory check)

        // The "strangler" part: call the new microservice instead of the old internal module
        boolean paymentSuccess = paymentServiceApiClient.processPayment(orderId, amount);

        if (paymentSuccess) {
            System.out.println("Monolith confirms checkout completion for order " + orderId);
        } else {
            System.out.println("Monolith handles payment failure for order " + orderId);
        }
    }
}

// 3. An API client within the monolith to communicate with the new service
class PaymentServiceApiClient {
    private final String serviceUrl;
    public PaymentServiceApiClient(String url) { this.serviceUrl = url; }
    public boolean processPayment(String orderId, double amount) {
        System.out.println("Monolith calling Payment Service at " + serviceUrl);
        // In a real app, this would be an HTTP or gRPC call.
        return new PaymentService().processPayment(orderId, amount);
    }
}

public class AdrExample {
    public static void main(String[] args) {
        MonolithCheckoutHandler checkoutHandler = new MonolithCheckoutHandler();
        checkoutHandler.handleCheckout("ORD-456", 199.99);
    }
}
