# ☁️ AWS & Cloud Interview Questions (150+)

---

### Q1: How do you design a highly available architecture on AWS?
**A:** Multi-AZ deployment (ALB + ECS/EKS across 2+ AZs), RDS Multi-AZ, ElastiCache Multi-AZ, S3 (11 9s durability), Route 53 health checks with failover routing. For DR: multi-region with Aurora Global Database.

### Q2: ECS vs EKS — when to use which?
**A:**
- **ECS:** AWS-native, simpler, good for AWS-only shops, Fargate for serverless containers
- **EKS:** Kubernetes standard, portable, richer ecosystem, better for multi-cloud or complex orchestration
- **Rule:** If you need K8s features (custom operators, service mesh) → EKS. Otherwise → ECS + Fargate.

### Q3: Lambda vs ECS — when to use serverless?
**A:**
- **Lambda:** Event-driven, sporadic traffic, < 15 min execution, pay-per-invocation
- **ECS:** Long-running, steady traffic, need more control, WebSocket support
- **Lambda gotchas:** Cold starts (100ms-2s), 15 min timeout, 10GB memory limit

### Q4: How do you optimize AWS costs?
**A:** Reserved Instances (72% savings), Spot for stateless (90% savings), right-sizing (CloudWatch metrics), S3 lifecycle policies, NAT Gateway alternatives (VPC endpoints), Graviton instances (20% cheaper).

### Q5: Explain VPC architecture best practices.
**A:** Public subnets (ALB, NAT Gateway), private subnets (app, DB), isolated subnets (no internet). Security groups (stateful, instance-level) + NACLs (stateless, subnet-level). VPC endpoints for AWS services.

### Q6: How does DynamoDB handle scaling?
**A:** Auto-scaling based on consumed capacity. On-demand mode for unpredictable traffic. Partition key determines data distribution. Hot partition = performance issue. Use composite keys for even distribution.
