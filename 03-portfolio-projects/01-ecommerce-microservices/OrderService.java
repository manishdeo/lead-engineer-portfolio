import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Example Order Service in a Microservices E-commerce Platform.
 * Demonstrates publishing an order creation event to a message broker (Kafka).
 */
@Service
public class OrderService {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    // Database repository would be here

    public OrderService(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Creates a new order and publishes an event to notify other services.
     * This follows the Choreography pattern for distributed transactions (Saga).
     */
    public void createOrder(String customerId, String productId, int quantity) {
        // 1. Save order to database with status PENDING (not shown here)
        String orderId = "ORD-" + System.currentTimeMillis();
        
        // 2. Publish OrderCreated event to Kafka
        OrderEvent event = new OrderEvent(orderId, customerId, productId, quantity);
        kafkaTemplate.send("order-events", orderId, event);
        
        // Other services (Inventory, Payment) will listen to this topic
        System.out.println("Order created and event published: " + orderId);
    }
}

class OrderEvent {
    String orderId;
    String customerId;
    String productId;
    int quantity;
    // Constructor, getters, setters
    public OrderEvent(String orderId, String customerId, String productId, int quantity) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
    }
}
