package com.maplehub.cache.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maplehub.cache.model.CacheEntry;

import java.time.Duration;
import java.util.Optional;

/**
 * L1 near-cache using Caffeine — fastest JVM cache (~0.5ms reads).
 *
 * Interview: Why two-level caching?
 * - L1 (Caffeine): in-process, no network hop, ~90% hit rate for hot keys
 * - L2 (Redis): shared across instances, survives restarts
 * - Trade-off: L1 is eventually consistent (TTL-based invalidation)
 *   For stronger consistency, use Redis pub/sub to invalidate L1 on writes.
 */
public class NearCache {

    private final Cache<String, CacheEntry> cache;

    public NearCache(int maxSize, Duration ttl) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttl)
                .recordStats()
                .build();
    }

    public Optional<CacheEntry> get(String key) {
        CacheEntry entry = cache.getIfPresent(key);
        if (entry != null && !entry.isExpired()) {
            entry.recordAccess();
            return Optional.of(entry);
        }
        if (entry != null) cache.invalidate(key);
        return Optional.empty();
    }

    public void put(String key, CacheEntry entry) {
        cache.put(key, entry);
    }

    public void invalidate(String key) {
        cache.invalidate(key);
    }

    public void invalidateAll() {
        cache.invalidateAll();
    }

    public long size() {
        return cache.estimatedSize();
    }

    public double hitRate() {
        return cache.stats().hitRate();
    }

    public long hitCount() {
        return cache.stats().hitCount();
    }

    public long missCount() {
        return cache.stats().missCount();
    }
}
