#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="$ROOT/deploy/edge/.env.edge"
COMPOSE_FILE="$ROOT/deploy/edge/compose.yml"
if [[ ! -f "$ENV_FILE" ]]; then echo "Missing $ENV_FILE (copy .env.edge.example and set strong secrets)." >&2; exit 1; fi
mkdir -p "$ROOT/backups/edge"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
OUT="$ROOT/backups/edge/umurava-edge-$STAMP.sql.gz"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T edge-postgres sh -c 'pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB"' | gzip -9 > "$OUT"
find "$ROOT/backups/edge" -type f -name 'umurava-edge-*.sql.gz' -mtime +14 -delete
echo "Backup written: $OUT"
