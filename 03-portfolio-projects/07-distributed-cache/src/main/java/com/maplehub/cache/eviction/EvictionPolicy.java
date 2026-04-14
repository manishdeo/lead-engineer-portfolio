package com.maplehub.cache.eviction;

import com.maplehub.cache.model.CacheEntry;

import java.util.Optional;

/**
 * Strategy interface for cache eviction policies.
 *
 * Interview: Why Strategy pattern here?
 * - Different workloads need different eviction: LRU for general, LFU for skewed access, TTL for time-sensitive
 * - Swappable at runtime without changing cache core logic
 * - Easy to add new policies (e.g., ARC, SLRU) without modifying existing code
 */
public interface EvictionPolicy {

    void onAccess(String key);

    void onPut(String key, CacheEntry entry);

    Optional<String> evict();

    void onRemove(String key);

    String name();
}
