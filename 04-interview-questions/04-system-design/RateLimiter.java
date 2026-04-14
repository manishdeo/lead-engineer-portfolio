import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple in-memory Rate Limiter using the Token Bucket algorithm.
 * This is a common system design interview question to test concurrency and algorithm knowledge.
 */
public class RateLimiter {

    private final int bucketCapacity;
    private final int refillRate; // tokens per second
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimiter(int bucketCapacity, int refillRate) {
        this.bucketCapacity = bucketCapacity;
        this.refillRate = refillRate;
    }

    public boolean allowRequest(String clientId) {
        TokenBucket bucket = buckets.computeIfAbsent(clientId, k -> new TokenBucket(bucketCapacity, refillRate));
        return bucket.tryConsume();
    }

    private static class TokenBucket {
        private final int capacity;
        private final int refillRate;
        private final AtomicInteger currentTokens;
        private long lastRefillTimestamp;

        public TokenBucket(int capacity, int refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.currentTokens = new AtomicInteger(capacity);
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (currentTokens.get() > 0) {
                currentTokens.decrementAndGet();
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long secondsSinceLastRefill = (now - lastRefillTimestamp) / 1000;
            if (secondsSinceLastRefill > 0) {
                int newTokens = (int) (secondsSinceLastRefill * refillRate);
                int currentVal = currentTokens.get();
                int newVal = Math.min(capacity, currentVal + newTokens);
                currentTokens.set(newVal);
                lastRefillTimestamp = now;
            }
        }
    }
}
