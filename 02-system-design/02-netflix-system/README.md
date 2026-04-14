# 🎬 Design Netflix / Video Streaming Platform

> Complete system design for a video streaming platform handling 200M+ users

---

## 1. Requirements

### Functional
- User registration & profiles
- Video upload & transcoding
- Video catalog & search
- Video streaming (adaptive bitrate)
- Recommendations engine
- Watch history & continue watching
- Offline downloads

### Non-Functional
- **Availability:** 99.99% (52 min downtime/year)
- **Latency:** Video start < 2s, search < 200ms
- **Scale:** 200M users, 15M concurrent streams
- **Storage:** 100PB+ video content
- **Bandwidth:** 15% of global internet traffic

---

## 2. Capacity Estimation

```
Users: 200M total, 100M DAU
Concurrent streams: 15M peak
Average video: 1 hour, 3 GB (multiple qualities)
New content/day: 1000 videos
Storage growth: 3 TB/day (1000 × 3 GB)
Bandwidth: 15M × 5 Mbps = 75 Tbps peak
```

---

## 3. High-Level Architecture

```
┌──────────┐     ┌──────────────┐     ┌──────────────┐
│  Client   │────▶│   CDN Edge   │────▶│  Origin      │
│ (App/Web) │     │  (Netflix    │     │  Servers     │
│           │◀────│   Open       │     │              │
└──────────┘     │   Connect)   │     └──────────────┘
                  └──────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
┌───────▼──────┐ ┌─────▼──────┐ ┌──────▼──────┐
│ API Gateway  │ │ Streaming  │ │ Transcoding │
│ (Zuul/Kong)  │ │ Service    │ │ Pipeline    │
└───────┬──────┘ └────────────┘ └─────────────┘
        │
┌───────┼───────────────┬──────────────┐
│       │               │              │
▼       ▼               ▼              ▼
User    Catalog      Recommend.    Search
Service Service      Service       Service
│       │               │              │
▼       ▼               ▼              ▼
PostgreSQL  Cassandra   ML Models   Elasticsearch
            + S3                    
```

---

## 4. Core Components

### 4.1 Video Upload & Transcoding Pipeline

```
Upload → S3 Raw → SQS → Transcoding Workers → S3 Transcoded → CDN
                          │
                    ┌─────┼─────┐
                    │     │     │
                  1080p  720p  480p  (Adaptive Bitrate)
                    │     │     │
                    └─────┼─────┘
                          │
                    HLS/DASH Segments
                    (2-10 sec chunks)
```

**Key decisions:**
- **Adaptive Bitrate Streaming (ABR):** HLS or MPEG-DASH
- **Chunked encoding:** 2-10 second segments for quick quality switching
- **Multiple qualities:** 4K, 1080p, 720p, 480p, 240p
- **Codec:** H.264 (compatibility) + H.265/AV1 (efficiency)

### 4.2 Content Delivery Network (CDN)

```
User Request → DNS → Nearest CDN Edge → Cache Hit? → Stream
                                          │ Miss
                                          ▼
                                    Origin Server → CDN Cache → Stream
```

**Netflix Open Connect:**
- Custom CDN appliances placed in ISP data centers
- Pre-populate popular content during off-peak hours
- 95%+ cache hit ratio
- Reduces origin bandwidth by 95%

### 4.3 Video Streaming Service

```
1. Client requests manifest file (HLS .m3u8 / DASH .mpd)
2. Manifest contains URLs for each quality level's segments
3. Client measures bandwidth → selects appropriate quality
4. Client requests segments sequentially
5. On bandwidth change → switch quality at next segment boundary
```

### 4.4 Recommendation Engine

```
User Behavior → Kafka → Feature Store → ML Models → Recommendations
                                            │
                                    ┌───────┼───────┐
                                    │       │       │
                              Collaborative  Content  Trending
                              Filtering     Based    
```

**Algorithms:**
- Collaborative filtering (users who watched X also watched Y)
- Content-based (genre, actors, director similarity)
- Deep learning (neural collaborative filtering)
- A/B testing for recommendation quality

