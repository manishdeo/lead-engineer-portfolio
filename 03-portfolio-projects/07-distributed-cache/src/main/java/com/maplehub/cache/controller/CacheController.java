package com.maplehub.cache.controller;

import com.maplehub.cache.service.CacheService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cache")
public class CacheController {

    private final CacheService cacheService;

    public CacheController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @GetMapping("/{key}")
    public ResponseEntity<Map<String, String>> get(@PathVariable String key) {
        return cacheService.get(key)
                .map(v -> ResponseEntity.ok(Map.of("key", key, "value", v)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{key}")
    public ResponseEntity<Void> put(@PathVariable String key,
                                     @RequestBody PutRequest request) {
        long ttl = request.ttlSeconds() != null ? request.ttlSeconds() : 0;
        if (Boolean.TRUE.equals(request.async())) {
            cacheService.putAsync(key, request.value(), ttl);
        } else {
            cacheService.put(key, request.value(), ttl);
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        boolean deleted = cacheService.delete(key);
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return cacheService.stats();
    }

    @PostMapping("/flush")
    public ResponseEntity<Void> flush() {
        cacheService.flush();
        return ResponseEntity.ok().build();
    }

    record PutRequest(String value, Long ttlSeconds, Boolean async) {}
}
