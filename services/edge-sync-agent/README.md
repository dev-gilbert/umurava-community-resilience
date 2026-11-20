# Edge sync agent

This relay continuously pulls event pages from cloud and edge, pushes each page to the other API, acknowledges the source only after the destination accepts every event, and persists independent cursors in a mode-0600 SQLite state file. Push is idempotent by event UUID. Access tokens are refreshed using rotating refresh tokens and the latest pair is persisted locally.

## Provisioning
1. Create the same community and a dedicated integration user on cloud and edge.
2. Register the same device UUID for that user/community on both APIs. The device must be ACTIVE on both.
3. Grant the integration user the minimum permissions required for push/pull/ack.
4. Set CLOUD_API_URL, CLOUD_ACCESS_TOKEN, CLOUD_REFRESH_TOKEN, EDGE_ACCESS_TOKEN and EDGE_REFRESH_TOKEN in the protected edge environment. EDGE_API_URL defaults to the internal Compose service.
5. Set SYNC_COMMUNITY_ID and SYNC_DEVICE_ID. Protect the state volume and environment file; tokens are credentials.
6. Start with a test community and verify both directions before production.

## Semantics and limits
- This is at-least-once transport with idempotent event IDs, not exactly-once delivery.
- Events remain on the source until destination push succeeds and source acknowledgement succeeds.
- A process restart resumes from persisted cursors; replay after an ack/cursor interruption is safe if destination idempotency is intact.
- A revoked device, expired/revoked refresh family, incompatible community/device registration, or API outage pauses relay and retries; inspect logs and re-provision credentials/device as appropriate.
- The generic event projection does not yet call the seven modules' domain services. Do not treat a successful relay as proof that food, water, energy, health, transport, incident or announcement business state has been reconciled.
- This service is not a peer-to-peer phone transport. Edge LAN access and API TLS/network policy must be configured separately.
