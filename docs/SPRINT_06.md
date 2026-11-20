# Sprint 06 — Health facility readiness and transport coordination

- Adds non-patient health facility readiness records: service status, available beds and oxygen availability.
- Adds community vehicle registry with capacity and availability status.
- Adds transport coordination requests with pickup/destination coordinates, priority, lifecycle status and vehicle assignment.
- Active community members may submit transport requests; transport coordinators/admins manage assignment and status.
- Health facility records are writable by HEALTH_WORKER or community administrator.

Privacy boundary: this sprint does not create patient records or collect passenger names, diagnoses or medical identifiers. Transport notes are bounded and callers are instructed not to include such information.

Not included: dispatch routing/ETA, emergency-service integration, patient records, clinical workflows, vehicle GPS telemetry, integration tests and field validation. This is a development increment, not a production release.
