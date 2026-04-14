import java.util.HashMap;
import java.util.Map;

/**
 * A Thread-Safe LRU (Least Recently Used) Cache implementation.
 * 
 * This is a classic senior/lead interview question because it combines:
 * 1. Data Structures (HashMap for O(1) access, Doubly Linked List for O(1) eviction).
 * 2. Concurrency (handling multiple threads accessing the cache).
 * 3. System Design (the core logic behind systems like Redis or Memcached).
 */
public class LRUCache<K, V> {

    // Doubly Linked List Node to keep track of access order
    private static class Node<K, V> {
        K key;
        V value;
        Node<K, V> prev;
        Node<K, V> next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private final int capacity;
    // The HashMap provides O(1) access to nodes
    private final Map<K, Node<K, V>> cache;
    // Dummy head and tail nodes simplify edge cases (inserting at head, removing from tail)
    private final Node<K, V> head;
    private final Node<K, V> tail;

    public LRUCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0");
        }
        this.capacity = capacity;
        this.cache = new HashMap<>(capacity);
        
        this.head = new Node<>(null, null);
        this.tail = new Node<>(null, null);
        head.next = tail;
        tail.prev = head;
    }

    /**
     * Gets a value from the cache.
     * Synchronized to ensure thread safety during concurrent reads/updates.
     */
    public synchronized V get(K key) {
        Node<K, V> node = cache.get(key);
        if (node == null) {
            return null; // Cache miss
        }
        
        // Cache hit: Move the accessed node to the head (most recently used)
        moveToHead(node);
        return node.value;
    }

    /**
     * Puts a key-value pair into the cache.
     * Synchronized to ensure thread safety during concurrent writes/evictions.
     */
    public synchronized void put(K key, V value) {
        Node<K, V> node = cache.get(key);

        if (node != null) {
            // Update existing node and move to head
            node.value = value;
            moveToHead(node);
        } else {
            // Insert new node
            Node<K, V> newNode = new Node<>(key, value);
            cache.put(key, newNode);
            addToHead(newNode);

            // Check capacity and evict if necessary
            if (cache.size() > capacity) {
                Node<K, V> lru = removeTail(); // The least recently used item is at the tail
                if (lru != null) {
                    cache.remove(lru.key);
                }
            }
        }
    }

    // --- Helper methods for the Doubly Linked List ---

    private void addToHead(Node<K, V> node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }

    private void removeNode(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private void moveToHead(Node<K, V> node) {
        removeNode(node);
        addToHead(node);
    }

    private Node<K, V> removeTail() {
        if (tail.prev == head) {
            return null; // Empty list
        }
        Node<K, V> res = tail.prev;
        removeNode(res);
        return res;
    }
    
    // --- Demo ---
    public static void main(String[] args) {
        LRUCache<Integer, String> lruCache = new LRUCache<>(2);
        
        lruCache.put(1, "one");
        lruCache.put(2, "two");
        System.out.println("Get 1: " + lruCache.get(1)); // returns "one"
        
        lruCache.put(3, "three");    // evicts key 2 because 1 was recently accessed
        System.out.println("Get 2: " + lruCache.get(2)); // returns null (not found)
        
        lruCache.put(4, "four");     // evicts key 1
        System.out.println("Get 1: " + lruCache.get(1)); // returns null (not found)
        System.out.println("Get 3: " + lruCache.get(3)); // returns "three"
        System.out.println("Get 4: " + lruCache.get(4)); // returns "four"
    }
}
