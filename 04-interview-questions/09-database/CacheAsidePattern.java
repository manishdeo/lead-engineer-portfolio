import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * Example of the Cache-Aside caching strategy.
 * This is the most common caching pattern, where the application code is
 * responsible for reading from and writing to the cache.
 */
@Service
public class CacheAsidePattern<T> {

    private final Cache cache;
    private final Database<T> database; // Mock database repository

    public CacheAsidePattern(CacheManager cacheManager, Database<T> database) {
        this.cache = cacheManager.getCache("myCache");
        this.database = database;
    }

    /**
     * Retrieves an item, applying the Cache-Aside strategy.
     */
    public T getItem(String key) {
        // 1. Look for the item in the cache first
        T item = cache.get(key, (Class<T>) Object.class);
        
        if (item != null) {
            System.out.println("Cache HIT for key: " + key);
            return item;
        }

        System.out.println("Cache MISS for key: " + key);
        
        // 2. If not in cache, fetch from the database
        item = database.readFromDb(key);

        // 3. Store the item in the cache for future requests
        if (item != null) {
            System.out.println("Storing item in cache for key: " + key);
            cache.put(key, item);
        }

        return item;
    }

    /**
     * Writes an item, invalidating the cache to ensure consistency.
     */
    public void writeItem(String key, T item) {
        // 1. Write the item to the database
        database.writeToDb(key, item);
        System.out.println("Wrote item to DB for key: " + key);

        // 2. Invalidate (remove) the corresponding entry from the cache
        // This ensures that the next read will fetch the fresh data from the DB.
        cache.evict(key);
        System.out.println("Invalidated cache for key: " + key);
    }
}

// Mock Database for demonstration
interface Database<T> {
    T readFromDb(String key);
    void writeToDb(String key, T item);
}
