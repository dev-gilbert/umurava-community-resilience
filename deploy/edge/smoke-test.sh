#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://127.0.0.1:8081}"
HEALTH="$(curl --fail --silent --show-error "$BASE_URL/actuator/health")"
printf '%s\n' "$HEALTH" | grep -q '"status":"UP"'
echo "API health: UP"
if [[ "${CHECK_METRICS:-false}" == "true" ]]; then
  curl --fail --silent --show-error "$BASE_URL/actuator/prometheus" | grep -q '^#'
  echo "Prometheus metrics endpoint: reachable"
fi
