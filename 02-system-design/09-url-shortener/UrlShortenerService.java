import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Example URL Shortener logic using a Base62 encoder.
 * Provides shortening and expanding functionality for systems like Bitly.
 */
@Service
public class UrlShortenerService {

    // Simple in-memory DB for demo. A real system uses PostgreSQL + Redis Cache.
    private final Map<String, String> urlMap = new ConcurrentHashMap<>();
    
    private static final String BASE62_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * Shortens a long URL by generating a unique short ID.
     */
    public String shortenUrl(String longUrl) {
        // MD5 Hash is a common way to generate a unique string
        String hash = generateMD5Hash(longUrl);
        
        // Take the first 6 characters to encode (configurable length)
        String shortCode = base62Encode(Long.parseLong(hash.substring(0, 10), 16));
        
        // Store in DB mapping shortCode -> longUrl
        urlMap.put(shortCode, longUrl);
        
        return "http://short.url/" + shortCode;
    }

    /**
     * Expands a short code back to the original long URL.
     */
    public String expandUrl(String shortUrl) {
        String shortCode = shortUrl.substring(shortUrl.lastIndexOf('/') + 1);
        
        // Retrieve longUrl from DB
        return urlMap.get(shortCode);
    }

    private String generateMD5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            // Convert to a hexadecimal string
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest).substring(0, 8); // Simplified
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not supported", e);
        }
    }

    /**
     * Encodes an ID using Base62.
     * This creates alphanumeric strings like "a1b2C3D".
     */
    private String base62Encode(long id) {
        StringBuilder sb = new StringBuilder();
        while (id > 0) {
            int remainder = (int) (id % 62);
            sb.append(BASE62_CHARS.charAt(remainder));
            id /= 62;
        }
        return sb.reverse().toString();
    }
}
