#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="$ROOT/deploy/edge/.env.edge"
COMPOSE_FILE="$ROOT/deploy/edge/compose.yml"
if [[ "${CONFIRM_RESTORE:-}" != "YES" ]]; then echo "Refusing destructive restore. Re-run with CONFIRM_RESTORE=YES BACKUP_FILE=/absolute/path/file.sql.gz" >&2; exit 1; fi
if [[ -z "${BACKUP_FILE:-}" || ! -f "$BACKUP_FILE" ]]; then echo "Set BACKUP_FILE to an existing .sql.gz backup." >&2; exit 1; fi
if [[ ! -f "$ENV_FILE" ]]; then echo "Missing $ENV_FILE" >&2; exit 1; fi
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T edge-postgres sh -c 'psql -U "$POSTGRES_USER" -d postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$POSTGRES_DB' AND pid <> pg_backend_pid();"; dropdb -U "$POSTGRES_USER" --if-exists "$POSTGRES_DB"; createdb -U "$POSTGRES_USER" "$POSTGRES_DB"'
gzip -dc "$BACKUP_FILE" | docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T edge-postgres sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" "$POSTGRES_DB"'
echo "Restore completed from $BACKUP_FILE"
