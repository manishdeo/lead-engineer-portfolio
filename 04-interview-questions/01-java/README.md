# ☕ Java Advanced Interview Questions

## Concurrency & Multithreading (50 Questions)

### Q1: Explain the difference between synchronized and ReentrantLock
**Answer:**
- **synchronized**: Built-in keyword, automatic lock release, simpler syntax
- **ReentrantLock**: More flexible, tryLock(), timed locking, interruptible locks

### Q2: What is the happens-before relationship?
**Answer:**
Guarantees memory visibility between threads:
- Program order rule
- Monitor lock rule  
- Volatile variable rule
- Thread start/join rules

### Q3: Explain ThreadLocal and its use cases
**Answer:**
- Thread-confined variables
- Each thread has its own copy
- Use cases: User sessions, database connections
- Memory leak risk if not cleaned up

## JVM & Memory Management (40 Questions)

### Q4: Explain JVM memory areas
**Answer:**
- **Heap**: Object storage (Young Gen, Old Gen)
- **Method Area**: Class metadata, constant pool
- **Stack**: Method frames, local variables
- **PC Register**: Current instruction pointer
- **Native Method Stack**: JNI calls

### Q5: What are the different types of garbage collectors?
**Answer:**
- **Serial GC**: Single-threaded, small applications
- **Parallel GC**: Multi-threaded, throughput focused
- **G1 GC**: Low-latency, large heaps
- **ZGC/Shenandoah**: Ultra-low latency collectors

## Collections Framework (30 Questions)

### Q6: HashMap vs ConcurrentHashMap implementation
**Answer:**
- **HashMap**: Not thread-safe, allows null keys/values
- **ConcurrentHashMap**: Thread-safe, segment-based locking (Java 7), CAS operations (Java 8+)

### Q7: Explain fail-fast vs fail-safe iterators
**Answer:**
- **Fail-fast**: Throw ConcurrentModificationException on structural modification
- **Fail-safe**: Work on copy, don't reflect concurrent changes

## Design Patterns (30 Questions)

### Q8: Implement Singleton with double-checked locking
**Answer:**
```java
public class Singleton {
    private static volatile Singleton instance;
    
    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

### Q9: Explain Builder pattern advantages
**Answer:**
- Handles complex object construction
- Immutable objects
- Optional parameters
- Readable code

## Performance & Optimization

### Q10: JVM tuning parameters for production
**Answer:**
```bash
-Xms4g -Xmx4g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+HeapDumpOnOutOfMemoryError
```

## Advanced Topics

### Q11: Explain CompletableFuture and its benefits
**Answer:**
- Asynchronous programming
- Composable operations
- Exception handling
- Better than raw Future

### Q12: What is Project Loom and Virtual Threads?
**Answer:**
- Lightweight threads (fibers)
- Millions of concurrent threads
- Structured concurrency
- Available in Java 19+