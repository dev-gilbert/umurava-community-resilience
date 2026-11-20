# Sprint 04 — Community geography and resource registry

- Adds Rwanda administrative geography metadata: province, district, sector, cell and village.
- Adds optional community code, description and validated coordinates.
- Adds a role-protected community resource register for ENERGY, WATER, FOOD, HEALTH, TRANSPORT, SHELTER and OTHER.
- Active community members can view inventory; COMMUNITY_ADMIN and RESOURCE_MANAGER can create resources and adjust quantities.
- Quantity adjustments are recorded in the resource transaction ledger, require idempotency keys and cannot produce negative stock.

## API
- GET /api/v1/communities/{communityId}/resources?category=WATER
- POST /api/v1/communities/{communityId}/resources
- POST /api/v1/communities/{communityId}/resources/{resourceId}/adjustments

Not yet included: official boundary import, PostGIS queries, community provisioning, resource distribution/reservations/expiry, full integration tests, offline event application and field validation. This is a development increment, not a production release.
