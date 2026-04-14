# 🚗 Uber Interview Guide — Lead/Staff Engineer

> Focus: Real-time Systems, Geospatial, High Scale

---

## Interview Structure
| Round | Focus |
|-------|-------|
| Phone Screen | Coding + System Design |
| Onsite 1 | System Design (deep, 90 min) |
| Onsite 2 | Coding |
| Onsite 3 | Behavioral + Technical Leadership |
| Onsite 4 | Hiring Manager |

## Uber-Specific Focus Areas

### Real-time Systems
- **Location tracking** — Millions of GPS updates/second
- **Matching algorithm** — Driver-rider matching in real-time
- **ETA calculation** — Graph algorithms, real-time traffic
- **Surge pricing** — Dynamic pricing based on supply/demand
- **Geofencing** — Region-based rules and pricing

### Key Technologies
- **Geospatial indexing** — H3 (Uber's hexagonal grid), Geohash, R-tree
- **Stream processing** — Apache Flink, Kafka Streams
- **Time-series DB** — InfluxDB, M3DB (Uber's custom)
- **Graph algorithms** — Dijkstra, A* for routing

### System Design Questions
1. **Design Uber/Ride-Sharing** — Matching, tracking, pricing, ETA
2. **Design Uber Eats** — Restaurant matching, delivery optimization
3. **Design a Real-time Location Tracker** — GPS ingestion at scale
4. **Design Surge Pricing** — Supply/demand calculation, geofencing
5. **Design a Notification System** — Push notifications at scale

### Distributed Systems Depth
- Uber operates at massive scale (100M+ rides/month)
- Multi-region, multi-datacenter architecture
- Strong focus on reliability and fault tolerance
- Ringpop (consistent hashing), TChannel (RPC framework)

## Preparation Checklist
- [ ] Study Uber's engineering blog (eng.uber.com)
- [ ] Understand geospatial indexing (H3, Geohash)
- [ ] Practice real-time system design
- [ ] Review stream processing concepts
- [ ] Prepare stories about handling scale/reliability
- [ ] Understand supply/demand marketplace dynamics
