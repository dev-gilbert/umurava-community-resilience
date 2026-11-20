#!/usr/bin/env bash
set -euo pipefail
: "${BACKUP_FILE:?Set BACKUP_FILE to downloaded .sql.gz.age}"
: "${AGE_IDENTITY_FILE:?Set AGE_IDENTITY_FILE to age private identity file}"
: "${RESTORE_TARGET:?Set RESTORE_TARGET to an isolated PostgreSQL service/container name}"
: "${BACKUP_CHECKSUM_FILE:?Set BACKUP_CHECKSUM_FILE to the downloaded .sha256 sidecar}"
command -v age >/dev/null
command -v gzip >/dev/null
command -v psql >/dev/null
[[ -f "$BACKUP_FILE" && -f "$AGE_IDENTITY_FILE" && -f "$BACKUP_CHECKSUM_FILE" ]] || { echo "Backup, checksum or identity file missing" >&2; exit 1; }
EXPECTED="$(awk 'NR==1 {print $1}' "$BACKUP_CHECKSUM_FILE")"
ACTUAL="$(sha256sum "$BACKUP_FILE" | awk '{print $1}')"
[[ "$EXPECTED" == "$ACTUAL" ]] || { echo "Encrypted backup checksum mismatch" >&2; exit 1; }
TMP="$(mktemp)"
trap 'rm -f "$TMP"' EXIT
age -d -i "$AGE_IDENTITY_FILE" "$BACKUP_FILE" | gzip -t
age -d -i "$AGE_IDENTITY_FILE" "$BACKUP_FILE" | gzip -dc > "$TMP"
psql "$RESTORE_TARGET" -v ON_ERROR_STOP=1 -f "$TMP"
psql "$RESTORE_TARGET" -v ON_ERROR_STOP=1 -Atc "SELECT COUNT(*) FROM communities" | grep -Eq '^[0-9]+$'
echo "Restore command completed. Operator must compare counts/checksums, run API smoke tests, record RTO/RPO and sign the DR exercise report."
