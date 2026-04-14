#!/bin/bash
set -euo pipefail

TENANT_NAME="${1:?Usage: $0 <tenant-name> [tier]}"
TIER="${2:-small}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "🚀 Onboarding tenant: $TENANT_NAME (tier: $TIER)"

# 1. Generate tenant values file
cat > "$PROJECT_DIR/tenants/${TENANT_NAME}.yaml" <<EOF
tenant:
  name: ${TENANT_NAME}
  tier: ${TIER}

app:
  image: ghcr.io/maplehub/sample-app:latest
  replicas: 2
  port: 8080

hpa:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPU: 70
EOF

echo "✅ Created tenants/${TENANT_NAME}.yaml"

# 2. Install via Helm (if not using ArgoCD auto-sync)
if command -v helm &> /dev/null; then
  helm upgrade --install "tenant-${TENANT_NAME}" \
    "$PROJECT_DIR/helm-charts/tenant-app" \
    -f "$PROJECT_DIR/tenants/${TENANT_NAME}.yaml" \
    --create-namespace \
    --namespace "tenant-${TENANT_NAME}"
  echo "✅ Helm release installed"
fi

# 3. Verify
echo ""
echo "📋 Tenant resources:"
kubectl get all -n "tenant-${TENANT_NAME}" 2>/dev/null || echo "(namespace not yet ready)"
kubectl get resourcequota -n "tenant-${TENANT_NAME}" 2>/dev/null || true
kubectl get networkpolicy -n "tenant-${TENANT_NAME}" 2>/dev/null || true

echo ""
echo "🎉 Tenant ${TENANT_NAME} onboarded successfully!"
echo "   If using ArgoCD, commit tenants/${TENANT_NAME}.yaml and ArgoCD will auto-sync."
