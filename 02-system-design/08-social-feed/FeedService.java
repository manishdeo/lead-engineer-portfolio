import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * News Feed Generation for a Social Media System (like Twitter/Instagram).
 * Implements a hybrid approach: push model (fan-out on write) for active users,
 * and pull model (fan-out on read) for inactive users.
 */
@Service
public class FeedService {

    private final RedisTemplate<String, String> redisTemplate;
    // Repositories for DB access would be here

    public FeedService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Called when a user publishes a new post.
     * Uses a Fan-out on Write approach to update followers' feeds in Redis.
     */
    public void publishPost(String authorId, String postId) {
        long timestamp = System.currentTimeMillis();
        
        // 1. Save post to database (not shown here)
        
        // 2. Get author's active followers
        List<String> activeFollowers = getActiveFollowers(authorId);
        
        // 3. Push to active followers' feeds in Redis (Fan-out)
        for (String followerId : activeFollowers) {
            String feedKey = "feed:user:" + followerId;
            redisTemplate.opsForZSet().add(feedKey, postId, timestamp);
            
            // Limit feed size to keep Redis memory bounded
            redisTemplate.opsForZSet().removeRange(feedKey, 0, -1001); // Keep last 1000
        }
    }

    /**
     * Retrieves the feed for a user.
     */
    public List<String> getUserFeed(String userId, int page, int pageSize) {
        String feedKey = "feed:user:" + userId;
        
        // Use ZREVRANGE to get newest posts first
        long start = (long) page * pageSize;
        long end = start + pageSize - 1;
        
        Set<String> postIds = redisTemplate.opsForZSet().reverseRange(feedKey, start, end);
        
        // In a real system, you'd then fetch the actual post content from the DB or Cache
        // using these IDs.
        return postIds != null ? postIds.stream().collect(Collectors.toList()) : List.of();
    }

    /**
     * Mock method to retrieve followers who have been recently active.
     */
    private List<String> getActiveFollowers(String authorId) {
        // Implementation would typically query a followers table and filter by last active time.
        return List.of("user2", "user3"); 
    }
}
