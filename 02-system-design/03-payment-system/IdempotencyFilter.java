import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple filter to handle Idempotency for payment APIs.
 * In a real distributed system, this would use Redis or a similar distributed cache.
 */
@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private final Map<String, Object> idempotencyCache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String idempotencyKey = request.getHeader("Idempotency-Key");

        if (idempotencyKey == null || !isMutationRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (idempotencyCache.containsKey(idempotencyKey)) {
            // Key exists, return cached response
            // In a real app, you'd serialize and return the full HTTP response
            response.setStatus(HttpServletResponse.SC_CONFLICT); // 409 Conflict
            response.getWriter().write("{\"status\":\"DUPLICATE_REQUEST\", \"idempotencyKey\":\"" + idempotencyKey + "\"}");
            return;
        }

        // First time seeing this key, process the request
        // A more robust solution would wrap the response and cache it on success
        idempotencyCache.put(idempotencyKey, "PROCESSING"); // Mark as processing

        try {
            filterChain.doFilter(request, response);
            // On successful response (e.g., 200 OK), cache the actual response body
            // For simplicity, we'll just mark it as completed.
            idempotencyCache.put(idempotencyKey, "COMPLETED");
        } catch (Exception e) {
            // On failure, remove the key to allow retries
            idempotencyCache.remove(idempotencyKey);
            throw e;
        }
    }

    private boolean isMutationRequest(HttpServletRequest request) {
        String method = request.getMethod();
        return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method);
    }
}
