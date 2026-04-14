import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Example demonstrating the Circuit Breaker pattern using Resilience4j in Spring Boot.
 * Crucial for preventing cascading failures in microservices architectures.
 */
@Service
public class CircuitBreakerExample {

    private final RestTemplate restTemplate;

    public CircuitBreakerExample(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Calls an external service. If the service fails repeatedly, the circuit breaker
     * will open and immediately route traffic to the fallback method.
     */
    @CircuitBreaker(name = "paymentService", fallbackMethod = "fallbackPayment")
    public String processPayment(String orderId) {
        // Simulating a call to a flaky external payment service
        return restTemplate.getForObject("http://flaky-payment-service/api/pay/" + orderId, String.class);
    }

    /**
     * Fallback method must have the same signature plus an Exception parameter.
     */
    public String fallbackPayment(String orderId, Exception ex) {
        // Fallback logic: e.g., save to a dead letter queue, return a default response,
        // or notify the user that payment is delayed.
        System.err.println("Payment service failed for order: " + orderId + ". Reason: " + ex.getMessage());
        return "Payment is currently unavailable. We will process it later.";
    }
}
