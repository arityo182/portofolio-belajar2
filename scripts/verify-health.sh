#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.yml}"
TIMEOUT="${HEALTH_TIMEOUT_SECONDS:-300}"
INTERVAL="${HEALTH_INTERVAL_SECONDS:-5}"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is required" >&2
  exit 1
fi

if ! docker compose -f "${COMPOSE_FILE}" ps --status running --quiet | grep -q .; then
  echo "No running compose services found. Start the stack with: docker compose up -d" >&2
  exit 1
fi

checks=(
  "nginx|https://localhost/healthz"
  "api-gateway|http://localhost:8080/actuator/health/liveness"
  "auth-service|http://localhost:8081/actuator/health/liveness"
  "loan-service|http://localhost:8082/actuator/health/liveness"
  "scoring-service|http://localhost:8083/health"
  "payment-service|http://localhost:8084/actuator/health/liveness"
  "notification-service|http://localhost:8085/actuator/health/liveness"
  "reporting-service|http://localhost:8086/actuator/health/liveness"
  "frontend|http://localhost:3000/login"
  "prometheus|http://localhost:9090/-/healthy"
  "grafana|http://localhost:3001/api/health"
)

deadline=$((SECONDS + TIMEOUT))
pending=("${checks[@]}")

while ((${#pending[@]} > 0)); do
  remaining=()
  for check in "${pending[@]}"; do
    name="${check%%|*}"
    url="${check#*|}"
    if curl -fsSk --max-time 5 -o /dev/null "${url}"; then
      printf "OK   %-22s %s\n" "${name}" "${url}"
    else
      remaining+=("${check}")
    fi
  done
  pending=("${remaining[@]}")
  if ((${#pending[@]} == 0)); then
    break
  fi
  if ((SECONDS >= deadline)); then
    echo "Health check timed out for:" >&2
    printf "  - %s\n" "${pending[@]}" >&2
    exit 1
  fi
  sleep "${INTERVAL}"
done

echo "All services are healthy."
