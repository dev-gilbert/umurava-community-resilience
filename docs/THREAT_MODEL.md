# Initial threat model

## Assets
Identity credentials and sessions; community membership and permissions; resource inventory and allocation records; emergency reports; health-facility availability; device identity; sync queues; audit history; cloud and edge signing keys.

## Trust boundaries
1. Public internet to cloud ingress.
2. Cloud services to PostgreSQL/object storage.
3. Community LAN to edge server.
4. Mobile devices to edge server over an intermittently trusted network.
5. Edge-to-cloud synchronization.
6. Administrator consoles and operational tooling.

## Principal threats and mitigations
- Credential stuffing: adaptive rate limits, generic login errors, password policy, breached-password screening where feasible, MFA for privileged roles.
- Token theft/replay: short access-token lifetime, refresh rotation, secure device storage, TLS, revocation and reuse detection.
- Broken object-level authorization: enforce community/facility scope server-side on every read and write; test cross-tenant access.
- Malicious or compromised edge node: device certificates, signed sync batches, least privilege, tamper-evident audit, remote quarantine and key rotation.
- Replay/duplicate sync: stable event UUID, idempotency key, unique database constraints and transactional processing.
- Inventory race conditions: immutable ledger, row-level locking or serializable transactions, non-negative constraints and reconciliation.
- Data leakage: data minimization, encryption at rest/in transit, no secrets in logs, restricted medical data and retention controls.
- Supply-chain compromise: pinned dependency versions, automated vulnerability scanning, signed release artifacts and review of transitive dependencies.
- Physical theft/power loss: encrypted storage, UPS for edge nodes, tested backups, device revocation and recovery drills.

## Residual risks
A completely disconnected phone cannot exchange data with another device without a shared transport (LAN, Bluetooth or equivalent). Offline authorization cannot receive immediate cloud revocation; use bounded leases and restrict high-risk actions. Radio gateways require separate regulatory, hardware and field validation.
