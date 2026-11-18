# Sprint 02 — Identity and Security Baseline

## Scope
This increment establishes the security contract before community and resource workflows are exposed. It is not a declaration of production readiness.

## Required controls
- Passwords are stored using BCrypt (work factor 12 or higher, tuned on deployment hardware).
- Access tokens are short-lived (recommended 10 minutes) and signed with a secret supplied by a secret manager/environment, never committed.
- Refresh tokens are opaque, high-entropy values. Store only a cryptographic hash; rotate on every refresh; revoke the token family on reuse.
- Every protected API denies anonymous access by default. Public endpoints must be explicitly allowlisted.
- Authorization is scoped: a role assignment belongs to a user and a community/organizational scope. A global role must be an explicit, audited exception.
- Audit authentication success/failure, refresh, logout, role changes and privileged actions without logging passwords, tokens or medical payloads.
- Rate-limit login and refresh endpoints at the edge/reverse proxy and application layer.
- Require TLS in deployed environments. Disable cleartext traffic outside local development.
- Use least-privilege database credentials, encrypted backups, key rotation and device/session revocation.

## Authorization model
Suggested roles: PLATFORM_ADMIN, DISTRICT_ADMIN, SECTOR_COORDINATOR, COMMUNITY_ADMIN, RESOURCE_MANAGER, HEALTH_WORKER, TRANSPORT_COORDINATOR, RESPONDER, CITIZEN, AUDITOR.

Role grants are scoped to a community, facility or geography. Permissions are action-oriented (for example `resource.inventory.read`, `resource.inventory.adjust`, `emergency.assign`). A user may hold different roles in different communities. Do not authorize by trusting a role name or community ID supplied by the client.

## Sensitive health information
Keep the first release to facility availability, aggregate stock availability and emergency referral metadata. Do not put patient records or diagnoses in general-purpose announcements, analytics, logs or sync payloads. Add a separate privacy review before any patient-level workflow.

## Offline authentication
Offline access is only for a previously provisioned device with a locally cached, revocable credential and a bounded offline authorization lease. A new device cannot enroll while disconnected. High-risk operations (role changes, sensitive medical access, inventory adjustments above configured thresholds) require online authorization or a separately approved local quorum policy. Local edge audit events must be append-only and reconciled to cloud.

## Acceptance tests
1. Unauthenticated requests to every non-public endpoint return 401.
2. A valid token cannot access another community's records (403 or non-disclosing 404).
3. Expired, malformed, wrong-issuer and wrong-audience tokens are rejected.
4. Refresh token rotation invalidates the previous token; reuse revokes the family.
5. Logout revokes the active refresh session.
6. Passwords and tokens never appear in logs or API responses.
7. Privileged actions create audit records.
8. CORS allowlist is environment-configured; wildcard origins are rejected with credentials.
9. A revoked/lost device cannot continue syncing after its authorization lease expires.

## Release gate
Do not expose this API publicly until automated tests cover the controls above, secrets are provisioned, HTTPS is enabled, and an independent security review is completed. Rwanda data-protection and sector-specific obligations require local legal/compliance review; this document is not a legal compliance certification.
