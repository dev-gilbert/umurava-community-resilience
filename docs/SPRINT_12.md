# Sprint 12 — Offline reliability and recovery

## Implemented in this branch
- V12 records device revocation metadata, processed event timestamps, materialized per-community entity state, version conflicts, and edge peer/cursor metadata.
- Sync processor applies only allow-listed entity types and UPSERT/DELETE operations to a durable generic event projection. It enforces strict sequential entity versions (first version is 1); gaps and stale versions enter a conflict queue. Community Admin or Resource Manager can process, inspect and resolve conflicts.
- Device revocation changes status to REVOKED; push, pull, and acknowledgement already require ACTIVE status, so subsequent operations are denied.
- Conflict decisions are explicit: accept incoming or reject incoming. They are audited by actor/time in conflict metadata.

## Important integration boundary
The projection is a durable synchronization state store, not a substitute for domain command handlers. It does not yet invoke each module's validated business service or enforce module-specific invariants (stock non-negativity, water-quality rules, clinical workflow, vehicle dispatch transitions, incident severity lifecycle). Those adapters must be implemented before using sync to mutate operational module tables. DELETE is a tombstone, not physical deletion.

## Edge-to-cloud
V12 adds peer identity/cursor persistence only. A secure relay still requires provisioned peer credentials, mutual TLS or signed requests, cloud/edge direction-specific cursors, replay protection, retry/dead-letter policy, and reconciliation. Do not expose the relay publicly until that protocol and deployment have been independently reviewed.

## Verification still requiring external resources
- Android airplane-mode, force-stop, storage pressure, clock skew, duplicate/reordered events and reconnect tests on supported physical devices.
- Edge power loss, WAN loss, disk-full, database restart and recovery drills on actual hardware.
- Independent penetration test by a qualified third party.
- Encrypted off-site backup credentials and a successful restore into isolated infrastructure.
- Rwanda legal/privacy assessment by qualified counsel and relevant stakeholders, including lawful basis, controller/processor roles, retention, data-subject rights, cross-border hosting and breach response.
- Community pilot approval, informed participation, safeguarding, training, support, and documented acceptance.

No external field test, legal opinion, penetration test, or disaster-recovery exercise is represented as completed by this code change.

## Edge relay deployment
- Provision a dedicated integration account and register the same community/device UUID on both cloud and edge APIs.
- Copy `deploy/edge/.env.edge.example` to `.env.edge`, protect it, and set cloud/edge rotating refresh tokens, API URL, community UUID and device UUID.
- Start the relay with `docker compose --env-file deploy/edge/.env.edge -f deploy/edge/compose.yml --profile sync up -d`.
- The relay persists cursors and rotated tokens in a protected named volume. Test in a non-production community first; confirm both API registrations and community membership before enabling it.

## Backup and restore
- Install `age` and `rclone`, configure an off-site destination, then run `AGE_RECIPIENT=... RCLONE_REMOTE=... bash deploy/edge/backup-encrypted-offsite.sh`.
- Download both the encrypted archive and its SHA-256 sidecar. Run `BACKUP_FILE=... BACKUP_CHECKSUM_FILE=... AGE_IDENTITY_FILE=... RESTORE_TARGET=... bash deploy/edge/dr-exercise.sh` only against an isolated disposable database.
- Record restore time, record counts, checksums and achieved RPO/RTO. Script completion alone is not a successful DR sign-off.

## Seven-module acceptance suite
Configure GitHub Actions secrets `UMURAVA_E2E_API_URL`, `UMURAVA_E2E_EMAIL`, `UMURAVA_E2E_PASSWORD`, and `UMURAVA_E2E_COMMUNITY_ID`, using a disposable community-admin account and isolated test deployment. Run the manual `UMURAVA module end-to-end acceptance` workflow. It creates clearly named E2E records and must not target a live community.
