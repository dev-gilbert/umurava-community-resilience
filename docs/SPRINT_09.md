# Sprint 09 — District and national command dashboards

- Adds read-only district and national aggregate dashboards across community resources, food batches, water sources, energy assets, health facilities, vehicles, open transport requests and active incidents.
- Adds a separate regional_access_grants table. Access is denied unless a user has been explicitly provisioned for the requested district or national scope.
- National scope uses the canonical scope value RW; district scope matches the district name case-insensitively.
- No public API allows users to grant themselves regional access. Initial grants must be provisioned by a trusted operator through controlled database/admin procedures.

## API
- GET /api/v1/command/districts/{districtName}/overview
- GET /api/v1/command/national/overview

This is an aggregate API foundation, not a complete national command centre UI. Data quality, district-name normalization, official administrative codes, cross-district disaster coordination, export/reporting, privacy review and field validation remain outstanding.
