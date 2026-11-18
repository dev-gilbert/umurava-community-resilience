# UMURAVA API

Base path: `/api/v1`

## Public endpoints
- `GET /system/info` — API identity and version.
- `GET /actuator/health` — health endpoint.
- `POST /auth/register` — create an account.
- `POST /auth/login` — authenticate and issue access/refresh tokens.
- `POST /auth/refresh` — rotate a refresh token.
- `POST /auth/logout` — revoke a refresh token.

## Authenticated endpoints
- `GET /auth/me` — current account profile.
- `GET /me/communities` — active community memberships for the current account.
- `GET /communities/{communityId}/memberships` — list active memberships; community administrator only.
- `POST /communities/{communityId}/memberships` — add a member; community administrator only.
- `DELETE /communities/{communityId}/memberships/{membershipId}` — deactivate a membership; community administrator only.
- `POST /sync/devices` — register a client device for a community.
- `POST /sync/push` — submit a batch of locally queued events (maximum 100).
- `GET /sync/pull?communityId=...&deviceId=...&cursor=0&limit=100` — pull ordered community events.
- `POST /sync/ack` — acknowledge events delivered to a registered device.

Send access tokens using `Authorization: Bearer <accessToken>`. Access tokens are short-lived. Refresh tokens are opaque, stored only as SHA-256 hashes, rotated on use, and grouped into token families. Reuse of a revoked or expired token revokes the remaining active sessions in that family.

## Community roles
- `CITIZEN`
- `COMMUNITY_ADMIN`
- `RESOURCE_MANAGER`
- `HEALTH_WORKER`
- `TRANSPORT_COORDINATOR`
- `COMMUNICATIONS_OFFICER`

Authorization is community-scoped. A membership in one community does not grant access to another. Community administrators can assign non-administrator roles and deactivate memberships. They cannot appoint another community administrator or remove the last administrator. Initial administrator provisioning must be performed through a controlled operator process; public self-registration never grants a community role.

## Offline synchronization contract
- Every client event has a client-generated UUID and is immutable after enqueue.
- Devices must be registered to the authenticated user and selected community.
- Push requests are bounded batches; event IDs are idempotency keys. Reusing an event ID with different content returns a conflict.
- Pull uses a monotonically increasing server sequence cursor, not client clocks.
- Clients persist accepted events and their cursor locally before acknowledging them.
- Event acceptance stores the event; it does not yet apply module-specific business rules or mutate resource inventory. Those domain handlers are added with the relevant module sprints.
- A disconnected phone cannot communicate with another device without a shared transport.

## Security boundary
All routes not explicitly listed above remain denied by default. Resource, health, emergency and module-specific synchronization handlers are not yet implemented. Do not treat this API as production-ready until the remaining security controls, integration tests, deployment hardening and field validation are complete.
