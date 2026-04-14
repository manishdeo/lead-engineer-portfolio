package com.maplehub.cache.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplehub.cache.core.ConsistentHashRing;
import com.maplehub.cache.core.DistributedCache;
import com.maplehub.cache.core.NearCache;
import com.maplehub.cache.eviction.EvictionPolicy;
import com.maplehub.cache.eviction.LfuEvictionPolicy;
import com.maplehub.cache.eviction.LruEvictionPolicy;
import com.maplehub.cache.eviction.TtlEvictionPolicy;
import com.maplehub.cache.metrics.CacheMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class CacheConfig {

    @Value("${cache.near.max-size:10000}")
    private int nearCacheMaxSize;

    @Value("${cache.near.ttl-seconds:60}")
    private int nearCacheTtl;

    @Value("${cache.eviction.policy:LRU}")
    private String evictionPolicyName;

    @Value("${cache.eviction.max-size:100000}")
    private int evictionMaxSize;

    @Value("${cache.hash-ring.virtual-nodes:150}")
    private int virtualNodes;

    @Value("${cache.redis.nodes:localhost:6379,localhost:6380,localhost:6381}")
    private List<String> redisNodeAddresses;

    @Bean
    public NearCache nearCache() {
        return new NearCache(nearCacheMaxSize, Duration.ofSeconds(nearCacheTtl));
    }

    @Bean
    public ConsistentHashRing<String> hashRing() {
        ConsistentHashRing<String> ring = new ConsistentHashRing<>(virtualNodes);
        redisNodeAddresses.forEach(ring::addNode);
        return ring;
    }

    @Bean
    public Map<String, StringRedisTemplate> redisNodes() {
        Map<String, StringRedisTemplate> nodes = new HashMap<>();
        for (String address : redisNodeAddresses) {
            String[] parts = address.split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 6379;

            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
            LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
            factory.afterPropertiesSet();

            StringRedisTemplate template = new StringRedisTemplate(factory);
            template.afterPropertiesSet();
            nodes.put(address, template);
        }
        return nodes;
    }

    @Bean
    public EvictionPolicy evictionPolicy() {
        return switch (evictionPolicyName.toUpperCase()) {
            case "LFU" -> new LfuEvictionPolicy(evictionMaxSize);
            case "TTL" -> new TtlEvictionPolicy();
            default -> new LruEvictionPolicy(evictionMaxSize);
        };
    }

    @Bean
    public DistributedCache distributedCache(NearCache nearCache,
                                              ConsistentHashRing<String> hashRing,
                                              Map<String, StringRedisTemplate> redisNodes,
                                              EvictionPolicy evictionPolicy,
                                              CacheMetrics metrics,
                                              ObjectMapper objectMapper) {
        return new DistributedCache(nearCache, hashRing, redisNodes, evictionPolicy, metrics, objectMapper);
    }
}
