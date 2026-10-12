#!/usr/bin/env bash
# Deploys to the current kubectl context.
#   REGISTRY=registry.example.com:5000 TAG=0.1.0 ./scripts/deploy-k8s.sh
set -euo pipefail
cd "$(dirname "$0")/.."
REGISTRY="${REGISTRY:?set REGISTRY to your image registry}"
TAG="${TAG:-latest}"
WORK=$(mktemp -d)
cp deploy/k8s/*.yaml "$WORK"/
sed -i "s#REGISTRY/fincore#${REGISTRY}/fincore#g; s#newTag: latest#newTag: ${TAG}#g" "$WORK/kustomization.yaml"
NS=fincore
kubectl apply -f "$WORK/00-namespace.yaml"
# Re-running migrations: Jobs are immutable, so remove finished ones first.
kubectl -n "$NS" delete job -l '!no-such-label' --field-selector status.successful=1 --ignore-not-found >/dev/null 2>&1 || true
kubectl apply -k "$WORK"
echo "Waiting for infrastructure..."
kubectl -n "$NS" rollout status statefulset/postgres --timeout=180s
kubectl -n "$NS" rollout status statefulset/kafka --timeout=180s
echo "Waiting for migrations..."
for job in ledger-service pricing-service payments-service accounts-service payouts-service lending-service reconciliation-service; do
  kubectl -n "$NS" wait --for=condition=complete "job/${job}-migrate" --timeout=300s
done
for d in rails-simulator ledger-service pricing-service accounts-service payments-service payouts-service lending-service reconciliation-service api-gateway; do
  kubectl -n "$NS" rollout status "deployment/${d}" --timeout=300s
done
echo "Done. Gateway: kubectl -n $NS port-forward svc/api-gateway 8080:8080"
