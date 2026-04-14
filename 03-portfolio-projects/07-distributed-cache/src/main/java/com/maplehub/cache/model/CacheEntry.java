package com.maplehub.cache.model;

import java.io.Serializable;
import java.time.Instant;

/**
 * Cache entry wrapping the actual value with metadata for eviction decisions.
 *
 * Interview: Why wrap values?
 * - TTL tracking without relying solely on Redis EXPIRE
 * - Access frequency for LFU eviction
 * - Creation time for cache stampede probabilistic early expiration
 */
public class CacheEntry implements Serializable {

    private final String key;
    private final String value;
    private final Instant createdAt;
    private final long ttlSeconds;
    private volatile long accessCount;
    private volatile Instant lastAccessedAt;

    public CacheEntry(String key, String value, long ttlSeconds) {
        this.key = key;
        this.value = value;
        this.ttlSeconds = ttlSeconds;
        this.createdAt = Instant.now();
        this.lastAccessedAt = this.createdAt;
        this.accessCount = 0;
    }

    public void recordAccess() {
        this.accessCount++;
        this.lastAccessedAt = Instant.now();
    }

    public boolean isExpired() {
        if (ttlSeconds <= 0) return false;
        return Instant.now().isAfter(createdAt.plusSeconds(ttlSeconds));
    }

    /**
     * Probabilistic early expiration to prevent cache stampede.
     * Returns true if this entry should be refreshed early.
     *
     * Formula: now > expiry - (ttl * beta * ln(random))
     * beta = 1.0 gives ~63% chance of early refresh in last beta*ttl seconds
     */
    public boolean shouldRefreshEarly(double beta) {
        if (ttlSeconds <= 0) return false;
        Instant expiry = createdAt.plusSeconds(ttlSeconds);
        double gap = beta * ttlSeconds * Math.log(Math.random());
        Instant threshold = expiry.plusSeconds((long) gap);
        return Instant.now().isAfter(threshold);
    }

    public String getKey() { return key; }
    public String getValue() { return value; }
    public Instant getCreatedAt() { return createdAt; }
    public long getTtlSeconds() { return ttlSeconds; }
    public long getAccessCount() { return accessCount; }
    public Instant getLastAccessedAt() { return lastAccessedAt; }
}
