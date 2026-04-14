# 🟠 Amazon Interview Guide — Lead/Principal Engineer

> Focus: Leadership Principles, System Design, Bar Raiser

---

## 📋 Interview Structure (L6/L7)

| Round | Duration | Focus |
|-------|----------|-------|
| Phone Screen | 60 min | 1 LP + 1 System Design or Coding |
| Loop 1 | 60 min | System Design (deep) |
| Loop 2 | 60 min | Leadership Principles (2-3 LPs) |
| Loop 3 | 60 min | Technical Deep Dive + LP |
| Loop 4 | 60 min | Bar Raiser (LP heavy + culture fit) |
| Loop 5 | 60 min | Hiring Manager (LP + team fit) |

---

## 🎯 Leadership Principles — STAR Format

### Customer Obsession
**Q:** Tell me about a time you went above and beyond for a customer.
**Framework:**
- **S:** Customer-facing system had 2s latency on search
- **T:** Reduce to <200ms without breaking existing functionality
- **A:** Profiled queries, added Redis caching, implemented pagination, A/B tested
- **R:** Latency dropped to 150ms, customer satisfaction +25%, NPS improved

### Ownership
**Q:** Tell me about a time you took ownership of something outside your area.
**Tips:**
- Show you didn't wait to be asked
- Demonstrate end-to-end ownership
- Include measurable impact

### Invent and Simplify
**Q:** Tell me about a time you simplified a complex process.
**Tips:**
- Show creative thinking
- Quantify the simplification (reduced steps, time saved)
- Mention adoption by others

### Are Right, A Lot
**Q:** Tell me about a time you made a decision with incomplete data.
**Tips:**
- Show good judgment
- Explain your reasoning framework
- Acknowledge when you were wrong and what you learned

### Hire and Develop the Best
**Q:** Tell me about a time you mentored someone.
**Tips:**
- Specific mentoring actions (code reviews, 1:1s, stretch assignments)
- Measurable growth of the mentee
- Show you invested time

### Insist on the Highest Standards
**Q:** Tell me about a time you refused to compromise on quality.
**Tips:**
- Show you pushed back constructively
- Explain the standard you set
- Demonstrate the outcome was better

### Think Big
**Q:** Tell me about a time you proposed a bold idea.
**Tips:**
- Show vision beyond immediate task
- Explain how you convinced others
- Quantify the impact

### Bias for Action
**Q:** Tell me about a time you made a decision quickly.
**Tips:**
- Show calculated risk-taking
- Explain reversible vs irreversible decisions
- Demonstrate speed without recklessness

### Dive Deep
**Q:** Tell me about a time you had to dive deep into data to solve a problem.
**Tips:**
- Show technical depth
- Explain the investigation process
- Root cause analysis

### Deliver Results
**Q:** Tell me about a time you delivered a project under tight deadlines.
**Tips:**
- Show prioritization
- Explain trade-offs you made
- Quantify the result

---

## 🏗️ System Design — Amazon Focus Areas

### Most Asked Systems
1. **Design Amazon's Product Search** — Elasticsearch, ranking, personalization
2. **Design Amazon's Order System** — Distributed transactions, inventory management
3. **Design Amazon Prime Video** — Video streaming (similar to Netflix)
4. **Design Amazon's Recommendation Engine** — Collaborative filtering at scale
5. **Design a URL Shortener** — Classic warm-up question
6. **Design a Rate Limiter** — Token bucket, distributed

### Amazon-Specific Patterns
- **Service-Oriented Architecture** — Amazon pioneered microservices
- **Two-Pizza Teams** — Small, autonomous teams
- **Working Backwards** — Start from customer, work backwards to solution
- **Operational Excellence** — Runbooks, alarms, dashboards for every service

---

## 💡 Bar Raiser Tips

The Bar Raiser is an experienced interviewer from a different team. They:
- Have veto power on hiring decisions
- Focus heavily on Leadership Principles
- Look for **"raising the bar"** — would this person make the team better?
- Assess culture fit and long-term potential

**How to impress:**
- Use specific, detailed STAR stories
- Show impact with numbers
- Demonstrate learning from failures
- Show you think about the customer first
- Be authentic — they detect rehearsed answers

---

## 📊 Scoring Criteria

Amazon uses a 1-4 scale per LP:
- **1 — Strong No Hire:** No evidence of LP
- **2 — No Hire:** Weak evidence
- **3 — Hire:** Good evidence, meets bar
- **4 — Strong Hire:** Exceptional evidence, raises bar

**To get an offer:** Need mostly 3s and 4s, no 1s, and Bar Raiser approval.

---

## 🎯 Preparation Checklist

- [ ] Prepare 2 STAR stories per Leadership Principle (28+ stories)
- [ ] Practice system design with Amazon-scale numbers
- [ ] Review Amazon's tech blog (aws.amazon.com/blogs)
- [ ] Understand AWS services deeply (they expect AWS knowledge)
- [ ] Practice "Working Backwards" — start with the customer
- [ ] Prepare questions about the team and role
- [ ] Review your resume — every bullet point is a potential LP question
