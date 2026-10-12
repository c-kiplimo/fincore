#!/usr/bin/env bash
# Builds every service image and (optionally) pushes it to your registry.
#   ./scripts/build-images.sh                       # local images fincore/<svc>:latest
#   REGISTRY=registry.example.com:5000 TAG=0.1.0 PUSH=1 ./scripts/build-images.sh
set -euo pipefail
cd "$(dirname "$0")/.."
REGISTRY="${REGISTRY:-}"
TAG="${TAG:-latest}"
SERVICES=(ledger-service pricing-service payments-service accounts-service payouts-service lending-service reconciliation-service rails-simulator api-gateway)
for svc in "${SERVICES[@]}"; do
  image="fincore/${svc}:${TAG}"
  [[ -n "$REGISTRY" ]] && image="${REGISTRY}/${image}"
  echo ">> building ${image}"
  docker build -f "${svc}/Dockerfile" -t "${image}" .
  if [[ "${PUSH:-0}" == "1" ]]; then docker push "${image}"; fi
done
