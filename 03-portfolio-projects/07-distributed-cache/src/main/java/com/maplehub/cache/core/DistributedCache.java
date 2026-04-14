package com.maplehub.cache.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplehub.cache.eviction.EvictionPolicy;
import com.maplehub.cache.metrics.CacheMetrics;
import com.maplehub.cache.model.CacheEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Two-level distributed cache with consistent hashing.
 *
 * Flow:
 * GET: L1 (NearCache) → miss → ConsistentHashRing.getNode(key) → L2 (Redis) → populate L1
 * PUT: ConsistentHashRing.getNode(key) → write L2 (Redis) → update L1 → eviction check
 *
 * Interview: Cache-aside vs Write-through vs Write-behind
 * - Cache-aside (used here): app manages cache + DB independently. Simple, most common.
 * - Write-through: write to cache, cache synchronously writes to DB. Strong consistency.
 * - Write-behind: write to cache, async batch flush to DB. Best throughput, risk of data loss.
 */
public class DistributedCache {

    private static final Logger log = LoggerFactory.getLogger(DistributedCache.class);

    private final NearCache nearCache;
    private final ConsistentHashRing<String> hashRing;
    private final Map<String, StringRedisTemplate> redisNodes;
    private final EvictionPolicy evictionPolicy;
    private final CacheMetrics metrics;
    private final ObjectMapper objectMapper;

    public DistributedCache(NearCache nearCache,
                            ConsistentHashRing<String> hashRing,
                            Map<String, StringRedisTemplate> redisNodes,
                            EvictionPolicy evictionPolicy,
                            CacheMetrics metrics,
                            ObjectMapper objectMapper) {
        this.nearCache = nearCache;
        this.hashRing = hashRing;
        this.redisNodes = redisNodes;
        this.evictionPolicy = evictionPolicy;
        this.metrics = metrics;
        this.objectMapper = objectMapper;
    }

    public Optional<String> get(String key) {
        // L1: Near-cache lookup
        Optional<CacheEntry> l1 = nearCache.get(key);
        if (l1.isPresent()) {
            metrics.recordHit("L1");
            evictionPolicy.onAccess(key);
            return Optional.of(l1.get().getValue());
        }

        // L2: Redis lookup via consistent hash
        metrics.recordMiss("L1");
        String nodeId = hashRing.getNode(key);
        StringRedisTemplate redis = resolveRedis(nodeId);

        try {
            String json = redis.opsForValue().get(key);
            if (json != null) {
                CacheEntry entry = objectMapper.readValue(json, CacheEntry.class);
                if (!entry.isExpired()) {
                    metrics.recordHit("L2");
                    nearCache.put(key, entry); // Populate L1
                    evictionPolicy.onAccess(key);
                    return Optional.of(entry.getValue());
                }
                redis.delete(key); // Expired in L2
            }
        } catch (Exception e) {
            log.warn("Redis read failed for key={}, node={}: {}", key, nodeId, e.getMessage());
        }

        metrics.recordMiss("L2");
        return Optional.empty();
    }

    public void put(String key, String value, long ttlSeconds) {
        CacheEntry entry = new CacheEntry(key, value, ttlSeconds);

        // Write to L2 (Redis) via consistent hash
        String nodeId = hashRing.getNode(key);
        StringRedisTemplate redis = resolveRedis(nodeId);

        try {
            String json = objectMapper.writeValueAsString(entry);
            if (ttlSeconds > 0) {
                redis.opsForValue().set(key, json, Duration.ofSeconds(ttlSeconds));
            } else {
                redis.opsForValue().set(key, json);
            }
        } catch (Exception e) {
            log.error("Redis write failed for key={}, node={}: {}", key, nodeId, e.getMessage());
            throw new RuntimeException("Cache write failed", e);
        }

        // Update L1
        nearCache.put(key, entry);

        // Eviction tracking
        evictionPolicy.onPut(key, entry);
        evictionPolicy.evict().ifPresent(this::evictKey);

        metrics.recordPut();
    }

    public boolean delete(String key) {
        nearCache.invalidate(key);
        evictionPolicy.onRemove(key);

        String nodeId = hashRing.getNode(key);
        StringRedisTemplate redis = resolveRedis(nodeId);
        Boolean deleted = redis.delete(key);
        return Boolean.TRUE.equals(deleted);
    }

    public void flush() {
        nearCache.invalidateAll();
        redisNodes.values().forEach(r -> {
            try {
                r.getConnectionFactory().getConnection().serverCommands().flushDb();
            } catch (Exception e) {
                log.warn("Flush failed for a node: {}", e.getMessage());
            }
        });
    }

    public Map<String, Object> stats() {
        return Map.of(
                "nearCache.size", nearCache.size(),
                "nearCache.hitRate", nearCache.hitRate(),
                "nearCache.hits", nearCache.hitCount(),
                "nearCache.misses", nearCache.missCount(),
                "hashRing.nodes", hashRing.getNodeCount(),
                "evictionPolicy", evictionPolicy.name(),
                "l2Hits", metrics.getL2Hits(),
                "l2Misses", metrics.getL2Misses(),
                "totalPuts", metrics.getTotalPuts()
        );
    }

    private void evictKey(String key) {
        nearCache.invalidate(key);
        try {
            String nodeId = hashRing.getNode(key);
            resolveRedis(nodeId).delete(key);
            metrics.recordEviction();
        } catch (Exception e) {
            log.warn("Eviction failed for key={}: {}", key, e.getMessage());
        }
    }

    private StringRedisTemplate resolveRedis(String nodeId) {
        StringRedisTemplate redis = redisNodes.get(nodeId);
        if (redis == null) {
            // Fallback to any available node
            return redisNodes.values().iterator().next();
        }
        return redis;
    }
}
