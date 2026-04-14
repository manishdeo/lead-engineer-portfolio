# 👔 Technical Leadership Interview Questions (60+)

---

## Architecture & Decision Making

### Q1: How do you make architecture decisions?
**A:** I use a structured approach:
1. **Define the problem** — What are we solving? Constraints?
2. **Identify options** — At least 3 alternatives
3. **Evaluate trade-offs** — Performance, cost, complexity, team skill
4. **Document decision** — ADR (Architecture Decision Record)
5. **Validate** — Prototype or proof of concept for risky decisions
6. **Review** — Architecture review with senior engineers

### Q2: How do you handle technical debt?
**A:**
- **Track it** — Maintain a tech debt backlog with impact scores
- **Quantify it** — "This slows feature delivery by 20%"
- **Allocate time** — 20% of sprint capacity for tech debt
- **Prioritize** — Fix debt that blocks current goals first
- **Prevent it** — Code reviews, standards, automated quality gates

### Q3: How do you drive adoption of new technologies?
**A:**
1. Build a **proof of concept** demonstrating value
2. Present **data-driven comparison** (not opinion)
3. Start with a **pilot project** (low-risk, high-visibility)
4. Create **documentation and training**
5. Get **early adopters** to champion it
6. **Iterate** based on feedback

---

## Team Management

### Q4: How do you handle a team member who consistently delivers low-quality code?
**A:**
1. **Private conversation** — Understand root cause (skill gap? motivation? personal issues?)
2. **Set clear expectations** — Define what "quality" means (code review checklist)
3. **Provide support** — Pair programming, mentoring, training
4. **Track progress** — Regular 1:1s with specific feedback
5. **Escalate if needed** — Involve manager if no improvement after support

### Q5: How do you handle disagreements between team members on technical approach?
**A:**
1. Let both sides present their case with **data/evidence**
2. Facilitate discussion focused on **trade-offs, not opinions**
3. If no consensus, use **decision criteria** (performance, maintainability, team skill)
4. Make the call if needed — **"disagree and commit"**
5. Document the decision and rationale

### Q6: How do you onboard new engineers effectively?
**A:**
- **Week 1:** Dev environment setup, architecture overview, first small PR
- **Week 2-3:** Pair programming on real tasks, code review participation
- **Month 1:** Own a small feature end-to-end
- **Month 2-3:** Contribute to design discussions, mentor newer members
- **Buddy system:** Assign an onboarding buddy for questions

---

## Scaling & Process

### Q7: How do you scale engineering processes as the team grows?
**A:**
- **2-5 engineers:** Informal, everyone reviews everything
- **5-10 engineers:** Defined code review process, CI/CD, sprint ceremonies
- **10-20 engineers:** Split into squads, tech leads per squad, architecture guild
- **20+ engineers:** Platform team, shared services, RFC process for cross-team changes

### Q8: How do you balance feature delivery with engineering excellence?
**A:**
- **70/20/10 rule:** 70% features, 20% tech debt, 10% innovation
- **Definition of Done** includes quality (tests, docs, monitoring)
- **Tech debt sprints** — Dedicated sprints quarterly
- **Quality gates** — Automated in CI (coverage, linting, security)

### Q9: How do you handle production incidents?
**A:**
1. **Detect** — Automated alerting (PagerDuty)
2. **Triage** — Severity classification (P1-P4)
3. **Communicate** — Status page, stakeholder updates
4. **Mitigate** — Rollback, feature flag, hotfix
5. **Resolve** — Root cause fix
6. **Postmortem** — Blameless, action items, share learnings

---

## Influence & Communication

### Q10: How do you influence without authority?
**A:**
- **Build credibility** through consistent delivery and expertise
- **Use data** to support proposals (not opinions)
- **Find allies** who share the vision
- **Start small** — Prove value with a pilot
- **Communicate in stakeholder language** (business impact, not tech details)

### Q11: How do you communicate technical decisions to non-technical stakeholders?
**A:**
- Use **analogies** (circuit breaker = "electrical fuse for software")
- Focus on **business impact** ("this reduces downtime from hours to minutes")
- Use **visuals** (architecture diagrams, before/after metrics)
- Avoid jargon, explain trade-offs in terms of **time, cost, risk**
