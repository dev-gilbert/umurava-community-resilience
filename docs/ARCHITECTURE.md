# UMURAVA architecture

## Three tiers
1. Central cloud for district/national coordination, central data, analytics and backups.
2. Community edge server for local operations during internet outages.
3. Flutter Android and web clients; mobile local persistence uses SQLite.

## Offline constraints
Local writes must be durable before the client reports success. A disconnected phone cannot exchange data without a shared transport such as LAN or Bluetooth. Sprint 01 provides a local database and outbox schema, not a complete synchronization engine. Bidirectional sync, idempotency, conflict handling and device provisioning are Sprint 03.

## Backend
Start with a modular monolith. PostgreSQL is the source of truth for each deployment tier. Flyway manages schema migrations. Inventory must use an immutable transaction ledger and atomic updates; do not overwrite stock totals from clients.

## Security
The Sprint 01 API denies non-public routes by default. Sprint 02 implements identity and scoped authorization. Do not deploy with public access to community or resource data.
