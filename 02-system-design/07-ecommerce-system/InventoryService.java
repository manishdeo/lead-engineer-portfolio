import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Inventory Management for an E-commerce Platform.
 * Uses Redis atomic operations to prevent overselling during high-traffic events like flash sales.
 */
@Service
public class InventoryService {

    private final RedisTemplate<String, String> redisTemplate;
    // In a real system, you'd also have a repository to persist to a database like PostgreSQL.

    public InventoryService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Attempts to reserve stock for a given product.
     * This is a critical operation that must be atomic.
     *
     * @param productId The ID of the product.
     * @param quantity  The quantity to reserve.
     * @return true if stock was successfully reserved, false otherwise.
     */
    public boolean reserveStock(String productId, int quantity) {
        String stockKey = "inventory:stock:" + productId;

        // Using Redis DECRBY for an atomic decrement operation.
        // This is a pessimistic locking approach suitable for high-contention items.
        Long newStock = redisTemplate.opsForValue().decrement(stockKey, quantity);

        if (newStock != null && newStock >= 0) {
            // Successfully reserved stock.
            // A background job or event would later persist this change to the main database.
            return true;
        } else {
            // Failed to reserve stock (insufficient quantity).
            // We must revert the decrement to correct the count.
            if (newStock != null) {
                redisTemplate.opsForValue().increment(stockKey, quantity);
            }
            return false;
        }
    }

    /**
     * Releases previously reserved stock.
     * Called if a user's cart expires or a payment fails.
     */
    public void releaseStock(String productId, int quantity) {
        String stockKey = "inventory:stock:" + productId;
        redisTemplate.opsForValue().increment(stockKey, quantity);
    }

    /**
     * Sets the initial stock level for a product.
     * Typically called by an admin or when a product is first created.
     */
    public void setStock(String productId, int initialStock) {
        String stockKey = "inventory:stock:" + productId;
        redisTemplate.opsForValue().set(stockKey, String.valueOf(initialStock));
    }
}
