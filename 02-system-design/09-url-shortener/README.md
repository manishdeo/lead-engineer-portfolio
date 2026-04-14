# 🔗 Design URL Shortener (bit.ly)

---

## Requirements
- Shorten long URLs → 7-char short code
- Redirect short URL → original URL (301)
- Custom aliases (optional)
- Analytics (click count, geo, referrer)
- TTL / expiration
- Scale: 100M URLs/month, 10:1 read/write ratio

## Capacity
```
Writes: 100M/month ≈ 40 URLs/sec
Reads: 1B/month ≈ 400 redirects/sec (peak: 4K/sec)
Storage: 100M × 500 bytes × 5 years ≈ 300 GB
```

## Architecture
```
Client → LB → API Service → Cache (Redis) → DB (PostgreSQL)
                   │                              │
              URL Generator                  Analytics
              (Base62 encode)                (Kafka → ClickHouse)
```

## Key Design

### Short Code Generation
**Option A: Counter + Base62** (recommended)
```
Auto-increment ID: 1000000001
Base62 encode: 1000000001 → "15FTGf"
```
- Unique by design, no collisions
- Predictable (use random offset to avoid sequential guessing)

**Option B: Hash + Truncate**
```
MD5(longUrl) → take first 7 chars of Base62
Collision? → append counter and retry
```

### Database Schema
```sql
CREATE TABLE urls (
    id BIGSERIAL PRIMARY KEY,
    short_code VARCHAR(10) UNIQUE NOT NULL,
    long_url TEXT NOT NULL,
    user_id BIGINT,
    created_at TIMESTAMP DEFAULT NOW(),
    expires_at TIMESTAMP,
    click_count BIGINT DEFAULT 0
);
CREATE INDEX idx_short_code ON urls(short_code);
```

### Redirect Flow
```
GET /abc123
  → Redis lookup (cache hit 90%+) → 301 Redirect
  → Cache miss → PostgreSQL lookup → populate cache → 301 Redirect
  → Not found → 404
  → Async: publish click event to Kafka → Analytics
```

### Caching Strategy
- **Cache-aside** with Redis
- Hot URLs (top 20%) cached with 24h TTL
- Cache hit ratio target: 90%+

## Interview Talking Points
1. **Base62 vs MD5** — trade-offs (predictability vs collision)
2. **301 vs 302 redirect** — 301 = permanent (browser caches), 302 = temporary (better for analytics)
3. **Read-heavy optimization** — Redis cache, CDN for popular URLs
4. **Distributed ID generation** — Snowflake ID, Twitter's approach, range-based allocation
5. **Analytics pipeline** — Async via Kafka, not blocking redirect
