import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Example implementation of Consistent Hashing.
 * Often used in distributed caches or databases to minimize key remapping when nodes are added or removed.
 */
public class ConsistentHashing<T> {
    private final int numberOfReplicas;
    private final SortedMap<Long, T> circle = new TreeMap<>();
    private final MessageDigest md;

    public ConsistentHashing(int numberOfReplicas, Collection<T> nodes) throws NoSuchAlgorithmException {
        this.numberOfReplicas = numberOfReplicas;
        this.md = MessageDigest.getInstance("MD5");
        for (T node : nodes) {
            addNode(node);
        }
    }

    public void addNode(T node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.put(hash(node.toString() + i), node);
        }
    }

    public void removeNode(T node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.remove(hash(node.toString() + i));
        }
    }

    public T getNode(String key) {
        if (circle.isEmpty()) {
            return null;
        }
        long hash = hash(key);
        if (!circle.containsKey(hash)) {
            // Find the next node in the circle
            SortedMap<Long, T> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }

    private long hash(String key) {
        md.reset();
        md.update(key.getBytes());
        byte[] digest = md.digest();
        // Create a 32-bit hash from the MD5 digest
        return ((long) (digest[3] & 0xFF) << 24) |
               ((long) (digest[2] & 0xFF) << 16) |
               ((long) (digest[1] & 0xFF) << 8) |
               ((long) (digest[0] & 0xFF));
    }
}
