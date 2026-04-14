# 🚀 Deployment Guide

## Local Development

```bash
# 1. Start infrastructure
docker-compose up -d postgres kafka redis zipkin prometheus grafana kafka-ui

# 2. Build all services
./mvnw clean package -DskipTests

# 3. Start all services
docker-compose up -d

# 4. Verify health
curl http://localhost:8080/actuator/health  # API Gateway
curl http://localhost:8081/actuator/health  # Product Service
curl http://localhost:8082/actuator/health  # Order Service
```

## AWS EKS Deployment

### Prerequisites
- AWS CLI configured
- kubectl installed
- Terraform installed

### Step 1: Provision Infrastructure
```bash
cd terraform
terraform init
terraform plan -var="db_password=<your-password>"
terraform apply -var="db_password=<your-password>"
```

### Step 2: Configure kubectl
```bash
aws eks update-kubeconfig --name ecommerce-cluster --region ap-southeast-2
```

### Step 3: Deploy Services
```bash
# Create namespace
kubectl create namespace ecommerce

# Apply configs and secrets
kubectl apply -f kubernetes/configmaps/ -n ecommerce
kubectl apply -f kubernetes/secrets/ -n ecommerce

# Deploy services
kubectl apply -f kubernetes/deployments/ -n ecommerce

# Verify
kubectl get pods -n ecommerce
```

### Step 4: Verify
```bash
kubectl get svc -n ecommerce
kubectl logs -f deployment/order-service -n ecommerce
```

## Monitoring

- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9091
- **Zipkin**: http://localhost:9411
- **Kafka UI**: http://localhost:9090
