# Sprint 05 — Food, water and energy operations

- Adds food batch/stock records with expiry-aware listing.
- Adds water-source registry and timestamped field screening records (pH, turbidity and E. coli indicator).
- Adds energy asset registry with capacity, output and operational status.
- Reads require active community membership; writes require COMMUNITY_ADMIN or RESOURCE_MANAGER.

## API
- GET/POST /api/v1/communities/{communityId}/food-batches
- GET/POST /api/v1/communities/{communityId}/water-sources
- GET/POST /api/v1/communities/{communityId}/water-sources/{sourceId}/quality-tests
- GET/POST /api/v1/communities/{communityId}/energy-assets

Water screening is not a drinking-water certification. NO_FLAG_DETECTED only means the limited recorded indicators did not cross the application's simple review thresholds; laboratory verification and local public-health protocols remain necessary. No automated allocation, sensor integration, stock distribution or offline event application is included yet.
