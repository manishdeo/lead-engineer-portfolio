# ☁️ AWS Architecture — Interview Reference

> Key AWS services and architecture patterns for Lead Engineer interviews

---

## Core Architecture Patterns

### Three-Tier Web Application
```
Route 53 → CloudFront → ALB → ECS/EKS → RDS Aurora
                                  ↕
                            ElastiCache (Redis)
```

### Event-Driven Serverless
```
API Gateway → Lambda → DynamoDB
                ↓
              SQS → Lambda → S3
                ↓
              EventBridge → Step Functions
```

### Microservices on EKS
```
Route 53 → ALB → EKS Ingress → Services (Pods)
                                    ↕
                    RDS | DynamoDB | ElastiCache | MSK (Kafka)
```

---

## Service Selection Guide

| Need | Best Choice | Why |
|------|------------|-----|
| Relational DB (< 64TB) | Aurora PostgreSQL | 5x faster than standard PG, auto-scaling replicas |
| Relational DB (> 64TB) | Aurora + read replicas + sharding | Or consider DynamoDB |
| NoSQL (key-value) | DynamoDB | Single-digit ms, auto-scaling, serverless |
| Cache | ElastiCache Redis | Sub-ms latency, data structures, pub/sub |
| Message Queue | SQS | Managed, serverless, dead-letter queues |
| Event Streaming | MSK (Kafka) or Kinesis | High throughput, replay, multiple consumers |
| Container Orchestration | EKS | Kubernetes managed, Fargate for serverless pods |
| Serverless Compute | Lambda | Event-driven, pay-per-invocation, 0 to scale |
| Object Storage | S3 | 11 9s durability, lifecycle policies, versioning |
| CDN | CloudFront | Global edge, Lambda@Edge for customization |
| DNS | Route 53 | Latency/weighted/failover routing |
| Monitoring | CloudWatch + X-Ray | Metrics, logs, traces, alarms |
| IaC | CDK or Terraform | CDK for AWS-native, Terraform for multi-cloud |
| Secrets | Secrets Manager | Auto-rotation, RDS integration |
| CI/CD | CodePipeline or GitHub Actions | CodePipeline for AWS-native |

---

## Key Concepts for Interviews

### Well-Architected Framework (6 Pillars)
1. **Operational Excellence** — Automate, monitor, improve
2. **Security** — Least privilege, encryption, compliance
3. **Reliability** — Multi-AZ, auto-scaling, backups
4. **Performance Efficiency** — Right-size, caching, CDN
5. **Cost Optimization** — Reserved instances, spot, right-sizing
6. **Sustainability** — Efficient resource usage

### Multi-AZ vs Multi-Region
- **Multi-AZ:** High availability within a region (RDS, ElastiCache)
- **Multi-Region:** Disaster recovery, global users (Aurora Global, DynamoDB Global Tables)

### Auto Scaling Strategies
- **Target tracking:** Maintain CPU at 70%
- **Step scaling:** Add 2 instances when CPU > 80%
- **Scheduled:** Scale up before known peak hours
- **Predictive:** ML-based forecasting (ECS, EC2)

---

## Cost Optimization Tips
- Use **Spot Instances** for stateless workloads (70% savings)
- **Reserved Instances** for steady-state (up to 72% savings)
- **S3 Intelligent-Tiering** for unknown access patterns
- **Lambda** for sporadic workloads (pay per invocation)
- **Right-sizing** — CloudWatch metrics to identify over-provisioned resources
- **NAT Gateway** — Expensive; use VPC endpoints for S3/DynamoDB
