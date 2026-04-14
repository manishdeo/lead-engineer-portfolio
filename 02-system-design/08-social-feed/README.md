# 📱 Design Social Media Feed (Twitter/Instagram)

---

## Requirements
- Post content (text, images, video)
- Follow/unfollow users
- News feed (timeline of followed users' posts)
- Like, comment, share
- Scale: 500M users, 200M DAU, 10K celebrity users (1M+ followers)

## The Core Problem: Fan-out

### Fan-out on Write (Push Model)
When user posts → push to all followers' timelines.
```
User A posts → Fan-out Service → Write to:
  Follower 1's timeline (Redis list)
  Follower 2's timeline (Redis list)
  ...
  Follower N's timeline (Redis list)
```
✅ Fast reads (pre-computed). ❌ Expensive for celebrities (1M+ followers).

### Fan-out on Read (Pull Model)
When user opens feed → pull from all followed users.
```
User opens feed → Query Service → Fetch latest posts from:
  Followed User 1's posts
  Followed User 2's posts
  ...
  Merge + Sort + Return top N
```
✅ No write amplification. ❌ Slow reads (N queries per feed load).

### Hybrid Approach (Twitter's actual design)
```
Regular users (< 10K followers) → Fan-out on Write
Celebrities (> 10K followers) → Fan-out on Read

Feed = Pre-computed timeline + Merge(celebrity posts at read time)
```

## Architecture
```
                    ┌──────────────┐
                    │   Client     │
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
                    │  API Gateway │
                    └──────┬───────┘
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                   │
  ┌──────▼──────┐  ┌──────▼──────┐  ┌────────▼────────┐
  │ Post Service│  │ Feed Service│  │ Social Graph    │
  │             │  │             │  │ (Follow/Unfollow)│
  └──────┬──────┘  └──────┬──────┘  └─────────────────┘
         │                │
    ┌────▼────┐    ┌──────▼──────┐
    │ Kafka   │    │   Redis     │
    │ (events)│    │ (timelines) │
    └────┬────┘    └─────────────┘
         │
  ┌──────▼──────┐
  │ Fan-out     │
  │ Service     │
  └─────────────┘
```

## Data Model

### Posts — Cassandra
```
posts_by_user (user_id, post_id, content, media_urls, created_at)
  Partition: user_id, Clustering: post_id DESC
```

### Timeline — Redis
```
Key: timeline:{user_id}
Value: Sorted Set (score = timestamp, member = post_id)
Max size: 800 posts (trim older)
```

### Social Graph — PostgreSQL or Neo4j
```sql
follows (follower_id, followee_id, created_at)
  Index on follower_id (who do I follow?)
  Index on followee_id (who follows me?)
```

## Feed Ranking (2025: AI-powered)
```
Raw Feed (chronological) → Ranking Model → Personalized Feed

Signals:
- Recency (time decay)
- Engagement (likes, comments on similar posts)
- Relationship strength (interaction frequency)
- Content type preference
- Trending/viral score
```

## Interview Talking Points
1. **Fan-out trade-off** — Push vs Pull vs Hybrid, why hybrid wins
2. **Celebrity problem** — Why fan-out on write breaks for 10M+ followers
3. **Feed ranking** — Chronological vs algorithmic, ML signals
4. **Cache invalidation** — When user deletes post, remove from all timelines
5. **Consistency** — Eventual consistency OK for feeds (not financial)
6. **Media storage** — S3 + CDN, different resolutions
7. **Real-time updates** — WebSocket/SSE for new posts, long polling fallback
