package com.interview.distributed;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.SortedMap;
import java.util.TreeMap;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Examples of Distributed Systems Patterns
 */
public class DistributedPatterns {

    // 1. Circuit Breaker Pattern (Simplified)
    public static class SimpleCircuitBreaker {
        private enum State { CLOSED, OPEN, HALF_OPEN }
        private State state = State.CLOSED;
        private int failureCount = 0;
        private final int failureThreshold = 3;
        private long lastFailureTime = 0;
        private final long resetTimeout = 5000; // 5 seconds

        public String execute(Supplier<String> action) {
            if (state == State.OPEN) {
                if (System.currentTimeMillis() - lastFailureTime > resetTimeout) {
                    state = State.HALF_OPEN;
                } else {
                    return "Fallback response (Circuit is OPEN)";
                }
            }

            try {
                String result = action.get();
                reset();
                return result;
            } catch (Exception e) {
                recordFailure();
                return "Fallback response (Action failed)";
            }
        }

        private void recordFailure() {
            failureCount++;
            lastFailureTime = System.currentTimeMillis();
            if (failureCount >= failureThreshold) {
                state = State.OPEN;
            }
        }

        private void reset() {
            failureCount = 0;
            state = State.CLOSED;
        }
    }

    // 2. Consistent Hashing Pattern
    public static class ConsistentHashing {
        private final TreeMap<Long, String> circle = new TreeMap<>();
        private final int numberOfReplicas;
        private final MessageDigest md;

        public ConsistentHashing(int numberOfReplicas) throws NoSuchAlgorithmException {
            this.numberOfReplicas = numberOfReplicas;
            this.md = MessageDigest.getInstance("MD5");
        }

        public void addNode(String node) {
            for (int i = 0; i < numberOfReplicas; i++) {
                circle.put(hash(node + i), node);
            }
        }

        public void removeNode(String node) {
            for (int i = 0; i < numberOfReplicas; i++) {
                circle.remove(hash(node + i));
            }
        }

        public String getNode(String key) {
            if (circle.isEmpty()) {
                return null;
            }
            long hash = hash(key);
            if (!circle.containsKey(hash)) {
                SortedMap<Long, String> tailMap = circle.tailMap(hash);
                hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
            }
            return circle.get(hash);
        }

        private long hash(String key) {
            md.reset();
            md.update(key.getBytes());
            byte[] digest = md.digest();
            return ((long) (digest[3] & 0xFF) << 24) |
                   ((long) (digest[2] & 0xFF) << 16) |
                   ((long) (digest[1] & 0xFF) << 8) |
                   ((long) (digest[0] & 0xFF));
        }
    }

    @FunctionalInterface
    public interface Supplier<T> {
        T get() throws Exception;
    }
}
