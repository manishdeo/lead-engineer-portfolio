package com.maplehub.cache.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Consistent hash ring with virtual nodes for uniform key distribution.
 *
 * Interview Deep-Dive:
 * - TreeMap gives O(log N) lookup via ceilingEntry()
 * - Virtual nodes (default 150 per physical node) prevent hotspots
 * - MD5 produces 128-bit hash → we use first 4 bytes as int for ring position
 * - Adding/removing a node only remaps ~K/N keys (K=total keys, N=nodes)
 *
 * Why not MurmurHash? MD5 is sufficient for distribution quality here;
 * MurmurHash3 is faster but MD5 is more universally understood in interviews.
 */
public class ConsistentHashRing<T> {

    private final TreeMap<Long, T> ring = new TreeMap<>();
    private final Map<T, Integer> nodeVnodeCount = new HashMap<>();
    private final int defaultVirtualNodes;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public ConsistentHashRing(int virtualNodesPerNode) {
        this.defaultVirtualNodes = virtualNodesPerNode;
    }

    public ConsistentHashRing() {
        this(150);
    }

    public void addNode(T node) {
        addNode(node, defaultVirtualNodes);
    }

    public void addNode(T node, int virtualNodes) {
        lock.writeLock().lock();
        try {
            for (int i = 0; i < virtualNodes; i++) {
                long hash = hash(node.toString() + "#" + i);
                ring.put(hash, node);
            }
            nodeVnodeCount.put(node, virtualNodes);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeNode(T node) {
        lock.writeLock().lock();
        try {
            int vnodes = nodeVnodeCount.getOrDefault(node, defaultVirtualNodes);
            for (int i = 0; i < vnodes; i++) {
                long hash = hash(node.toString() + "#" + i);
                ring.remove(hash);
            }
            nodeVnodeCount.remove(node);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Find the node responsible for the given key.
     * Walks clockwise on the ring from hash(key) to find the first node.
     */
    public T getNode(String key) {
        lock.readLock().lock();
        try {
            if (ring.isEmpty()) throw new IllegalStateException("Hash ring is empty");
            long hash = hash(key);
            // ceilingEntry: smallest key >= hash (clockwise walk)
            Map.Entry<Long, T> entry = ring.ceilingEntry(hash);
            // Wrap around to first node if past the end
            return (entry != null) ? entry.getValue() : ring.firstEntry().getValue();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get N distinct nodes for replication (walk clockwise, skip duplicates).
     */
    public List<T> getNodes(String key, int count) {
        lock.readLock().lock();
        try {
            if (ring.isEmpty()) return Collections.emptyList();
            Set<T> result = new LinkedHashSet<>();
            long hash = hash(key);

            SortedMap<Long, T> tailMap = ring.tailMap(hash);
            for (T node : tailMap.values()) {
                result.add(node);
                if (result.size() >= count) return new ArrayList<>(result);
            }
            // Wrap around
            for (T node : ring.values()) {
                result.add(node);
                if (result.size() >= count) break;
            }
            return new ArrayList<>(result);
        } finally {
            lock.readLock().unlock();
        }
    }

    public int getNodeCount() {
        return nodeVnodeCount.size();
    }

    public Set<T> getAllNodes() {
        return Collections.unmodifiableSet(nodeVnodeCount.keySet());
    }

    /**
     * MD5 hash → first 4 bytes as unsigned long for ring position.
     */
    static long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));
            return ((long) (digest[0] & 0xFF) << 24)
                 | ((long) (digest[1] & 0xFF) << 16)
                 | ((long) (digest[2] & 0xFF) << 8)
                 | ((long) (digest[3] & 0xFF));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }
}
