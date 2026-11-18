# UMURAVA architecture

## Three tiers
1. Central cloud for district/national coordination, central data, analytics and backups.
2. Community edge server for local operations during internet outages.
3. Flutter Android and web clients; mobile local persistence uses SQLite.

## Offline constraints
Local writes must be durable before the client reports success. A disconnected phone cannot exchange data without a shared transport such as LAN or Bluetooth. Sprint 01 provides the local database foundation. Sprint 03 adds a durable mobile outbox, registered devices, bounded event push, ordered pull cursors, idempotency checks and event acknowledgements. It does not yet provide a complete peer-to-peer transport or module-specific event application.

## Backend
Start with a modular monolith. PostgreSQL is the source of truth for each deployment tier. Flyway manages schema migrations. Inventory must use an immutable transaction ledger and atomic updates; do not overwrite stock totals from clients.

## Identity and authorization
- Passwords are hashed with BCrypt.
- Access tokens are short-lived JWTs; refresh tokens are random opaque values stored as SHA-256 hashes.
- Refresh tokens rotate on use. A reused/expired token revokes active sessions in its token family.
- Community membership is scoped by community ID; roles in one community do not confer access to another.
- Public registration creates an account only, not a community membership or administrator role.
- Initial community administrator provisioning requires a controlled operator process.
- Sync devices are bound to an authenticated user and a community membership.
- Unknown API routes remain denied by default.

## Synchronization rules
- Persist client events before attempting network delivery.
- Use client UUIDs as immutable idempotency keys.
- Use server-assigned monotonic sequence numbers for pull pagination; do not order solely by device clocks.
- A duplicate event ID with different content is a conflict, not a silent overwrite.
- Store acknowledgements per receiving device.
- A sync event being accepted does not mean its business operation has been applied. Domain-specific validation and transaction handlers must own that step.

## Release limitations
Sprint 02 implements the initial identity and community-membership authorization foundation. Sprint 03 implements the first sync transport contract, not full offline operation. Rate limiting, MFA, device lifecycle management, privileged bootstrap tooling, external secret management, security monitoring, peer-to-peer transport and independent security testing remain outstanding. Do not expose the development stack publicly or enter real patient/sensitive personal data.
