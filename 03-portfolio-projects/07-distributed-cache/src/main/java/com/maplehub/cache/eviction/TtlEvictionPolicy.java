package com.maplehub.cache.eviction;

import com.maplehub.cache.model.CacheEntry;

import java.util.Optional;
import java.util.concurrent.*;

/**
 * TTL-based eviction using a DelayQueue for scheduled cleanup.
 *
 * Interview: TTL eviction strategies
 * 1. Lazy expiration: check on access, don't proactively clean — simple but leaks memory
 * 2. Scheduled cleanup: background thread scans periodically — batched, efficient
 * 3. DelayQueue: entries auto-surface when expired — used here, precise timing
 *
 * Redis uses a hybrid: lazy + probabilistic sampling (10 random keys/sec, delete expired)
 */
public class TtlEvictionPolicy implements EvictionPolicy {

    private final ConcurrentHashMap<String, Long> expirations = new ConcurrentHashMap<>();
    private final DelayQueue<DelayedKey> delayQueue = new DelayQueue<>();

    public TtlEvictionPolicy() {
        // Background cleanup thread
        Thread cleaner = Thread.ofVirtual().name("ttl-cleaner").start(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    DelayedKey expired = delayQueue.take();
                    expirations.remove(expired.key);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    @Override
    public void onAccess(String key) {
        // TTL doesn't change on access
    }

    @Override
    public void onPut(String key, CacheEntry entry) {
        if (entry.getTtlSeconds() > 0) {
            long expiresAt = System.currentTimeMillis() + (entry.getTtlSeconds() * 1000);
            expirations.put(key, expiresAt);
            delayQueue.offer(new DelayedKey(key, entry.getTtlSeconds(), TimeUnit.SECONDS));
        }
    }

    @Override
    public Optional<String> evict() {
        DelayedKey expired = delayQueue.poll();
        if (expired != null) {
            expirations.remove(expired.key);
            return Optional.of(expired.key);
        }
        return Optional.empty();
    }

    @Override
    public void onRemove(String key) {
        expirations.remove(key);
    }

    @Override
    public String name() {
        return "TTL";
    }

    private record DelayedKey(String key, long delay, TimeUnit unit) implements Delayed {
        private static final long createdAt = System.currentTimeMillis();

        @Override
        public long getDelay(TimeUnit unit) {
            long remaining = (createdAt + this.unit.toMillis(delay)) - System.currentTimeMillis();
            return unit.convert(remaining, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.compare(getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS));
        }
    }
}
