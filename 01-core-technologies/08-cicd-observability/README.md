# 🔄 CI/CD & Observability — Interview Reference

---

## CI/CD Pipeline

```
Code Push → Build → Unit Test → Integration Test → Security Scan → 
  → Docker Build → Push to Registry → Deploy to Staging → 
  → Smoke Test → Deploy to Production (Canary) → Monitor → Full Rollout
```

### Key Principles
- **Trunk-based development** — Short-lived branches, merge to main frequently
- **Automated everything** — Build, test, deploy, rollback
- **Shift left** — Security scanning, linting in CI (not production)
- **Feature flags** — Decouple deployment from release
- **Immutable artifacts** — Same Docker image from CI to production

### Tools Comparison
| Tool | Best For |
|------|----------|
| GitHub Actions | GitHub-native, simple workflows |
| GitLab CI | GitLab-native, built-in registry |
| Jenkins | Highly customizable, plugin ecosystem |
| ArgoCD | GitOps for Kubernetes |
| Tekton | Cloud-native CI/CD on K8s |

---

## Observability — Three Pillars

### 1. Metrics (Prometheus + Grafana)
```
Application → Micrometer → Prometheus (scrape) → Grafana (visualize)
```
**Key metrics:** RED (Rate, Errors, Duration) + USE (Utilization, Saturation, Errors)

### 2. Logging (ELK Stack)
```
Application → Logback (JSON) → Filebeat → Elasticsearch → Kibana
```
**Best practices:** Structured JSON, correlation IDs, log levels, no PII

### 3. Tracing (Zipkin / Jaeger)
```
Service A → Service B → Service C
   span1  →   span2   →   span3
   └──────── traceId ────────────┘
```
**Implementation:** Micrometer Tracing (Spring Boot 3) auto-propagates W3C Trace Context.

---

## SRE Concepts

| Concept | Definition |
|---------|-----------|
| **SLI** | Service Level Indicator — measurable metric (latency p99) |
| **SLO** | Service Level Objective — target for SLI (p99 < 200ms) |
| **SLA** | Service Level Agreement — contract with consequences |
| **Error Budget** | 100% - SLO = allowed downtime for innovation |
| **Toil** | Repetitive manual work that should be automated |
| **Incident Management** | Detect → Triage → Mitigate → Resolve → Postmortem |
