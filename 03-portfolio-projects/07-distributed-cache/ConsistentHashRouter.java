import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A basic Consistent Hashing Router used to direct cache requests
 * to the appropriate Redis cluster node.
 */
public class ConsistentHashRouter<T extends Node> {
    private final SortedMap<Integer, T> ring = new TreeMap<>();
    private final HashFunction hashFunction;
    private final int virtualNodes;

    public ConsistentHashRouter(Collection<T> nodes, int virtualNodes, HashFunction hashFunction) {
        this.hashFunction = hashFunction;
        this.virtualNodes = virtualNodes;
        for (T node : nodes) {
            addNode(node);
        }
    }

    public void addNode(T node) {
        for (int i = 0; i < virtualNodes; i++) {
            ring.put(hashFunction.hash(node.getId() + "-" + i), node);
        }
    }

    public void removeNode(T node) {
        for (int i = 0; i < virtualNodes; i++) {
            ring.remove(hashFunction.hash(node.getId() + "-" + i));
        }
    }

    /**
     * Determines which node should store or retrieve the given key.
     */
    public T getNode(String key) {
        if (ring.isEmpty()) {
            return null;
        }
        int hash = hashFunction.hash(key);
        if (!ring.containsKey(hash)) {
            SortedMap<Integer, T> tailMap = ring.tailMap(hash);
            hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        }
        return ring.get(hash);
    }
}

interface Node {
    String getId();
}

interface HashFunction {
    int hash(String key);
}
