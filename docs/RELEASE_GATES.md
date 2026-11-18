# Release gates

A feature is not production-ready merely because it compiles or appears in a dashboard.

## Required evidence before a public pilot
- Automated unit, integration, API contract and authorization tests.
- Dependency and container vulnerability scans; secret scan; code review.
- Independent penetration test and remediation of critical/high findings.
- Backup restore and disaster-recovery exercise with measured RPO/RTO.
- Edge power-loss, internet-loss, disk-full, clock-skew and reconnect tests.
- Mobile device-loss and offline-lease expiry tests.
- Data protection impact assessment and Rwanda legal/regulatory review.
- Accessibility, Kinyarwanda/English/French translation review and community usability testing.
- Operational runbooks, incident response, monitoring, alert ownership and support plan.
- Signed pilot acceptance from participating communities and responsible authorities.

## Operational rule
Keep the public API deny-by-default until authentication, scoped authorization and test coverage are complete. Do not use sample dashboard numbers as real operational data.
