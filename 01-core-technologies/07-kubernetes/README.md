# ☸️ Kubernetes & Docker — Interview Reference

---

## Kubernetes Architecture

```
┌─────────────────── Control Plane ───────────────────┐
│  API Server ← etcd (state store)                     │
│  Scheduler → Controller Manager                      │
└──────────────────────────────────────────────────────┘
        │
┌───────▼──────── Worker Nodes ───────────────────────┐
│  kubelet → Container Runtime (containerd)            │
│  kube-proxy → Pod Network (CNI)                      │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐             │
│  │  Pod A   │  │  Pod B   │  │  Pod C   │             │
│  │┌───────┐│  │┌───────┐│  │┌───────┐│             │
│  ││Container│  ││Container│  ││Container│             │
│  │└───────┘│  │└───────┘│  │└───────┘│             │
│  └─────────┘  └─────────┘  └─────────┘             │
└──────────────────────────────────────────────────────┘
```

## Key Concepts

| Concept | Purpose |
|---------|---------|
| **Pod** | Smallest deployable unit (1+ containers) |
| **Deployment** | Manages ReplicaSets, rolling updates |
| **Service** | Stable network endpoint for pods (ClusterIP, NodePort, LoadBalancer) |
| **Ingress** | HTTP routing, TLS termination |
| **ConfigMap** | Non-sensitive configuration |
| **Secret** | Sensitive data (base64 encoded) |
| **PersistentVolume** | Storage abstraction |
| **HPA** | Horizontal Pod Autoscaler (CPU/memory based) |
| **Namespace** | Logical isolation within cluster |
| **DaemonSet** | One pod per node (logging, monitoring agents) |
| **StatefulSet** | Ordered, stable pods (databases) |
| **Job/CronJob** | Batch / scheduled tasks |

## Deployment Strategies

| Strategy | How | Use When |
|----------|-----|----------|
| **Rolling Update** | Replace pods gradually | Default, zero-downtime |
| **Blue/Green** | Two full environments, switch traffic | Need instant rollback |
| **Canary** | Route small % to new version | Risk-sensitive deployments |
| **A/B Testing** | Route by header/cookie | Feature testing |

## Interview Questions

**Q: How does Kubernetes handle service discovery?**
A: CoreDNS resolves service names to ClusterIP. `http://order-service.default.svc.cluster.local:8080`

**Q: How do you handle secrets in K8s?**
A: K8s Secrets (base64, not encrypted by default). Better: AWS Secrets Manager + External Secrets Operator, or HashiCorp Vault.

**Q: How does HPA work?**
A: Metrics Server collects CPU/memory → HPA controller compares against target → scales replicas up/down. Custom metrics via Prometheus Adapter.

**Q: Pod vs Container?**
A: Pod = group of containers sharing network/storage. Sidecar pattern: main app + logging agent in same pod.
