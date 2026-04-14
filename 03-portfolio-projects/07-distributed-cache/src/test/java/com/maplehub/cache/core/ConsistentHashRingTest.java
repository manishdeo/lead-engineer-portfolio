package com.maplehub.cache.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ConsistentHashRingTest {

    private ConsistentHashRing<String> ring;

    @BeforeEach
    void setUp() {
        ring = new ConsistentHashRing<>(150);
        ring.addNode("node-1");
        ring.addNode("node-2");
        ring.addNode("node-3");
    }

    @Test
    void shouldDistributeKeysAcrossNodes() {
        Map<String, Integer> distribution = new HashMap<>();
        int totalKeys = 10_000;

        for (int i = 0; i < totalKeys; i++) {
            String node = ring.getNode("key-" + i);
            distribution.merge(node, 1, Integer::sum);
        }

        assertEquals(3, distribution.size(), "All nodes should receive keys");
        // With 150 virtual nodes, each node should get roughly 33% ± 10%
        distribution.values().forEach(count -> {
            double ratio = (double) count / totalKeys;
            assertTrue(ratio > 0.2 && ratio < 0.5,
                    "Distribution should be roughly uniform, got " + ratio);
        });
    }

    @Test
    void shouldReturnSameNodeForSameKey() {
        String node1 = ring.getNode("consistent-key");
        String node2 = ring.getNode("consistent-key");
        assertEquals(node1, node2, "Same key should always map to same node");
    }

    @Test
    void shouldMinimizeRemappingOnNodeAddition() {
        Map<String, String> before = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            before.put("key-" + i, ring.getNode("key-" + i));
        }

        ring.addNode("node-4");

        int remapped = 0;
        for (int i = 0; i < 1000; i++) {
            if (!before.get("key-" + i).equals(ring.getNode("key-" + i))) {
                remapped++;
            }
        }

        // Ideally ~K/N = 1000/4 = 250 keys remapped. Allow generous margin.
        assertTrue(remapped < 500, "Should remap roughly K/N keys, got " + remapped);
    }

    @Test
    void shouldReturnMultipleDistinctNodesForReplication() {
        List<String> nodes = ring.getNodes("repl-key", 3);
        assertEquals(3, nodes.size());
        assertEquals(3, new HashSet<>(nodes).size(), "Replication nodes should be distinct");
    }

    @Test
    void shouldHandleNodeRemoval() {
        ring.removeNode("node-2");
        assertEquals(2, ring.getNodeCount());
        // All keys should still resolve
        assertNotNull(ring.getNode("any-key"));
    }

    @Test
    void shouldThrowOnEmptyRing() {
        ConsistentHashRing<String> empty = new ConsistentHashRing<>();
        assertThrows(IllegalStateException.class, () -> empty.getNode("key"));
    }
}
