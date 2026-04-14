package com.interview.microservices.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableFeignClients
@EnableKafka
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}

// Order Entity
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String customerId;
    
    @Column(nullable = false)
    private BigDecimal totalAmount;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();
    
    // Constructors, getters, setters
}

enum OrderStatus {
    PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
}

// Order Service with Circuit Breaker
@Service
@Transactional
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private PaymentServiceClient paymentServiceClient;
    
    @Autowired
    private InventoryServiceClient inventoryServiceClient;
    
    @Autowired
    private OrderEventPublisher eventPublisher;
    
    public OrderResponse createOrder(CreateOrderRequest request) {
        // 1. Validate inventory
        boolean inventoryAvailable = checkInventoryAvailability(request.getItems());
        if (!inventoryAvailable) {
            throw new InsufficientInventoryException("Items not available");
        }
        
        // 2. Create order
        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setTotalAmount(calculateTotal(request.getItems()));
        order.setStatus(OrderStatus.PENDING);
        
        request.getItems().forEach(item -> {
            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(item.getProductId());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(item.getPrice());
            orderItem.setOrder(order);
            order.getItems().add(orderItem);
        });
        
        order = orderRepository.save(order);
        
        // 3. Process payment asynchronously
        processPaymentAsync(order);
        
        // 4. Publish order created event
        eventPublisher.publishOrderCreated(order);
        
        return OrderResponse.from(order);
    }
    
    @CircuitBreaker(name = "inventory-service", fallbackMethod = "inventoryFallback")
    @Retry(name = "inventory-service")
    @TimeLimiter(name = "inventory-service")
    private boolean checkInventoryAvailability(List<OrderItemRequest> items) {
        return inventoryServiceClient.checkAvailability(items);
    }
    
    private boolean inventoryFallback(List<OrderItemRequest> items, Exception ex) {
        // Fallback: assume inventory is available but log for manual check
        log.warn("Inventory service unavailable, assuming availability", ex);
        return true;
    }
    
    @Async
    private void processPaymentAsync(Order order) {
        try {
            PaymentRequest paymentRequest = PaymentRequest.builder()
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .amount(order.getTotalAmount())
                .build();
                
            PaymentResponse response = paymentServiceClient.processPayment(paymentRequest);
            
            if (response.isSuccessful()) {
                order.setStatus(OrderStatus.CONFIRMED);
                orderRepository.save(order);
                eventPublisher.publishOrderConfirmed(order);
            } else {
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                eventPublisher.publishOrderCancelled(order);
            }
        } catch (Exception e) {
            log.error("Payment processing failed for order: " + order.getId(), e);
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
        }
    }
}

// Feign Client for Payment Service
@FeignClient(name = "payment-service", url = "${services.payment.url}")
public interface PaymentServiceClient {
    
    @PostMapping("/api/payments")
    PaymentResponse processPayment(@RequestBody PaymentRequest request);
}

// Kafka Event Publisher
@Component
public class OrderEventPublisher {
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    public void publishOrderCreated(Order order) {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
            .orderId(order.getId())
            .customerId(order.getCustomerId())
            .totalAmount(order.getTotalAmount())
            .items(order.getItems().stream()
                .map(item -> OrderItemEvent.builder()
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .price(item.getPrice())
                    .build())
                .collect(Collectors.toList()))
            .timestamp(Instant.now())
            .build();
            
        kafkaTemplate.send("order-events", event);
    }
    
    public void publishOrderConfirmed(Order order) {
        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
            .orderId(order.getId())
            .customerId(order.getCustomerId())
            .timestamp(Instant.now())
            .build();
            
        kafkaTemplate.send("order-events", event);
    }
}

// REST Controller
@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long orderId) {
        OrderResponse response = orderService.getOrder(orderId);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{orderId}/status")
    public ResponseEntity<Void> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody UpdateOrderStatusRequest request) {
        
        orderService.updateOrderStatus(orderId, request.getStatus());
        return ResponseEntity.ok().build();
    }
}

// Global Exception Handler
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(InsufficientInventoryException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientInventory(
            InsufficientInventoryException ex) {
        
        ErrorResponse error = ErrorResponse.builder()
            .code("INSUFFICIENT_INVENTORY")
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .build();
            
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(
            OrderNotFoundException ex) {
        
        ErrorResponse error = ErrorResponse.builder()
            .code("ORDER_NOT_FOUND")
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .build();
            
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}

// Configuration
@Configuration
@EnableConfigurationProperties
public class OrderServiceConfig {
    
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
    
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }
}

// Application Properties
# application.yml
server:
  port: 8081

spring:
  application:
    name: order-service
  datasource:
    url: jdbc:postgresql://localhost:5432/orderdb
    username: ${DB_USERNAME:orderuser}
    password: ${DB_PASSWORD:orderpass}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

services:
  payment:
    url: http://payment-service:8082
  inventory:
    url: http://inventory-service:8083

resilience4j:
  circuitbreaker:
    instances:
      inventory-service:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
  retry:
    instances:
      inventory-service:
        max-attempts: 3
        wait-duration: 1s
  timelimiter:
    instances:
      inventory-service:
        timeout-duration: 3s

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always