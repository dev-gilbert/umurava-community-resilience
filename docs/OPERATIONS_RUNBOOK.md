# Operations runbook (development/staging)

## Start the community edge node
1. Copy `deploy/edge/.env.edge.example` to `deploy/edge/.env.edge` and replace every placeholder secret.
2. Start with `docker compose --env-file deploy/edge/.env.edge -f deploy/edge/compose.yml up -d --build`.
3. Verify with `bash deploy/edge/smoke-test.sh`.
4. Enable monitoring with `--profile monitoring` only on a trusted management network.

## Backup and restore
- Create: `bash deploy/edge/backup.sh`.
- Copy encrypted backups off the edge host. Local-only backups do not protect against host theft, fire or disk loss.
- Restore is destructive and requires `CONFIRM_RESTORE=YES BACKUP_FILE=/absolute/path/file.sql.gz bash deploy/edge/restore.sh`.
- Run restore drills against a disposable staging database before production use.

## Incident response
- Preserve logs and timestamps; avoid copying personal or patient information into tickets or chat.
- If a device is lost, disable/revoke its registration through the operational process before re-issuing credentials. Device revocation automation remains incomplete.
- If cloud connectivity is lost, use only the deployed local edge/LAN service. Do not assume disconnected phones can exchange data directly.
- If data appears inconsistent, stop manual reconciliation, preserve both event histories and escalate to the technical operator. Do not overwrite local databases.
- For suspected compromise, isolate the affected node, preserve evidence and rotate credentials after containment.

This runbook is not a substitute for a staffed on-call rotation, tested disaster recovery plan or incident-response agreement.
