package com.interview.java.concurrency;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Producer-Consumer Problem - Classic Interview Question
 * Demonstrates: BlockingQueue, wait/notify, synchronization
 */
public class ProducerConsumerExample {
    
    private final BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);
    private volatile boolean running = true;
    
    // Method 1: Using BlockingQueue (Recommended)
    class Producer implements Runnable {
        @Override
        public void run() {
            int value = 0;
            while (running) {
                try {
                    queue.put(value++);
                    System.out.println("Produced: " + (value - 1));
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    
    class Consumer implements Runnable {
        @Override
        public void run() {
            while (running) {
                try {
                    Integer value = queue.take();
                    System.out.println("Consumed: " + value);
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        ProducerConsumerExample example = new ProducerConsumerExample();
        
        Thread producer = new Thread(example.new Producer());
        Thread consumer = new Thread(example.new Consumer());
        
        producer.start();
        consumer.start();
        
        Thread.sleep(2000);
        example.running = false;
        
        producer.interrupt();
        consumer.interrupt();
    }
}

/**
 * Thread Pool Example - Common Interview Topic
 * Demonstrates: ExecutorService, Future, CompletableFuture
 */
class ThreadPoolExample {
    
    public static void demonstrateThreadPool() {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        
        // Submit tasks and get Future
        Future<String> future1 = executor.submit(() -> {
            Thread.sleep(1000);
            return "Task 1 completed";
        });
        
        Future<String> future2 = executor.submit(() -> {
            Thread.sleep(500);
            return "Task 2 completed";
        });
        
        try {
            System.out.println(future1.get()); // Blocking call
            System.out.println(future2.get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        
        executor.shutdown();
    }
    
    // CompletableFuture - Modern Approach
    public static void demonstrateCompletableFuture() {
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
            return "Async Task 1";
        });
        
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            try { Thread.sleep(500); } catch (InterruptedException e) {}
            return "Async Task 2";
        });
        
        // Combine results
        CompletableFuture<String> combined = future1.thenCombine(future2, 
            (result1, result2) -> result1 + " + " + result2);
        
        combined.thenAccept(System.out::println);
        
        // Wait for completion
        combined.join();
    }
}

/**
 * Custom Thread-Safe Cache - Interview Favorite
 * Demonstrates: ConcurrentHashMap, ReentrantLock, double-checked locking
 */
class ThreadSafeCache<K, V> {
    private final ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    
    public V get(K key, Supplier<V> valueSupplier) {
        V value = cache.get(key);
        if (value == null) {
            lock.lock();
            try {
                // Double-checked locking
                value = cache.get(key);
                if (value == null) {
                    value = valueSupplier.get();
                    cache.put(key, value);
                }
            } finally {
                lock.unlock();
            }
        }
        return value;
    }
    
    public void put(K key, V value) {
        cache.put(key, value);
    }
    
    public V remove(K key) {
        return cache.remove(key);
    }
    
    public int size() {
        return cache.size();
    }
}

/**
 * Deadlock Example and Prevention
 * Common interview question: How to prevent deadlock?
 */
class DeadlockExample {
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();
    
    // This can cause deadlock
    public void method1() {
        synchronized (lock1) {
            System.out.println("Thread " + Thread.currentThread().getName() + " acquired lock1");
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            
            synchronized (lock2) {
                System.out.println("Thread " + Thread.currentThread().getName() + " acquired lock2");
            }
        }
    }
    
    public void method2() {
        synchronized (lock2) {
            System.out.println("Thread " + Thread.currentThread().getName() + " acquired lock2");
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            
            synchronized (lock1) {
                System.out.println("Thread " + Thread.currentThread().getName() + " acquired lock1");
            }
        }
    }
    
    // Deadlock prevention: Always acquire locks in same order
    public void safeMethod1() {
        synchronized (lock1) {
            synchronized (lock2) {
                // Safe execution
            }
        }
    }
    
    public void safeMethod2() {
        synchronized (lock1) {  // Same order as safeMethod1
            synchronized (lock2) {
                // Safe execution
            }
        }
    }
}

/**
 * Interview Question: Implement a Rate Limiter
 * Token Bucket Algorithm Implementation
 */
class RateLimiter {
    private final int maxTokens;
    private final int refillRate;
    private int currentTokens;
    private long lastRefillTime;
    private final ReentrantLock lock = new ReentrantLock();
    
    public RateLimiter(int maxTokens, int refillRate) {
        this.maxTokens = maxTokens;
        this.refillRate = refillRate;
        this.currentTokens = maxTokens;
        this.lastRefillTime = System.currentTimeMillis();
    }
    
    public boolean tryAcquire() {
        lock.lock();
        try {
            refillTokens();
            if (currentTokens > 0) {
                currentTokens--;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }
    
    private void refillTokens() {
        long now = System.currentTimeMillis();
        long timePassed = now - lastRefillTime;
        int tokensToAdd = (int) (timePassed / 1000 * refillRate);
        
        if (tokensToAdd > 0) {
            currentTokens = Math.min(maxTokens, currentTokens + tokensToAdd);
            lastRefillTime = now;
        }
    }
}