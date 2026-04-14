package com.maplehub.cache.eviction;

import com.maplehub.cache.model.CacheEntry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * LRU eviction using access-order LinkedHashMap.
 *
 * Interview: How does LinkedHashMap LRU work?
 * - accessOrder=true moves accessed entries to tail
 * - Head is always the least recently used → O(1) eviction
 * - All operations (get, put, remove) are O(1)
 * - Trade-off: scan-resistant workloads can pollute cache (one-time reads evict hot keys)
 *   → Solution: use SLRU (segmented LRU) or ARC for scan resistance
 */
public class LruEvictionPolicy implements EvictionPolicy {

    private final LinkedHashMap<String, Boolean> accessOrder;
    private final int maxSize;

    public LruEvictionPolicy(int maxSize) {
        this.maxSize = maxSize;
        this.accessOrder = new LinkedHashMap<>(maxSize, 0.75f, true);
    }

    @Override
    public synchronized void onAccess(String key) {
        accessOrder.get(key); // moves to tail
    }

    @Override
    public synchronized void onPut(String key, CacheEntry entry) {
        accessOrder.put(key, Boolean.TRUE);
    }

    @Override
    public synchronized Optional<String> evict() {
        if (accessOrder.size() <= maxSize) return Optional.empty();
        // Head of access-order LinkedHashMap = least recently used
        Map.Entry<String, Boolean> eldest = accessOrder.entrySet().iterator().next();
        accessOrder.remove(eldest.getKey());
        return Optional.of(eldest.getKey());
    }

    @Override
    public synchronized void onRemove(String key) {
        accessOrder.remove(key);
    }

    @Override
    public String name() {
        return "LRU";
    }
}
