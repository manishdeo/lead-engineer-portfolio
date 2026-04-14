# 🍃 Spring Boot & Microservices - Interview Q&A

> 200+ Questions with Answers, Examples & Latest Trends (2024-2026)

---

## 📋 Table of Contents

1. [Spring Boot Fundamentals](#1-spring-boot-fundamentals)
2. [REST API Design](#2-rest-api-design)
3. [Spring Security & OAuth2](#3-spring-security)
4. [Microservices Patterns](#4-microservices-patterns)
5. [Spring Cloud](#5-spring-cloud)
6. [Reactive Programming](#6-reactive-programming)
7. [Testing Strategies](#7-testing-strategies)

---

## 1. Spring Boot Fundamentals

### Q1: Explain Spring Boot Auto-Configuration. How does it work internally?

**Answer:**

**Auto-Configuration:**
- Automatically configures beans based on classpath
- Uses @Conditional annotations
- Can be overridden by custom configuration
- Defined in spring.factories (Spring Boot 2.x) or AutoConfiguration.imports (Spring Boot 3.x)

**Example:**

```java
// Spring Boot's DataSource Auto-Configuration (Simplified)
@Configuration
@ConditionalOnClass({DataSource.class, EmbeddedDatabaseType.class})
@ConditionalOnMissingBean(DataSource.class)
@EnableConfigurationProperties(DataSourceProperties.class)
public class DataSourceAutoConfiguration {
    
    @Bean
    @ConditionalOnProperty(name = "spring.datasource.url")
    public DataSource dataSource(DataSourceProperties properties) {
        return DataSourceBuilder
            .create()
            .url(properties.getUrl())
            .username(properties.getUsername())
            .password(properties.getPassword())
            .build();
    }
}

// Custom Auto-Configuration
@Configuration
@ConditionalOnClass(RedisTemplate.class)
@EnableConfigurationProperties(RedisProperties.class)
public class CustomRedisAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
```

**How to Create Custom Starter:**

```java
// 1. Create auto-configuration
@Configuration
@EnableConfigurationProperties(MyServiceProperties.class)
public class MyServiceAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public MyService myService(MyServiceProperties properties) {
        return new MyService(properties.getApiKey());
    }
}

// 2. Create properties class
@ConfigurationProperties(prefix = "myservice")
public class MyServiceProperties {
    private String apiKey;
    private int timeout = 5000;
    // getters/setters
}

// 3. Register in META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
com.example.MyServiceAutoConfiguration
```

**Interview Tip:** Mention @Conditional annotations and how Spring Boot reduces boilerplate configuration.

---

### Q2: What's new in Spring Boot 3.x? How does it differ from 2.x?

**Answer:**

**Major Changes in Spring Boot 3.x:**

| Feature | Spring Boot 2.x | Spring Boot 3.x |
|---------|----------------|-----------------|
| **Java Version** | Java 8+ | Java 17+ |
| **Jakarta EE** | javax.* | jakarta.* |
| **Native Images** | Experimental | Production-ready |
| **Observability** | Micrometer | Enhanced with Micrometer Tracing |
| **HTTP Interface** | RestTemplate/WebClient | HTTP Interface (declarative) |

**Example - HTTP Interface (New in Spring Boot 3):**

```java
// Old Way - RestTemplate
@Service
public class UserService {
    private final RestTemplate restTemplate;
    
    public User getUser(Long id) {
        return restTemplate.getForObject(
            "https://api.example.com/users/" + id, 
            User.class
        );
    }
}

// New Way - HTTP Interface (Spring Boot 3)
@HttpExchange("/users")
public interface UserClient {
    
    @GetExchange("/{id}")
    User getUser(@PathVariable Long id);
    
    @PostExchange
    User createUser(@RequestBody User user);
    
    @PutExchange("/{id}")
    User updateUser(@PathVariable Long id, @RequestBody User user);
    
    @DeleteExchange("/{id}")
    void deleteUser(@PathVariable Long id);
}

// Configuration
@Configuration
public class HttpClientConfig {
    
    @Bean
    public UserClient userClient() {
        WebClient webClient = WebClient.builder()
            .baseUrl("https://api.example.com")
            .build();
        
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builder(WebClientAdapter.forClient(webClient))
            .build();
        
        return factory.createClient(UserClient.class);
    }
}
```

**Native Image Support:**

```bash
# Build native image with GraalVM
./mvnw -Pnative native:compile

# Run native executable (starts in milliseconds)
./target/myapp
```

**Interview Tip:** Emphasize Jakarta EE migration and native image support for faster startup times.

---

## 2. REST API Design

### Q3: Design a RESTful API for an e-commerce order system. Include error handling and validation.

**Answer:**

```java
// Domain Model
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String customerId;
    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// DTO with Validation
public record CreateOrderRequest(
    @NotBlank(message = "Customer ID is required")
    String customerId,
    
    @NotEmpty(message = "Order must have at least one item")
    @Valid
    List<OrderItemRequest> items
) {}

public record OrderItemRequest(
    @NotBlank String productId,
    @Min(value = 1, message = "Quantity must be at least 1") 
    int quantity,
    @DecimalMin(value = "0.01", message = "Price must be positive") 
    BigDecimal price
) {}

// Controller with proper REST design
@RestController
@RequestMapping("/api/v1/orders")
@Validated
public class OrderController {
    
    private final OrderService orderService;
    
    // Create Order
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        
        Order order = orderService.createOrder(request);
        OrderResponse response = OrderMapper.toResponse(order);
        
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(order.getId())
            .toUri();
        
        return ResponseEntity.created(location).body(response);
    }
    
    // Get Order
    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable Long id) {
        return orderService.findById(id)
            .map(OrderMapper::toResponse)
            .orElseThrow(() -> new OrderNotFoundException(id));
    }
    
    // List Orders with Pagination
    @GetMapping
    public Page<OrderResponse> listOrders(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) 
            Pageable pageable) {
        
        return orderService.findOrders(customerId, status, pageable)
            .map(OrderMapper::toResponse);
    }
    
    // Update Order Status
    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(
            @PathVariable Long id,
            @RequestBody @Valid UpdateStatusRequest request) {
        
        Order order = orderService.updateStatus(id, request.status());
        return OrderMapper.toResponse(order);
    }
    
    // Cancel Order
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelOrder(@PathVariable Long id) {
        orderService.cancelOrder(id);
    }
}

// Global Exception Handler
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleOrderNotFound(OrderNotFoundException ex) {
        return new ErrorResponse(
            "ORDER_NOT_FOUND",
            ex.getMessage(),
            LocalDateTime.now()
        );
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                FieldError::getDefaultMessage
            ));
        
        return new ErrorResponse(
            "VALIDATION_ERROR",
            "Invalid request parameters",
            errors,
            LocalDateTime.now()
        );
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGenericError(Exception ex) {
        log.error("Unexpected error", ex);
        return new ErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            LocalDateTime.now()
        );
    }
}

// Service Layer
@Service
@Transactional
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final ApplicationEventPublisher eventPublisher;
    
    public Order createOrder(CreateOrderRequest request) {
        // Validate inventory
        inventoryService.validateStock(request.items());
        
        // Create order
        Order order = new Order();
        order.setCustomerId(request.customerId());
        order.setStatus(OrderStatus.PENDING);
        
        request.items().forEach(item -> {
            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(item.productId());
            orderItem.setQuantity(item.quantity());
            orderItem.setPrice(item.price());
            order.addItem(orderItem);
        });
        
        order.calculateTotal();
        Order savedOrder = orderRepository.save(order);
        
        // Publish event
        eventPublisher.publishEvent(new OrderCreatedEvent(savedOrder));
        
        return savedOrder;
    }
}
```

**API Documentation with OpenAPI:**

```java
@Configuration
public class OpenAPIConfig {
    
    @Bean
    public OpenAPI orderAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Order Management API")
                .version("v1")
                .description("RESTful API for order management"))
            .servers(List.of(
                new Server().url("https://api.example.com").description("Production"),
                new Server().url("http://localhost:8080").description("Development")
            ));
    }
}
```

**Interview Tip:** Emphasize proper HTTP status codes, validation, error handling, and HATEOAS principles.

---

## 3. Spring Security

### Q4: Implement JWT-based authentication with Spring Security. Include refresh token mechanism.

**Answer:**

```java
// Security Configuration
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
            );
        
        return http.build();
    }
}

// JWT Service
@Service
public class JwtService {
    
    @Value("${jwt.secret}")
    private String secretKey;
    
    @Value("${jwt.expiration}")
    private long jwtExpiration;
    
    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;
    
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails, jwtExpiration);
    }
    
    public String generateRefreshToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails, refreshExpiration);
    }
    
    private String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration) {
        
        return Jwts.builder()
            .setClaims(extraClaims)
            .setSubject(userDetails.getUsername())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSignInKey(), SignatureAlgorithm.HS256)
            .compact();
    }
    
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }
    
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSignInKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
    
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

// JWT Authentication Filter
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        final String jwt = authHeader.substring(7);
        final String username = jwtService.extractUsername(jwt);
        
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );
                
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}

// Authentication Controller
@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
    
    private final AuthenticationService authService;
    
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refreshToken(
            @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String token) {
        authService.logout(token.substring(7));
        return ResponseEntity.noContent().build();
    }
}

// Authentication Service
@Service
public class AuthenticationService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    
    public AuthenticationResponse register(RegisterRequest request) {
        var user = User.builder()
            .username(request.username())
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .role(Role.USER)
            .build();
        
        userRepository.save(user);
        
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        
        saveRefreshToken(user, refreshToken);
        
        return new AuthenticationResponse(jwtToken, refreshToken);
    }
    
    public AuthenticationResponse authenticate(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.username(),
                request.password()
            )
        );
        
        var user = userRepository.findByUsername(request.username())
            .orElseThrow();
        
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        
        revokeAllUserTokens(user);
        saveRefreshToken(user, refreshToken);
        
        return new AuthenticationResponse(jwtToken, refreshToken);
    }
    
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        final String refreshToken = request.refreshToken();
        final String username = jwtService.extractUsername(refreshToken);
        
        if (username != null) {
            var user = userRepository.findByUsername(username)
                .orElseThrow();
            
            if (jwtService.isTokenValid(refreshToken, user)) {
                var accessToken = jwtService.generateToken(user);
                return new AuthenticationResponse(accessToken, refreshToken);
            }
        }
        
        throw new InvalidTokenException("Invalid refresh token");
    }
    
    private void saveRefreshToken(User user, String token) {
        var refreshToken = RefreshToken.builder()
            .user(user)
            .token(token)
            .expiryDate(Instant.now().plusMillis(604800000)) // 7 days
            .build();
        
        refreshTokenRepository.save(refreshToken);
    }
    
    private void revokeAllUserTokens(User user) {
        var validUserTokens = refreshTokenRepository.findAllValidTokensByUser(user.getId());
        if (validUserTokens.isEmpty()) return;
        
        validUserTokens.forEach(token -> token.setRevoked(true));
        refreshTokenRepository.saveAll(validUserTokens);
    }
}
```

**Interview Tip:** Mention token rotation, refresh token security, and storing tokens securely (HttpOnly cookies for web apps).

---

## 4. Microservices Patterns

### Q5: Implement Saga Pattern for distributed transactions. Show both Orchestration and Choreography approaches.

**Answer:**

**Orchestration-Based Saga:**

```java
// Saga Orchestrator
@Service
public class OrderSagaOrchestrator {
    
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final ShippingService shippingService;
    
    @Transactional
    public OrderResult createOrder(CreateOrderRequest request) {
        String sagaId = UUID.randomUUID().toString();
        
        try {
            // Step 1: Create Order
            Order order = orderService.createOrder(request, sagaId);
            
            // Step 2: Reserve Inventory
            InventoryReservation reservation = inventoryService.reserveInventory(
                order.getId(), 
                order.getItems()
            );
            
            // Step 3: Process Payment
            Payment payment = paymentService.processPayment(
                order.getId(),
                order.getTotalAmount()
            );
            
            // Step 4: Arrange Shipping
            Shipment shipment = shippingService.createShipment(order.getId());
            
            // All steps successful
            orderService.completeOrder(order.getId());
            return OrderResult.success(order);
            
        } catch (InventoryException e) {
            // Compensate: Cancel order
            orderService.cancelOrder(sagaId);
            return OrderResult.failure("Inventory not available");
            
        } catch (PaymentException e) {
            // Compensate: Release inventory, cancel order
            inventoryService.releaseInventory(sagaId);
            orderService.cancelOrder(sagaId);
            return OrderResult.failure("Payment failed");
            
        } catch (ShippingException e) {
            // Compensate: Refund payment, release inventory, cancel order
            paymentService.refundPayment(sagaId);
            inventoryService.releaseInventory(sagaId);
            orderService.cancelOrder(sagaId);
            return OrderResult.failure("Shipping unavailable");
        }
    }
}
```

**Choreography-Based Saga:**

```java
// Event-Driven Saga with Kafka

// Order Service
@Service
public class OrderService {
    
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING);
        // ... set other fields
        
        Order savedOrder = orderRepository.save(order);
        
        // Publish OrderCreated event
        OrderCreatedEvent event = new OrderCreatedEvent(
            savedOrder.getId(),
            savedOrder.getCustomerId(),
            savedOrder.getItems(),
            savedOrder.getTotalAmount()
        );
        
        kafkaTemplate.send("order-events", event);
        
        return savedOrder;
    }
    
    // Listen for saga completion/failure
    @KafkaListener(topics = "payment-events")
    public void handlePaymentEvent(PaymentEvent event) {
        if (event instanceof PaymentSuccessEvent) {
            orderRepository.updateStatus(event.getOrderId(), OrderStatus.PAID);
        } else if (event instanceof PaymentFailedEvent) {
            orderRepository.updateStatus(event.getOrderId(), OrderStatus.CANCELLED);
        }
    }
}

// Inventory Service
@Service
public class InventoryService {
    
    private final KafkaTemplate<String, InventoryEvent> kafkaTemplate;
    
    @KafkaListener(topics = "order-events")
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        try {
            // Reserve inventory
            for (OrderItem item : event.getItems()) {
                inventoryRepository.reserveStock(item.getProductId(), item.getQuantity());
            }
            
            // Publish success event
            InventoryReservedEvent successEvent = new InventoryReservedEvent(
                event.getOrderId(),
                event.getItems()
            );
            kafkaTemplate.send("inventory-events", successEvent);
            
        } catch (InsufficientStockException e) {
            // Publish failure event
            InventoryReservationFailedEvent failureEvent = 
                new InventoryReservationFailedEvent(event.getOrderId(), e.getMessage());
            kafkaTemplate.send("inventory-events", failureEvent);
        }
    }
    
    // Compensating transaction
    @KafkaListener(topics = "payment-events")
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        // Release reserved inventory
        inventoryRepository.releaseReservation(event.getOrderId());
    }
}

// Payment Service
@Service
public class PaymentService {
    
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    
    @KafkaListener(topics = "inventory-events")
    @Transactional
    public void handleInventoryReserved(InventoryReservedEvent event) {
        try {
            // Process payment
            Payment payment = processPayment(event.getOrderId(), event.getAmount());
            
            // Publish success event
            PaymentSuccessEvent successEvent = new PaymentSuccessEvent(
                event.getOrderId(),
                payment.getTransactionId()
            );
            kafkaTemplate.send("payment-events", successEvent);
            
        } catch (PaymentException e) {
            // Publish failure event
            PaymentFailedEvent failureEvent = new PaymentFailedEvent(
                event.getOrderId(),
                e.getMessage()
            );
            kafkaTemplate.send("payment-events", failureEvent);
        }
    }
}

// Saga State Management
@Entity
public class SagaState {
    @Id
    private String sagaId;
    private String orderId;
    private SagaStatus status;
    private String currentStep;
    private Map<String, String> compensations;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

@Service
public class SagaStateManager {
    
    private final SagaStateRepository repository;
    
    public void startSaga(String sagaId, String orderId) {
        SagaState state = new SagaState();
        state.setSagaId(sagaId);
        state.setOrderId(orderId);
        state.setStatus(SagaStatus.STARTED);
        state.setCurrentStep("ORDER_CREATED");
        repository.save(state);
    }
    
    public void updateStep(String sagaId, String step) {
        SagaState state = repository.findById(sagaId).orElseThrow();
        state.setCurrentStep(step);
        state.setUpdatedAt(LocalDateTime.now());
        repository.save(state);
    }
    
    public void completeSaga(String sagaId) {
        SagaState state = repository.findById(sagaId).orElseThrow();
        state.setStatus(SagaStatus.COMPLETED);
        repository.save(state);
    }
    
    public void failSaga(String sagaId, String reason) {
        SagaState state = repository.findById(sagaId).orElseThrow();
        state.setStatus(SagaStatus.FAILED);
        // Trigger compensations
        triggerCompensations(state);
        repository.save(state);
    }
}
```

**Interview Tip:** Explain trade-offs: Orchestration (centralized control, easier debugging) vs Choreography (decoupled, more scalable).

---

## 🎯 Quick Reference

### Top 10 Must-Know Topics

1. ✅ Spring Boot 3.x new features
2. ✅ JWT Authentication & Authorization
3. ✅ Saga Pattern (Orchestration vs Choreography)
4. ✅ Circuit Breaker (Resilience4j)
5. ✅ API Gateway patterns
6. ✅ Event-Driven Architecture
7. ✅ Reactive Programming (WebFlux)
8. ✅ Distributed Tracing
9. ✅ Service Mesh (Istio)
10. ✅ Testing strategies

### Latest Trends (2024-2026)

- **Spring Boot 3.2+** - Virtual Threads support
- **Spring AI** - LLM integration framework
- **GraalVM Native Images** - Fast startup
- **Observability** - OpenTelemetry integration
- **Testcontainers** - Integration testing

---

**Next:** [React JS Advanced Q&A](../04-react-advanced/README.md)
