# 🏛️ Architecture Decision Making (50+ Scenarios)

---

### Q1: Monolith vs Microservices — how do you decide?
**A:** Start monolith if: small team (< 10), unclear domain boundaries, speed to market. Move to microservices when: independent scaling needed, team autonomy required, different tech stacks per domain. Use Strangler Fig for migration.

### Q2: When would you choose event-driven over request-response?
**A:** Event-driven when: loose coupling needed, eventual consistency OK, multiple consumers per event, audit trail needed. Request-response when: immediate response required, simple request-reply, strong consistency needed.

### Q3: How do you write an Architecture Decision Record (ADR)?
**A:** Title, Status (proposed/accepted/deprecated), Context (why), Decision (what), Consequences (trade-offs). Store in repo alongside code. Review in architecture guild meetings.

### Q4: SQL vs NoSQL for a new project?
**A:** Default to SQL (PostgreSQL) unless you have a specific reason for NoSQL. NoSQL reasons: massive write throughput, flexible schema, key-value access only, geo-distribution. Most projects are fine with SQL.

### Q5: How do you evaluate build vs buy decisions?
**A:** Build when: core differentiator, unique requirements, long-term cost advantage. Buy when: commodity functionality, faster time to market, maintenance burden not worth it. Always consider total cost of ownership (TCO).
