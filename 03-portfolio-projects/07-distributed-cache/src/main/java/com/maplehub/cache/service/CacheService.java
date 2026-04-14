package com.maplehub.cache.service;

import com.maplehub.cache.core.DistributedCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * Cache service with write-behind batching for write-heavy workloads.
 *
 * Interview: Write-behind pattern
 * - Writes buffered in ConcurrentHashMap, flushed every 500ms
 * - Reduces Redis round-trips by batching multiple writes
 * - Risk: data loss on crash before flush → mitigate with WAL or reduce flush interval
 * - Per-key last-write-wins semantics (latest value in buffer overwrites)
 */
@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    private final DistributedCache cache;
    private final ConcurrentHashMap<String, PendingWrite> writeBuffer = new ConcurrentHashMap<>();
    private final ScheduledExecutorService flusher = Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual().name("write-behind-flusher").factory()
    );

    public CacheService(DistributedCache cache) {
        this.cache = cache;
        // Flush write buffer every 500ms
        flusher.scheduleAtFixedRate(this::flushWriteBuffer, 500, 500, TimeUnit.MILLISECONDS);
    }

    public Optional<String> get(String key) {
        // Check write buffer first (uncommitted writes)
        PendingWrite pending = writeBuffer.get(key);
        if (pending != null) return Optional.of(pending.value);
        return cache.get(key);
    }

    public void put(String key, String value, long ttlSeconds) {
        cache.put(key, value, ttlSeconds);
    }

    public void putAsync(String key, String value, long ttlSeconds) {
        writeBuffer.put(key, new PendingWrite(value, ttlSeconds));
    }

    public boolean delete(String key) {
        writeBuffer.remove(key);
        return cache.delete(key);
    }

    public void flush() {
        flushWriteBuffer();
        cache.flush();
    }

    public Map<String, Object> stats() {
        var stats = new java.util.HashMap<>(cache.stats());
        stats.put("writeBuffer.size", writeBuffer.size());
        return stats;
    }

    private void flushWriteBuffer() {
        if (writeBuffer.isEmpty()) return;
        int count = 0;
        var snapshot = new ConcurrentHashMap<>(writeBuffer);
        writeBuffer.clear();

        for (var entry : snapshot.entrySet()) {
            try {
                cache.put(entry.getKey(), entry.getValue().value, entry.getValue().ttlSeconds);
                count++;
            } catch (Exception e) {
                log.error("Write-behind flush failed for key={}: {}", entry.getKey(), e.getMessage());
                writeBuffer.putIfAbsent(entry.getKey(), entry.getValue()); // Re-queue on failure
            }
        }
        if (count > 0) log.debug("Write-behind flushed {} entries", count);
    }

    private record PendingWrite(String value, long ttlSeconds) {}
}
