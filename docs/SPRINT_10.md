# Sprint 10 — Community edge deployment and operations

## Edge node
- `deploy/edge/compose.yml` runs an independent PostgreSQL instance and the same UMURAVA API on a community server.
- The API is bound to `127.0.0.1` by default. Change `EDGE_BIND_ADDRESS` only after configuring a trusted LAN, firewall and TLS/reverse proxy.
- Optional Prometheus/Grafana monitoring is enabled with the `monitoring` profile.
- PostgreSQL backup and destructive restore scripts are provided. Backups are local; copy them to encrypted off-site storage and test restores regularly.

## Setup
1. Copy `deploy/edge/.env.edge.example` to `deploy/edge/.env.edge` and set unique strong database, JWT and Grafana secrets.
2. From the repository root run: `docker compose --env-file deploy/edge/.env.edge -f deploy/edge/compose.yml up -d --build`.
3. Enable monitoring with `--profile monitoring`.
4. Check the API at `http://127.0.0.1:8081/actuator/health`.
5. Create a backup with `bash deploy/edge/backup.sh`.
6. Restore only after verifying a backup: `CONFIRM_RESTORE=YES BACKUP_FILE=/absolute/path/backup.sql.gz bash deploy/edge/restore.sh`.

## Critical limitations
This creates an independent edge deployment, not automatic cloud-edge replication. The current sync API stores and pages events, but a complete edge-to-cloud relay, conflict reconciliation, device peer discovery and tested failover are not yet implemented. Do not assume that data automatically reaches the cloud. Before LAN exposure, configure TLS, firewall rules, secret rotation, host hardening and encrypted off-site backups. Prometheus/Grafana are local operational tools and need access control before remote exposure.
