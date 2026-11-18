# UMURAVA — Community Resilience Network

Rwanda-first hybrid platform for coordinating essential community resources during disruptions.

## Sprint 01 foundation
- Spring Boot 3.5 / Java 21 API foundation with PostgreSQL and Flyway.
- Next.js 16 / React 19 dashboard shell with English, Kinyarwanda and French labels.
- Flutter Android shell with SQLite local-resource and outbox tables.
- Docker Compose development environment.
- API is deny-by-default: only health and system information are public. Authentication, scoped RBAC and operational endpoints are not yet implemented.

## Run
1. Copy .env.example to .env and replace the local database password.
2. Run docker compose up --build.
3. Web: http://localhost:3000
4. API health: http://localhost:8080/actuator/health
5. API info: http://localhost:8080/api/v1/system/info

Do not expose the development stack publicly. Do not enter real patient or sensitive personal data.

See docs/ARCHITECTURE.md and docs/API.md. This is not production-ready; authentication, sync, all module workflows, monitoring, backups, security review and field validation remain outstanding.
