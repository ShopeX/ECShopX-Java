---
okf_version: "0.2"
---

# Files

- [ECShopX-Java Wiki Quickstart](quickstart.md) - Entry point of the ECShopX-Java wiki — a one-paragraph system summary plus a task-to-page routing table that sends coding agents to the right page for module architecture, API endpoints, domain logic, dispatch events/jobs and cron handlers, schema migrations, payment flows, third-party integrations, deployment/configuration, and testing.
- [Testing Conventions and Verification](testing.md) - How ECShopX-Java verifies changes — per-Bundle plain JUnit 5 unit tests with mocked persistence, the DispatchFlow / PublishProbe / BusProbe / Delegation / WebSlice / ScopeTest / CompanyScopeTest / DistributorScopeTest / DistributorNameScopeTest / ScopeGuardTest / OwnershipTest / IdempotencyTest / ReplayGuardTest / TableContractTest naming taxonomy that pins dispatch publishing, AFTER_COMMIT timing, tenant/ownership/distributor-name scoping, idempotency and replay guards, CSV formula-injection defense, Excel row caps, and entity table metadata, the narrowest mvnw commands per change type, and the suite's coverage boundaries.

# Directories

- [architecture](architecture/)
- [concepts](concepts/)
- [domains](domains/)
- [integrations](integrations/)
- [operations](operations/)
- [workflows](workflows/)
