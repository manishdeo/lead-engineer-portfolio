# 🔵 Google Interview Guide — Lead/Senior Staff Engineer

> Focus: System Design Heavy, Googleyness, Coding

---

## 📋 Interview Structure (L5/L6)

| Round | Duration | Focus |
|-------|----------|-------|
| Phone Screen | 45 min | Coding (LeetCode Medium/Hard) |
| Onsite 1 | 45 min | System Design |
| Onsite 2 | 45 min | System Design |
| Onsite 3 | 45 min | Coding |
| Onsite 4 | 45 min | Behavioral (Googleyness & Leadership) |
| Onsite 5 | 45 min | Technical Leadership / Coding |

---

## 🏗️ System Design — Google Focus

### Most Asked Systems
1. **Design Google Search** — Web crawling, indexing, ranking (PageRank)
2. **Design YouTube** — Video upload, transcoding, streaming, recommendations
3. **Design Google Maps** — Geospatial data, routing, real-time traffic
4. **Design Google Drive** — File storage, sync, sharing, collaboration
5. **Design Gmail** — Email at scale, search, spam filtering
6. **Design Google Docs** — Real-time collaboration (OT/CRDT)

### Google-Specific Expectations
- **Scale:** Think in billions (users, requests, data points)
- **Distributed systems depth:** Consensus (Paxos/Raft), sharding, replication
- **Trade-off analysis:** Always discuss alternatives and why you chose one
- **Data pipeline:** MapReduce, Pub/Sub, BigQuery thinking
- **Infrastructure awareness:** Borg (→ Kubernetes), Spanner, Bigtable, Colossus

### System Design Framework for Google
1. **Clarify scope** — What's in/out? Scale numbers?
2. **API design** — Define the contract first
3. **High-level design** — Components and data flow
4. **Data model** — Schema, storage choice, partitioning
5. **Deep dive** — Pick 2 components, go deep
6. **Scale & reliability** — Replication, sharding, failover
7. **Trade-offs** — Always articulate what you're trading

---

## 🧠 Googleyness & Leadership

### What Google Looks For
- **Intellectual humility** — "I don't know, but here's how I'd find out"
- **Collaborative** — "We" not "I", credit to team
- **Comfort with ambiguity** — Navigate unclear requirements
- **Bias to action** — Ship and iterate
- **Ethical thinking** — Consider user impact, privacy

### Common Questions
- "Tell me about a time you had to influence without authority"
- "Describe a situation where you disagreed with your manager"
- "How do you handle ambiguous requirements?"
- "Tell me about a technical decision you regret"
- "How do you mentor junior engineers?"

---

## 💻 Coding — Google Expectations

### For L5+ (Senior/Staff)
- LeetCode Medium fluently, Hard with hints
- Clean, production-quality code
- Discuss time/space complexity proactively
- Handle edge cases
- Test your solution verbally

### Top Patterns
- Graph algorithms (BFS, DFS, shortest path)
- Dynamic programming
- Sliding window / Two pointers
- Tree traversals
- System design coding (design a class/module)

---

## 📊 Hiring Committee

Google uses a **hiring committee** (not individual interviewers) to make decisions:
- Interviewers submit written feedback
- Committee reviews all feedback holistically
- No single interviewer can veto
- Committee looks for consistent signal across rounds

**Tip:** Every round matters equally. A strong system design can compensate for a weaker coding round.

---

## 🎯 Preparation Checklist

- [ ] Master 2 system designs at Google scale
- [ ] Practice coding daily (LeetCode 2-3 problems)
- [ ] Read Google's engineering blog (research.google)
- [ ] Understand Google's infrastructure papers (Spanner, Bigtable, MapReduce)
- [ ] Prepare 5+ behavioral stories (Googleyness focus)
- [ ] Practice explaining trade-offs clearly
- [ ] Review distributed systems fundamentals (CAP, consensus, replication)
