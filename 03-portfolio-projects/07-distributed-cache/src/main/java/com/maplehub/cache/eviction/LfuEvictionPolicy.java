package com.maplehub.cache.eviction;

import com.maplehub.cache.model.CacheEntry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LFU eviction using frequency counters with tie-breaking by recency.
 *
 * Interview: LFU implementation approaches
 * 1. HashMap + PriorityQueue: O(log N) eviction — used here for clarity
 * 2. O(1) LFU (LeetCode 460): doubly-linked list per frequency bucket
 * 3. Count-Min Sketch: approximate frequency, O(1), used by Caffeine internally
 *
 * Trade-off vs LRU:
 * - LFU better for skewed distributions (few hot keys accessed repeatedly)
 * - LFU worse for changing access patterns (old popular keys stick around)
 * - Solution: frequency decay (halve counts periodically) or window-LFU
 */
public class LfuEvictionPolicy implements EvictionPolicy {

    private final Map<String, AtomicLong> frequency = new ConcurrentHashMap<>();
    private final Map<String, Long> insertionOrder = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong(0);
    private final int maxSize;

    public LfuEvictionPolicy(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public void onAccess(String key) {
        AtomicLong freq = frequency.get(key);
        if (freq != null) freq.incrementAndGet();
    }

    @Override
    public void onPut(String key, CacheEntry entry) {
        frequency.put(key, new AtomicLong(1));
        insertionOrder.put(key, counter.incrementAndGet());
    }

    @Override
    public synchronized Optional<String> evict() {
        if (frequency.size() <= maxSize) return Optional.empty();

        // Find key with minimum frequency; break ties by insertion order (oldest first)
        String victim = frequency.entrySet().stream()
                .min(Comparator.<Map.Entry<String, AtomicLong>>comparingLong(e -> e.getValue().get())
                        .thenComparingLong(e -> insertionOrder.getOrDefault(e.getKey(), 0L)))
                .map(Map.Entry::getKey)
                .orElse(null);

        if (victim != null) {
            frequency.remove(victim);
            insertionOrder.remove(victim);
            return Optional.of(victim);
        }
        return Optional.empty();
    }

    @Override
    public void onRemove(String key) {
        frequency.remove(key);
        insertionOrder.remove(key);
    }

    @Override
    public String name() {
        return "LFU";
    }
}
