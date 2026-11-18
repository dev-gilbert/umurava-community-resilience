# UMURAVA — Community Resilience Network

Rwanda-first hybrid platform for coordinating essential community resources during disruptions.

## Implemented foundation
- Spring Boot 3.5 / Java 21 API foundation with PostgreSQL and Flyway.
- Next.js 16 / React 19 dashboard shell with English, Kinyarwanda and French labels.
- Flutter Android shell with SQLite local-resource and outbox tables.
- Docker Compose development environment.
- JWT authentication, BCrypt password hashing and opaque rotating refresh tokens.
- Community-scoped memberships and role checks for membership administration.
- Deny-by-default API security.

## Current development status
Sprint 01 and Sprint 02 CI workflows have passed on their latest baseline commits. Sprint 02 is still undergoing security hardening and verification. Offline synchronization, operational resource workflows, the seven product modules, AI, edge-server deployment and field validation are not yet complete.

## Run
1. Copy `.env.example` to `.env` and replace the local database password and JWT secret.
2. Run `docker compose up --build`.
3. Web: http://localhost:3000
4. API health: http://localhost:8080/actuator/health
5. API info: http://localhost:8080/api/v1/system/info

Do not expose the development stack publicly. Do not enter real patient or sensitive personal data. See `docs/ARCHITECTURE.md` and `docs/API.md`. This project is not production-ready; additional security controls, sync, module workflows, monitoring, backups, security review and field validation remain outstanding.