### 4.5 Search Service

```
Query → API Gateway → Search Service → Elasticsearch
                                            │
                                    ┌───────┼───────┐
                                    │       │       │
                                  Title   Genre   Actor
                                  Match   Filter  Filter
```

---

## 5. Database Design

### User Service — PostgreSQL
```sql
users (id, email, name, plan_type, created_at)
profiles (id, user_id, name, avatar, preferences)
watch_history (id, profile_id, video_id, progress_seconds, watched_at)
```

### Catalog Service — Cassandra
```
videos (video_id, title, description, genre, release_year, duration, ratings)
  -- Partition key: video_id
  -- Wide rows for metadata

video_by_genre (genre, release_year, video_id, title, rating)
  -- Partition key: genre, Clustering: release_year DESC
```

**Why Cassandra?**
- High read throughput for catalog browsing
- Tunable consistency (eventual OK for catalog)
- Linear scalability
- Multi-region replication

### Watch History — Cassandra
```
watch_history (profile_id, video_id, progress, timestamp)
  -- Partition key: profile_id
  -- Clustering: timestamp DESC
```

---

## 6. Key Design Decisions

### Why Microservices?
- 1000+ engineers, need team autonomy
- Different scaling needs (streaming vs search vs recommendations)
- Independent deployment (deploy 100+ times/day)

### Why Cassandra over DynamoDB?
- Multi-region active-active replication
- Tunable consistency per query
- No vendor lock-in
- Better for wide-column time-series data (watch history)

### Why Custom CDN (Open Connect)?
- Control over hardware and caching strategy
- Pre-positioning content reduces latency
- Cost savings at Netflix scale (vs paying Akamai/CloudFront)

### Handling 15M Concurrent Streams
- CDN serves 95%+ of traffic (not origin)
- Stateless streaming service scales horizontally
- Connection pooling and HTTP/2 multiplexing
- Regional deployment (3+ regions)

---

## 7. Scalability & Reliability

### Multi-Region Architecture
```
Region 1 (US-East)     Region 2 (EU-West)     Region 3 (AP-Southeast)
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│ Full Stack   │◄─────▶│ Full Stack   │◄─────▶│ Full Stack   │
│ + Cassandra  │       │ + Cassandra  │       │ + Cassandra  │
│   Replica    │       │   Replica    │       │   Replica    │
└──────────────┘       └──────────────┘       └──────────────┘
```

### Chaos Engineering (Simian Army)
- **Chaos Monkey:** Randomly kills instances
- **Chaos Kong:** Simulates region failure
- **Latency Monkey:** Injects artificial delays

### Circuit Breaker (Hystrix → Resilience4j)
- Every inter-service call wrapped with circuit breaker
- Fallback: show cached recommendations, generic content

---

## 8. Interview Talking Points

1. **CDN strategy** — Why Netflix built Open Connect vs using commercial CDN
2. **Adaptive bitrate** — How client switches quality seamlessly
3. **Transcoding pipeline** — Parallel encoding, cost optimization
4. **Recommendation at scale** — Real-time vs batch, A/B testing
5. **Multi-region** — Active-active with Cassandra, failover strategy
6. **Chaos engineering** — How Netflix ensures reliability
7. **Microservices at scale** — 1000+ services, deployment strategy
8. **Cost optimization** — CDN placement, codec selection, storage tiering

---

## 9. Follow-up Questions

**Q: How would you handle live streaming (like Netflix Live)?**
A: Different pipeline — RTMP ingest → real-time transcoding → HLS/DASH → CDN. Lower latency requirements (5-30s). Use WebRTC for ultra-low latency.

**Q: How do you prevent content piracy?**
A: DRM (Widevine, FairPlay, PlayReady), watermarking, token-based URL signing, geo-restrictions.

**Q: How do you handle the "thundering herd" when a popular show drops?**
A: Pre-warm CDN caches, staggered release by region, request coalescing at origin.
