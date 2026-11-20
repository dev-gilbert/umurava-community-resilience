#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="$ROOT/deploy/edge/.env.edge"
COMPOSE_FILE="$ROOT/deploy/edge/compose.yml"
: "${AGE_RECIPIENT:?Set AGE_RECIPIENT to the age public recipient}"
: "${RCLONE_REMOTE:?Set RCLONE_REMOTE, e.g. s3:umurava-backups/edge01}"
[[ -f "$ENV_FILE" ]] || { echo "Missing $ENV_FILE" >&2; exit 1; }
command -v age >/dev/null || { echo "Install age" >&2; exit 1; }
command -v rclone >/dev/null || { echo "Install rclone and configure an encrypted/secured remote" >&2; exit 1; }
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
TMP="$(mktemp)"
trap 'rm -f "$TMP" "${TMP}.age" "${TMP}.age.sha256"' EXIT
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T edge-postgres sh -c 'pg_dump --no-owner --no-privileges -U "$POSTGRES_USER" "$POSTGRES_DB"' | gzip -9 > "$TMP"
age -r "$AGE_RECIPIENT" -o "${TMP}.age" "$TMP"
sha256sum "${TMP}.age" | awk '{print $1}' > "${TMP}.age.sha256"
rclone copyto "${TMP}.age" "$RCLONE_REMOTE/umurava-edge-$STAMP.sql.gz.age"
rclone copyto "${TMP}.age.sha256" "$RCLONE_REMOTE/umurava-edge-$STAMP.sql.gz.age.sha256"
echo "Encrypted backup uploaded: $RCLONE_REMOTE/umurava-edge-$STAMP.sql.gz.age"
