import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Example of Distributed Seat Locking using Redis SETNX.
 * Prevents double booking in a Ticket Booking System like BookMyShow.
 */
@Service
public class DistributedSeatLock {

    private final RedisTemplate<String, String> redisTemplate;
    
    // Default hold time for a seat while checking out
    private static final Duration LOCK_EXPIRY = Duration.ofMinutes(10);

    public DistributedSeatLock(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Tries to acquire a lock for a specific seat in a show.
     * 
     * @param showId The ID of the show/event.
     * @param seatId The ID of the specific seat.
     * @param userId The ID of the user attempting to hold the seat.
     * @return true if the lock was acquired, false if the seat is already held.
     */
    public boolean lockSeat(String showId, String seatId, String userId) {
        String key = String.format("seat:lock:%s:%s", showId, seatId);
        
        // SETNX: Set only if the key doesn't exist
        // Returns true if the key was set (lock acquired), false otherwise
        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(key, userId, LOCK_EXPIRY);
        
        return Boolean.TRUE.equals(lockAcquired);
    }

    /**
     * Releases the lock on a seat. Called either when the booking is confirmed or canceled.
     * 
     * @param showId The ID of the show/event.
     * @param seatId The ID of the specific seat.
     * @param userId The ID of the user who holds the lock.
     */
    public void releaseSeat(String showId, String seatId, String userId) {
        String key = String.format("seat:lock:%s:%s", showId, seatId);
        String currentHolder = redisTemplate.opsForValue().get(key);
        
        // Ensure only the user who holds the lock can release it
        if (userId.equals(currentHolder)) {
            redisTemplate.delete(key);
        }
    }
}
