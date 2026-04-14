import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Waitlist Management for an Event Platform.
 * Uses a Redis Sorted Set (ZSET) to maintain priority ordering based on timestamp.
 */
@Service
public class WaitlistManager {

    private final RedisTemplate<String, String> redisTemplate;

    public WaitlistManager(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Adds a user to the waitlist for a specific event.
     * The score in the sorted set is the current timestamp to ensure FIFO ordering.
     */
    public void joinWaitlist(String eventId, String userId) {
        String waitlistKey = "event:waitlist:" + eventId;
        long currentTimestamp = System.currentTimeMillis();
        
        // ZADD: Add to sorted set with timestamp as score
        redisTemplate.opsForZSet().add(waitlistKey, userId, currentTimestamp);
    }

    /**
     * Gets the next batch of users from the waitlist when tickets become available.
     * 
     * @param eventId The ID of the event.
     * @param count The number of users to pop from the waitlist.
     * @return A set of user IDs to notify/grant tickets.
     */
    public Set<String> popNextFromWaitlist(String eventId, int count) {
        String waitlistKey = "event:waitlist:" + eventId;
        
        // ZRANGE: Get the lowest scores (oldest timestamps)
        Set<String> users = redisTemplate.opsForZSet().range(waitlistKey, 0, count - 1);
        
        if (users != null && !users.isEmpty()) {
            // ZREM: Remove the selected users from the waitlist
            redisTemplate.opsForZSet().remove(waitlistKey, users.toArray());
        }
        
        return users;
    }

    /**
     * Checks the current position of a user in the waitlist.
     * @return 0-based index of the user, or null if not in waitlist.
     */
    public Long getPositionInWaitlist(String eventId, String userId) {
        String waitlistKey = "event:waitlist:" + eventId;
        // ZRANK: Returns the rank (position) of the member
        return redisTemplate.opsForZSet().rank(waitlistKey, userId);
    }
}
