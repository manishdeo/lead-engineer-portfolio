# ☸️ Kubernetes Multi-tenant Platform

> Production-grade multi-tenant Kubernetes platform with namespace isolation, Istio service mesh, ArgoCD GitOps, and full observability stack.

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)]()
[![Kubernetes](https://img.shields.io/badge/K8s-1.29-blue)]()
[![Istio](https://img.shields.io/badge/Istio-1.21-purple)]()
[![License](https://img.shields.io/badge/license-MIT-blue)]()

---

## 🎯 Overview

Platform engineering project that provisions isolated tenant environments on Kubernetes with automated onboarding, service mesh traffic management, GitOps-driven deployments, and SLO-based observability. Demonstrates how a Lead Engineer designs internal developer platforms.

**Key Interview Differentiators:**
- Namespace-per-tenant isolation with NetworkPolicy + ResourceQuota + LimitRange
- Istio service mesh for mTLS, traffic splitting, fault injection
- ArgoCD ApplicationSet for GitOps tenant provisioning
- OPA Gatekeeper for policy enforcement
- Prometheus + Grafana SLO dashboards with multi-tenant metrics

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        AWS EKS Cluster                          │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │  tenant-alpha │  │  tenant-beta │  │ tenant-gamma │  ...    │
│  │  (namespace)  │  │  (namespace) │  │  (namespace) │         │
│  │  ┌─────────┐ │  │  ┌─────────┐ │  │  ┌─────────┐ │         │
│  │  │ App Pods │ │  │  │ App Pods │ │  │  │ App Pods │ │         │
│  │  └────┬────┘ │  │  └────┬────┘ │  │  └────┬────┘ │         │
│  │       │      │  │       │      │  │       │      │         │
│  │  NetworkPolicy  │  NetworkPolicy  │  NetworkPolicy │         │
│  │  ResourceQuota  │  ResourceQuota  │  ResourceQuota │         │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘         │
│         └──────────────────┼──────────────────┘                 │
│                            │                                    │
│  ┌─────────────────────────▼─────────────────────────┐         │
│  │              Istio Service Mesh                    │         │
│  │  mTLS │ Traffic Splitting │ Rate Limiting          │         │
│  └─────────────────────────┬─────────────────────────┘         │
│                            │                                    │
│  ┌────────────┐  ┌────────▼───────┐  ┌──────────────┐         │
│  │   ArgoCD   │  │ Istio Gateway  │  │  OPA          │         │
│  │  (GitOps)  │  │ (Ingress)      │  │  Gatekeeper   │         │
│  └────────────┘  └────────────────┘  └──────────────┘         │
│                                                                 │
│  ┌──────────────────────────────────────────────────┐          │
│  │         Observability Stack                       │          │
│  │  Prometheus │ Grafana │ Loki │ Jaeger             │          │
│  └──────────────────────────────────────────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Tech Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Cluster | AWS EKS 1.29 | Managed Kubernetes |
| IaC | Terraform | EKS + VPC + IAM provisioning |
| Service Mesh | Istio 1.21 | mTLS, traffic management, observability |
| GitOps | ArgoCD | Declarative deployments, ApplicationSet |
| Policy | OPA Gatekeeper | Admission control, constraint templates |
| Charts | Helm 3 | Tenant app packaging |
| Monitoring | Prometheus + Grafana | Metrics, SLO dashboards |
| CI/CD | GitHub Actions | Build, scan, deploy |

---

## 🚀 Features

- **Tenant onboarding** — Single Helm values file creates namespace + RBAC + quotas + network policies + Istio sidecar
- **Namespace isolation** — NetworkPolicy denies cross-tenant traffic, ResourceQuota caps CPU/memory/pods
- **Service mesh** — Istio mTLS everywhere, VirtualService for canary/blue-green, fault injection for chaos testing
- **GitOps** — ArgoCD ApplicationSet auto-discovers tenant configs, syncs from Git
- **Policy enforcement** — OPA Gatekeeper blocks privileged containers, enforces labels, image registry whitelist
- **Observability** — Per-tenant Prometheus metrics, Grafana dashboards, Istio telemetry
- **Auto-scaling** — HPA on CPU/memory + Karpenter for node auto-provisioning

---

## 📦 Project Structure

```
06-k8s-platform/
├── terraform/              # EKS cluster provisioning
│   ├── main.tf
│   ├── variables.tf
│   └── outputs.tf
├── helm-charts/
│   ├── platform/           # Platform-level chart (Istio, monitoring, ArgoCD)
│   └── tenant-app/         # Per-tenant application chart
├── argocd/                 # ArgoCD ApplicationSet + App-of-Apps
├── istio/                  # Istio Gateway, VirtualService, PeerAuth
├── policies/               # OPA Gatekeeper constraints
├── monitoring/             # Prometheus rules, Grafana dashboards
├── tenants/                # Tenant-specific values files
├── scripts/                # Onboarding automation
└── .github/workflows/
```

---

## 📊 Multi-tenancy Model

| Isolation Layer | Mechanism | What It Prevents |
|----------------|-----------|-----------------|
| Network | NetworkPolicy | Cross-tenant pod communication |
| Resource | ResourceQuota + LimitRange | Noisy neighbor (CPU/memory hogging) |
| Security | PodSecurityStandard (restricted) | Privileged escalation |
| Traffic | Istio AuthorizationPolicy | Unauthorized service-to-service calls |
| Data | Separate namespaces + RBAC | Unauthorized kubectl access |
| Policy | OPA Gatekeeper | Non-compliant deployments |

---

## 🏃 Getting Started

### Prerequisites
- AWS CLI, Terraform, kubectl, Helm, istioctl

### Provision Cluster
```bash
cd terraform && terraform init && terraform apply
aws eks update-kubeconfig --name maplehub-platform
```

### Install Platform
```bash
helm install platform helm-charts/platform -n platform-system --create-namespace
```

### Onboard a Tenant
```bash
./scripts/onboard-tenant.sh alpha
```

---

## 📖 Interview Talking Points

1. **Namespace-per-tenant vs cluster-per-tenant** — Namespace is cost-efficient, sufficient isolation for most SaaS; cluster-per-tenant for regulated industries
2. **Why Istio over Linkerd?** — Richer traffic management (VirtualService, fault injection), better multi-tenant AuthorizationPolicy, Envoy ecosystem
3. **ArgoCD ApplicationSet** — Auto-discovers tenant configs from Git directory structure, eliminates manual App creation
4. **OPA Gatekeeper vs Kyverno** — Gatekeeper uses Rego (powerful policy language), audit mode for gradual rollout
5. **Resource quotas design** — Tiered quotas (small/medium/large), LimitRange sets defaults so devs don't need to specify
6. **Observability per tenant** — Prometheus relabeling adds tenant label, Grafana dashboard variables filter by tenant

---

## 📄 License

MIT License
