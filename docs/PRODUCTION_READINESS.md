# UMURAVA production-readiness gates

**Current status: NOT PRODUCTION READY.** Passing CI validates only the checks in the repository workflow. It does not establish security, operational resilience, regulatory compliance or successful field operation.

## Engineering gates
- [ ] Sprint 03: end-to-end offline/online fault injection on multiple Android devices and a real edge node; event application, replay safety, conflict policy and device revocation tests.
- [ ] Sprint 04–07: integration tests for community isolation, stock adjustments, expiry, water test interpretation, health facility updates, vehicle assignment, incident transitions and announcement acknowledgements.
- [ ] Sprint 08: secure service-to-service authentication, historical data governance, temporal backtesting, model comparison, drift monitoring and human approval. The current linear trend is an experimental baseline only.
- [ ] Sprint 09: verified regional grant provisioning, official Rwanda administrative codes, aggregate correctness tests and dashboard UX review.
- [ ] Sprint 10: edge deployment in staging, TLS/firewall/host hardening, monitoring access control, backup encryption, restore drill and measured recovery objectives.
- [ ] Sprint 11: independent penetration test, dependency review, load test, accessibility review, disaster-recovery exercise and signed release checklist.

## Rwanda field and governance gates
- [ ] Validate district/sector/cell/village data against authoritative Rwanda sources and agree the owner of updates.
- [ ] Review data protection, privacy, retention, hosting, health-information boundaries and cross-border data handling with qualified Rwanda counsel and relevant authorities.
- [ ] Agree operational ownership with participating communities, local authorities, health facilities, water/energy operators and transport providers.
- [ ] Pilot in a small number of willing communities with documented consent, support contacts, training and a rollback plan.
- [ ] Test real outages: power loss, ISP loss, intermittent 2G/3G/4G, device battery failure, LAN isolation, edge disk failure and delayed synchronization.
- [ ] Obtain sign-off from community representatives and service owners before wider rollout.

## Release decision
Do not store real patient records, national IDs or sensitive household data in this prototype. Do not use forecasts for automatic rationing or emergency decisions. Do not describe water screening as proof of potability. Do not claim messages are delivered during an outage unless the local edge/LAN path has been deployed and tested.
