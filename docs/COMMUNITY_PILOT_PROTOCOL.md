# UMURAVA community pilot protocol

## Preconditions
- Written community/district authorization and named local pilot lead.
- Privacy/legal review sign-off and plain-language participant notices in Kinyarwanda, English and French.
- No real patient-level clinical records in the pilot.
- Trained operators, support contact, incident escalation, offline fallback and printed operating procedures.
- Defined scope, duration, success metrics, stop conditions and data-retention plan.

## Test scenarios
1. Register a community and role-scoped users; verify one community cannot read another.
2. Record food, water and energy resource updates online and offline.
3. Record health-facility readiness without patient details.
4. Create transport requests and verify dispatch lifecycle.
5. Report incidents and publish/acknowledge community announcements.
6. Disable WAN and cloud access; verify edge operations continue locally.
7. Reconnect and verify idempotent replay, conflict visibility and cursor convergence.
8. Revoke a lost device and verify its next API operation is rejected.
9. Restore a backup into isolated infrastructure and compare record counts/checksums.

## Metrics and stop conditions
- Event delivery/application rate, duplicate rate, conflict count and age, sync lag, recovery time, data loss.
- Task completion time, operator error rate, availability during WAN loss and participant feedback.
- Stop immediately for unauthorized disclosure, cross-community access, data corruption, unsafe operational recommendations, loss of auditability or unresolved security incident.

Pilot report must include sample size, dates, sites, device models, network conditions, incidents, limitations, participant feedback, and signed go/no-go decision.
