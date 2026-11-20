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
