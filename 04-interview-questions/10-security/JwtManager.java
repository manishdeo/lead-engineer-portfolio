import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

/**
 * A manager for creating and validating JSON Web Tokens (JWTs).
 * JWTs are a standard for stateless, secure authentication in modern applications.
 */
public class JwtManager {

    // In a real app, this key should be loaded securely from a config file or secrets manager.
    private final SecretKey secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    /**
     * Generates a JWT for a given subject (e.g., username or user ID).
     */
    public String generateToken(String subject, List<String> roles, long expirationMillis) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .setSubject(subject)
                .claim("roles", roles) // Custom claim for user roles
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validates a JWT and extracts all claims if the token is valid.
     */
    public Claims validateAndExtractClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            // Catches ExpiredJwtException, SignatureException, etc.
            System.err.println("Invalid JWT: " + e.getMessage());
            return null;
        }
    }

    public String getSubject(String token) {
        return getClaim(token, Claims::getSubject);
    }

    public Date getExpirationDate(String token) {
        return getClaim(token, Claims::getExpiration);
    }

    public boolean isTokenExpired(String token) {
        try {
            return getExpirationDate(token).before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    private <T> T getClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = validateAndExtractClaims(token);
        if (claims != null) {
            return claimsResolver.apply(claims);
        }
        return null;
    }
}
