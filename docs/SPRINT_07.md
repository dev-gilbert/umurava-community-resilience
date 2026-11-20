# Sprint 07 — Emergency reporting and community communications

- Enables active community members to report geolocated incidents with severity and a controlled status lifecycle.
- Records status transitions in an incident update history.
- Adds community announcements with priority, start/expiry times and per-member acknowledgements.
- Restricts incident status management and announcement publishing to COMMUNICATIONS_OFFICER or COMMUNITY_ADMIN.

Privacy boundary: do not include patient names, diagnoses, national IDs or other sensitive personal data in incident descriptions or public announcements. No SMS, USSD, radio, push-notification or emergency-services integration is implemented in this increment. Local delivery during internet outages still depends on the edge/LAN sync transport being deployed.

Not included: verified emergency escalation, moderation workflow, push/SMS gateway, edge message broker, peer-to-peer transport, integration tests and field validation. This is not a production emergency dispatch service.
