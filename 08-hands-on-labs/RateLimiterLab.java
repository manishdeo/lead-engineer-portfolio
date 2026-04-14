import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hands-On Lab: Implement a Rate Limiter (Token Bucket Algorithm)
 * 
 * Objective: 
 * Build an in-memory Rate Limiter that restricts requests per client ID.
 * This tests concurrency, data structures, and understanding of standard algorithms.
 */
public class RateLimiterLab {

    private final int maxTokens;
    private final int refillRatePerSecond;
    private final ConcurrentHashMap<String, TokenBucket> clientBuckets = new ConcurrentHashMap<>();

    public RateLimiterLab(int maxTokens, int refillRatePerSecond) {
        this.maxTokens = maxTokens;
        this.refillRatePerSecond = refillRatePerSecond;
    }

    /**
     * Attempts to consume a token for a given client.
     * @param clientId The unique identifier for the user/client.
     * @return True if allowed, False if rate limited (429 Too Many Requests).
     */
    public boolean allowRequest(String clientId) {
        TokenBucket bucket = clientBuckets.computeIfAbsent(
            clientId, 
            k -> new TokenBucket(maxTokens, refillRatePerSecond)
        );
        return bucket.tryConsume();
    }

    /**
     * Inner class representing the Token Bucket logic for a single client.
     */
    private static class TokenBucket {
        private final int capacity;
        private final int refillRate;
        private final AtomicInteger tokens;
        private long lastRefillTimeMillis;

        public TokenBucket(int capacity, int refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.tokens = new AtomicInteger(capacity);
            this.lastRefillTimeMillis = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refill(); // Always try to add tokens before consuming
            
            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsedTimeMillis = now - lastRefillTimeMillis;
            
            // Calculate how many seconds have passed
            long secondsPassed = TimeUnit.MILLISECONDS.toSeconds(elapsedTimeMillis);

            if (secondsPassed > 0) {
                int newTokens = (int) (secondsPassed * refillRate);
                int currentTokens = tokens.get();
                
                // Add tokens, but don't exceed the max capacity
                tokens.set(Math.min(capacity, currentTokens + newTokens));
                
                // Update the last refill time
                lastRefillTimeMillis = now;
            }
        }
    }

    // --- Test the implementation ---
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting Rate Limiter Lab...");
        // Capacity of 5 tokens, refill rate of 2 tokens per second
        RateLimiterLab limiter = new RateLimiterLab(5, 2);
        String client = "user-123";

        // 1. Consume all initial tokens rapidly (Burst)
        for (int i = 1; i <= 6; i++) {
            boolean allowed = limiter.allowRequest(client);
            System.out.printf("Request %d: %s%n", i, allowed ? "ALLOWED" : "REJECTED (Rate Limited)");
        }

        // 2. Wait for refill (e.g., 2.5 seconds should give us ~5 tokens back)
        System.out.println("\nWaiting for 2.5 seconds to refill tokens...");
        Thread.sleep(2500);

        // 3. Try consuming again
        for (int i = 1; i <= 6; i++) {
            boolean allowed = limiter.allowRequest(client);
            System.out.printf("Request %d (After Wait): %s%n", i, allowed ? "ALLOWED" : "REJECTED");
        }
    }
}
