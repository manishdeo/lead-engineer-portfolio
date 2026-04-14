package com.maplehub.cache.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class CacheMetrics {

    private final Counter l1Hits;
    private final Counter l1Misses;
    private final Counter l2Hits;
    private final Counter l2Misses;
    private final Counter puts;
    private final Counter evictions;
    private final AtomicLong l2HitCount = new AtomicLong();
    private final AtomicLong l2MissCount = new AtomicLong();
    private final AtomicLong putCount = new AtomicLong();

    public CacheMetrics(MeterRegistry registry) {
        this.l1Hits = Counter.builder("cache.hits").tag("level", "L1").register(registry);
        this.l1Misses = Counter.builder("cache.misses").tag("level", "L1").register(registry);
        this.l2Hits = Counter.builder("cache.hits").tag("level", "L2").register(registry);
        this.l2Misses = Counter.builder("cache.misses").tag("level", "L2").register(registry);
        this.puts = Counter.builder("cache.puts").register(registry);
        this.evictions = Counter.builder("cache.evictions").register(registry);
    }

    public void recordHit(String level) {
        if ("L1".equals(level)) l1Hits.increment();
        else { l2Hits.increment(); l2HitCount.incrementAndGet(); }
    }

    public void recordMiss(String level) {
        if ("L1".equals(level)) l1Misses.increment();
        else { l2Misses.increment(); l2MissCount.incrementAndGet(); }
    }

    public void recordPut() { puts.increment(); putCount.incrementAndGet(); }

    public void recordEviction() { evictions.increment(); }

    public long getL2Hits() { return l2HitCount.get(); }
    public long getL2Misses() { return l2MissCount.get(); }
    public long getTotalPuts() { return putCount.get(); }
}
