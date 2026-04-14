# ☕ Java Advanced - Interview Q&A

> 150+ Questions with Answers, Examples & Latest Trends (2024-2026)

---

## 📋 Table of Contents

1. [Java Concurrency & Multithreading](#1-concurrency--multithreading)
2. [JVM Internals & Memory Management](#2-jvm-internals)
3. [Garbage Collection](#3-garbage-collection)
4. [Java 17+ Modern Features](#4-java-17-features)
5. [Performance Tuning](#5-performance-tuning)
6. [Design Patterns in Java](#6-design-patterns)

---

## 1. Concurrency & Multithreading

### Q1: Explain the difference between synchronized and ReentrantLock. When would you use each?

**Answer:**

**synchronized:**
- Built-in Java keyword
- Implicit lock acquisition/release
- Cannot interrupt waiting threads
- No fairness guarantee
- Simpler syntax

**ReentrantLock:**
- Explicit lock/unlock
- Can interrupt waiting threads (lockInterruptibly)
- Supports fairness policy
- Try-lock with timeout
- More flexible but requires manual unlock

**Example:**

```java
// synchronized approach
public class Counter {
    private int count = 0;
    
    public synchronized void increment() {
        count++;
    }
}

// ReentrantLock approach
public class Counter {
    private int count = 0;
    private final ReentrantLock lock = new ReentrantLock(true); // fair lock
    
    public void increment() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock(); // Always unlock in finally
        }
    }
    
    public boolean tryIncrement(long timeout, TimeUnit unit) throws InterruptedException {
        if (lock.tryLock(timeout, unit)) {
            try {
                count++;
                return true;
            } finally {
                lock.unlock();
            }
        }
        return false;
    }
}
```

**When to use:**
- **synchronized**: Simple cases, method-level locking
- **ReentrantLock**: Need timeout, fairness, or interruptibility

**Interview Tip:** Mention that ReentrantLock gives more control but requires discipline (always unlock in finally).

---

### Q2: What are Virtual Threads (Project Loom)? How do they differ from Platform Threads?

**Answer:**

**Virtual Threads (Java 19+):**
- Lightweight threads managed by JVM
- Can create millions of virtual threads
- Scheduled on carrier (platform) threads
- Ideal for I/O-bound operations
- No need for thread pools

**Platform Threads:**
- Traditional OS threads
- Heavy (1MB stack per thread)
- Limited by OS resources
- Better for CPU-bound tasks

**Example:**

```java
// Traditional Platform Threads
ExecutorService executor = Executors.newFixedThreadPool(100);
for (int i = 0; i < 10000; i++) {
    executor.submit(() -> {
        // This would exhaust thread pool
        Thread.sleep(1000);
    });
}

// Virtual Threads (Java 21+)
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 10000; i++) {
        executor.submit(() -> {
            // Can handle millions of tasks
            Thread.sleep(Duration.ofSeconds(1));
            return "Task completed";
        });
    }
} // Auto-close waits for all tasks

// Creating Virtual Thread directly
Thread.startVirtualThread(() -> {
    System.out.println("Running in virtual thread");
});

// Using Thread.ofVirtual()
Thread vThread = Thread.ofVirtual()
    .name("virtual-worker")
    .start(() -> {
        // Task logic
    });
```

**Real-World Use Case:**
```java
@RestController
public class UserController {
    
    // Spring Boot 3.2+ with Virtual Threads
    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        // Each request runs on virtual thread
        // Can handle 100K+ concurrent requests
        return userService.findById(id); // I/O operation
    }
}
```

**Interview Tip:** Emphasize that Virtual Threads are game-changers for high-concurrency I/O applications like microservices.

---

### Q3: Explain CompletableFuture and its use cases. Provide a real-world example.

**Answer:**

**CompletableFuture:**
- Asynchronous programming API
- Composable async operations
- Exception handling
- Combines multiple futures
- Non-blocking

**Example - Microservice Orchestration:**

```java
@Service
public class OrderService {
    
    @Autowired
    private UserService userService;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private PaymentService paymentService;
    
    public CompletableFuture<OrderResponse> createOrder(OrderRequest request) {
        
        // Parallel async calls
        CompletableFuture<User> userFuture = 
            CompletableFuture.supplyAsync(() -> userService.getUser(request.getUserId()));
        
        CompletableFuture<Inventory> inventoryFuture = 
            CompletableFuture.supplyAsync(() -> inventoryService.checkStock(request.getProductId()));
        
        CompletableFuture<PaymentMethod> paymentFuture = 
            CompletableFuture.supplyAsync(() -> paymentService.getPaymentMethod(request.getPaymentId()));
        
        // Combine all results
        return CompletableFuture.allOf(userFuture, inventoryFuture, paymentFuture)
            .thenApply(v -> {
                User user = userFuture.join();
                Inventory inventory = inventoryFuture.join();
                PaymentMethod payment = paymentFuture.join();
                
                // Process order
                return processOrder(user, inventory, payment);
            })
            .exceptionally(ex -> {
                log.error("Order creation failed", ex);
                return OrderResponse.failed(ex.getMessage());
            });
    }
    
    // Chaining example
    public CompletableFuture<String> complexWorkflow(String orderId) {
        return CompletableFuture.supplyAsync(() -> validateOrder(orderId))
            .thenCompose(order -> reserveInventory(order))
            .thenCompose(reservation -> processPayment(reservation))
            .thenApply(payment -> sendConfirmation(payment))
            .thenApply(confirmation -> "Order completed: " + confirmation)
            .orTimeout(5, TimeUnit.SECONDS)
            .exceptionally(ex -> "Order failed: " + ex.getMessage());
    }
}
```

**Advanced Pattern - Timeout & Fallback:**

```java
public CompletableFuture<String> callExternalAPI() {
    return CompletableFuture.supplyAsync(() -> {
        // Slow external API call
        return restTemplate.getForObject("https://api.example.com/data", String.class);
    })
    .orTimeout(2, TimeUnit.SECONDS)
    .exceptionally(ex -> {
        log.warn("API timeout, using cache");
        return cacheService.get("fallback-data");
    });
}
```

**Interview Tip:** Mention real-world scenarios like parallel microservice calls, async event processing, or timeout handling.

---

### Q4: What is the difference between ConcurrentHashMap and Collections.synchronizedMap()?

**Answer:**

**ConcurrentHashMap:**
- Lock striping (segment-level locking)
- Better concurrency
- No locking for reads
- Null keys/values not allowed
- Weakly consistent iterators

**Collections.synchronizedMap():**
- Single lock for entire map
- Poor concurrency
- Locks for both read/write
- Allows null keys/values
- Fail-fast iterators

**Example:**

```java
// ConcurrentHashMap - High Performance
ConcurrentHashMap<String, User> userCache = new ConcurrentHashMap<>();

// Thread-safe operations
userCache.put("user1", new User("John"));
userCache.computeIfAbsent("user2", k -> fetchUserFromDB(k));
userCache.merge("user3", new User("Jane"), (old, new) -> old.merge(new));

// Atomic operations
userCache.compute("user1", (k, v) -> {
    v.incrementLoginCount();
    return v;
});

// Bulk operations
userCache.forEach(10, (k, v) -> processUser(v)); // Parallel threshold

// Collections.synchronizedMap - Lower Performance
Map<String, User> syncMap = Collections.synchronizedMap(new HashMap<>());

// Must synchronize iteration
synchronized(syncMap) {
    for (Map.Entry<String, User> entry : syncMap.entrySet()) {
        processUser(entry.getValue());
    }
}
```

**Real-World Use Case:**

```java
@Service
public class RateLimiter {
    private final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    
    public boolean allowRequest(String userId) {
        AtomicInteger count = requestCounts.computeIfAbsent(userId, k -> new AtomicInteger(0));
        return count.incrementAndGet() <= 100; // 100 requests per window
    }
    
    @Scheduled(fixedRate = 60000) // Reset every minute
    public void resetCounts() {
        requestCounts.clear();
    }
}
```

**Interview Tip:** Emphasize ConcurrentHashMap's superior performance for high-concurrency scenarios.

---

### Q5: Explain ThreadLocal and its use cases. What are the potential memory leak issues?

**Answer:**

**ThreadLocal:**
- Thread-confined variables
- Each thread has its own copy
- No synchronization needed
- Useful for context propagation

**Example:**

```java
// User Context in Web Application
public class UserContext {
    private static final ThreadLocal<User> currentUser = new ThreadLocal<>();
    
    public static void setUser(User user) {
        currentUser.set(user);
    }
    
    public static User getUser() {
        return currentUser.get();
    }
    
    public static void clear() {
        currentUser.remove(); // Important to prevent memory leak
    }
}

// Filter to set context
@Component
public class UserContextFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        try {
            User user = extractUserFromRequest(request);
            UserContext.setUser(user);
            chain.doFilter(request, response);
        } finally {
            UserContext.clear(); // Always clear
        }
    }
}

// Usage in service
@Service
public class AuditService {
    public void logAction(String action) {
        User user = UserContext.getUser();
        log.info("User {} performed {}", user.getId(), action);
    }
}
```

**Memory Leak Issue:**

```java
// BAD - Memory Leak
public class LeakyThreadLocal {
    private static ThreadLocal<List<byte[]>> data = ThreadLocal.withInitial(ArrayList::new);
    
    public void addData() {
        data.get().add(new byte[1024 * 1024]); // 1MB
        // Never cleared - memory leak in thread pools
    }
}

// GOOD - Proper Cleanup
public class SafeThreadLocal {
    private static ThreadLocal<List<byte[]>> data = ThreadLocal.withInitial(ArrayList::new);
    
    public void processData() {
        try {
            data.get().add(new byte[1024 * 1024]);
            // Process data
        } finally {
            data.remove(); // Always remove
        }
    }
}
```

**Interview Tip:** Always mention the importance of calling remove() to prevent memory leaks, especially in thread pools.

---

## 2. JVM Internals

### Q6: Explain JVM memory structure. What are the different memory areas?

**Answer:**

**JVM Memory Areas:**

1. **Heap** - Object storage, GC managed
2. **Method Area (Metaspace)** - Class metadata, static variables
3. **Stack** - Method frames, local variables
4. **PC Register** - Current instruction pointer
5. **Native Method Stack** - Native method calls

**Diagram:**

```
┌─────────────────────────────────────┐
│           JVM Memory                │
├─────────────────────────────────────┤
│  Heap (Shared)                      │
│  ├─ Young Generation                │
│  │  ├─ Eden Space                   │
│  │  ├─ Survivor 0                   │
│  │  └─ Survivor 1                   │
│  └─ Old Generation                  │
├─────────────────────────────────────┤
│  Metaspace (Shared)                 │
│  └─ Class Metadata                  │
├─────────────────────────────────────┤
│  Thread Stacks (Per Thread)         │
│  ├─ Thread 1 Stack                  │
│  ├─ Thread 2 Stack                  │
│  └─ Thread N Stack                  │
├─────────────────────────────────────┤
│  Code Cache                         │
│  └─ JIT Compiled Code               │
└─────────────────────────────────────┘
```

**Example - Memory Configuration:**

```bash
# JVM Tuning Parameters
java -Xms2g \              # Initial heap size
     -Xmx4g \              # Maximum heap size
     -XX:NewRatio=2 \      # Old:Young ratio
     -XX:SurvivorRatio=8 \ # Eden:Survivor ratio
     -XX:MaxMetaspaceSize=512m \
     -XX:+UseG1GC \        # G1 Garbage Collector
     -XX:MaxGCPauseMillis=200 \
     -jar application.jar
```

**Interview Tip:** Draw the memory diagram and explain object lifecycle from Eden → Survivor → Old Gen.

---

### Q7: What is JIT compilation? Explain tiered compilation.

**Answer:**

**JIT (Just-In-Time) Compilation:**
- Compiles bytecode to native machine code at runtime
- Optimizes hot code paths
- Improves performance over interpretation

**Tiered Compilation (Java 8+):**

```
Level 0: Interpreter
Level 1: C1 (Client) Compiler - Quick compilation, basic optimizations
Level 2: C1 with limited profiling
Level 3: C1 with full profiling
Level 4: C2 (Server) Compiler - Aggressive optimizations
```

**Example:**

```java
public class JITExample {
    
    // This method will be JIT compiled after ~10,000 invocations
    public int calculateSum(int n) {
        int sum = 0;
        for (int i = 0; i < n; i++) {
            sum += i;
        }
        return sum;
    }
    
    public static void main(String[] args) {
        JITExample example = new JITExample();
        
        // Warm-up phase - triggers JIT compilation
        for (int i = 0; i < 20000; i++) {
            example.calculateSum(1000);
        }
        
        // Now method is compiled to native code
        long start = System.nanoTime();
        example.calculateSum(1000000);
        long end = System.nanoTime();
        
        System.out.println("Execution time: " + (end - start) + " ns");
    }
}
```

**JIT Optimizations:**
- Inlining
- Dead code elimination
- Loop unrolling
- Escape analysis

**Monitoring JIT:**

```bash
# Enable JIT compilation logs
java -XX:+PrintCompilation \
     -XX:+UnlockDiagnosticVMOptions \
     -XX:+LogCompilation \
     -jar application.jar
```

**Interview Tip:** Mention that JIT makes Java performance comparable to C++ for long-running applications.

---

## 3. Garbage Collection

### Q8: Compare G1GC, ZGC, and Shenandoah GC. When would you use each?

**Answer:**

| Feature | G1GC | ZGC | Shenandoah |
|---------|------|-----|------------|
| **Pause Time** | 10-200ms | <10ms | <10ms |
| **Heap Size** | Up to 64GB | Multi-TB | Multi-TB |
| **Throughput** | High | Medium | Medium |
| **Java Version** | 7+ | 11+ | 12+ |
| **Use Case** | General purpose | Low latency | Low latency |

**Example Configuration:**

```bash
# G1GC (Default in Java 9+)
java -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:G1HeapRegionSize=16m \
     -jar app.jar

# ZGC (Ultra-low latency)
java -XX:+UseZGC \
     -XX:ZCollectionInterval=5 \
     -Xmx16g \
     -jar app.jar

# Shenandoah (Concurrent GC)
java -XX:+UseShenandoahGC \
     -Xmx8g \
     -jar app.jar
```

**When to Use:**

- **G1GC**: Default choice, balanced performance
- **ZGC**: Trading systems, real-time applications (<10ms pause)
- **Shenandoah**: Similar to ZGC, better for smaller heaps

**Interview Tip:** Mention that ZGC and Shenandoah are production-ready for low-latency requirements.

---

## 4. Java 17+ Features

### Q9: Explain Records in Java. How do they differ from regular classes?

**Answer:**

**Records (Java 14+):**
- Immutable data carriers
- Auto-generated constructor, getters, equals, hashCode, toString
- Compact syntax
- Cannot extend other classes

**Example:**

```java
// Traditional Class
public final class User {
    private final String name;
    private final int age;
    
    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    public String getName() { return name; }
    public int getAge() { return age; }
    
    @Override
    public boolean equals(Object o) { /* ... */ }
    @Override
    public int hashCode() { /* ... */ }
    @Override
    public String toString() { /* ... */ }
}

// Record (Equivalent)
public record User(String name, int age) {
    // That's it! Auto-generates everything
}

// Custom validation in Record
public record User(String name, int age) {
    public User {
        if (age < 0) {
            throw new IllegalArgumentException("Age cannot be negative");
        }
    }
}

// Record with custom methods
public record Point(int x, int y) {
    public double distanceFromOrigin() {
        return Math.sqrt(x * x + y * y);
    }
    
    public static Point origin() {
        return new Point(0, 0);
    }
}
```

**Real-World Use Case - DTOs:**

```java
// API Response
public record UserResponse(
    Long id,
    String username,
    String email,
    LocalDateTime createdAt
) {}

// Database Result
public record OrderSummary(
    Long orderId,
    BigDecimal totalAmount,
    int itemCount,
    String status
) {}

@RestController
public class UserController {
    
    @GetMapping("/users/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getCreatedAt()
        );
    }
}
```

**Interview Tip:** Emphasize that Records are perfect for DTOs, API responses, and immutable data structures.

---

### Q10: What are Sealed Classes? Provide a real-world example.

**Answer:**

**Sealed Classes (Java 17+):**
- Restrict which classes can extend/implement
- Exhaustive pattern matching
- Better domain modeling

**Example:**

```java
// Payment hierarchy
public sealed interface Payment 
    permits CreditCardPayment, DebitCardPayment, UPIPayment, WalletPayment {
    
    BigDecimal amount();
    String transactionId();
}

public final class CreditCardPayment implements Payment {
    private final BigDecimal amount;
    private final String transactionId;
    private final String cardNumber;
    private final String cvv;
    
    // Constructor, getters...
}

public final class UPIPayment implements Payment {
    private final BigDecimal amount;
    private final String transactionId;
    private final String upiId;
    
    // Constructor, getters...
}

// Pattern Matching with Sealed Classes
public class PaymentProcessor {
    
    public void processPayment(Payment payment) {
        switch (payment) {
            case CreditCardPayment cc -> processCreditCard(cc);
            case DebitCardPayment dc -> processDebitCard(dc);
            case UPIPayment upi -> processUPI(upi);
            case WalletPayment wallet -> processWallet(wallet);
            // No default needed - compiler knows all cases
        };
    }
}
```

**Real-World Example - Result Type:**

```java
public sealed interface Result<T> permits Success, Failure {
    
    record Success<T>(T value) implements Result<T> {}
    record Failure<T>(String error, Exception cause) implements Result<T> {}
    
    default T getOrThrow() {
        return switch (this) {
            case Success<T> s -> s.value();
            case Failure<T> f -> throw new RuntimeException(f.error(), f.cause());
        };
    }
}

// Usage
public Result<User> findUser(Long id) {
    try {
        User user = userRepository.findById(id);
        return new Result.Success<>(user);
    } catch (Exception e) {
        return new Result.Failure<>("User not found", e);
    }
}
```

**Interview Tip:** Sealed classes provide type-safe domain modeling and exhaustive pattern matching.

---

## 5. Performance Tuning

### Q11: How would you identify and fix a memory leak in a Java application?

**Answer:**

**Steps to Identify Memory Leak:**

1. **Monitor Heap Usage**
2. **Take Heap Dumps**
3. **Analyze with Tools** (VisualVM, MAT, JProfiler)
4. **Identify Retained Objects**
5. **Fix Root Cause**

**Example - Common Memory Leak:**

```java
// BAD - Memory Leak
public class CacheService {
    private static final Map<String, byte[]> cache = new HashMap<>();
    
    public void cacheData(String key, byte[] data) {
        cache.put(key, data); // Never removed - memory leak
    }
}

// GOOD - Fixed with WeakHashMap
public class CacheService {
    private static final Map<String, byte[]> cache = 
        Collections.synchronizedMap(new WeakHashMap<>());
    
    public void cacheData(String key, byte[] data) {
        cache.put(key, data); // Auto-removed when key is GC'd
    }
}

// BETTER - Use Caffeine Cache
public class CacheService {
    private final Cache<String, byte[]> cache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build();
    
    public void cacheData(String key, byte[] data) {
        cache.put(key, data);
    }
}
```

**Monitoring Code:**

```java
@Component
public class MemoryMonitor {
    
    @Scheduled(fixedRate = 60000)
    public void logMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        log.info("Memory Usage: {} MB / {} MB", 
            usedMemory / 1024 / 1024, 
            totalMemory / 1024 / 1024);
        
        if (usedMemory > totalMemory * 0.9) {
            log.warn("High memory usage detected!");
        }
    }
}
```

**Interview Tip:** Mention specific tools (VisualVM, MAT) and common leak patterns (static collections, listeners, ThreadLocal).

---

## 🎯 Quick Reference

### Top 10 Must-Know Topics

1. ✅ Virtual Threads (Project Loom)
2. ✅ CompletableFuture & Async Programming
3. ✅ ConcurrentHashMap internals
4. ✅ JVM Memory Model
5. ✅ G1GC vs ZGC
6. ✅ Records & Sealed Classes
7. ✅ Pattern Matching
8. ✅ Memory Leak Detection
9. ✅ JIT Compilation
10. ✅ ThreadLocal best practices

### Latest Trends (2024-2026)

- **Virtual Threads** - Production-ready in Java 21
- **Pattern Matching** - Enhanced in Java 21
- **Foreign Function & Memory API** - Native interop
- **Vector API** - SIMD operations
- **Structured Concurrency** - Better async code

---

**Next:** [Spring Boot & Microservices Q&A](../02-spring-boot/README.md)
