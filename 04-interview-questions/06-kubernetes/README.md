# ☸️ Kubernetes & Docker Interview Questions (120+)

---

### Q1: What happens when you run `kubectl apply -f deployment.yaml`?
**A:** kubectl sends to API Server → etcd stores desired state → Scheduler assigns pods to nodes → kubelet on node pulls image → container runtime starts container → kube-proxy updates network rules.

### Q2: How do you handle zero-downtime deployments in K8s?
**A:** Rolling update (default), readiness probes (don't route until ready), PodDisruptionBudget (min available), preStop hook (graceful shutdown), connection draining.

### Q3: How do you debug a CrashLoopBackOff?
**A:** `kubectl logs <pod> --previous` (see crash logs), `kubectl describe pod` (events), check readiness/liveness probes, verify image exists, check resource limits, review application startup.

### Q4: Explain Kubernetes networking model.
**A:** Every pod gets unique IP, pods can communicate directly (no NAT), services provide stable endpoints. CNI plugins (Calico, Cilium) implement networking. Ingress for external HTTP routing.

### Q5: How do you manage secrets in production K8s?
**A:** K8s Secrets (base64, not encrypted by default) → Enable encryption at rest → Better: External Secrets Operator + AWS Secrets Manager or HashiCorp Vault with CSI driver.
