---
name: jimureport-integration
description: Modify or troubleshoot AllinOne JimuReport or JimuBI integration, embedded report URLs, report configuration, ticket authentication, engine dependencies, or database initialization.
---

# AllinOne JimuReport integration

JimuReport and JimuBI 2.5.0 are already integrated. Do not follow old instructions that say their Maven dependencies are pending.

## Integration map

- Engine dependencies: `allinone-admin/pom.xml` and `allinone-framework/pom.xml`.
- Component scanning and engine exclusions: `allinone-admin/src/main/java/com/allinone/RuoYiApplication.java`.
- Engine configuration: `allinone-admin/src/main/resources/application.yml` and `application-prod.yml`.
- Security routing: `allinone-framework/src/main/java/com/allinone/framework/config/SecurityConfig.java`.
- Token bridge: `allinone-framework/src/main/java/com/allinone/framework/jimureport/JimuReportTokenService.java`.
- One-time ticket lifecycle: `JimuTicketService.java` and `JimuTicketController.java`.
- Embedded report and dashboard pages: `allinone-typescript/src/views/report/view` and `views/report/dashboard`.
- Application report configuration: `allinone-report` and `allinone-typescript/src/views/report/config`.
- Engine schema: `sql/jimureport.mysql5.7.create.sql`.

## Security invariants

- Never place the long-lived JWT in an iframe URL, query string, redirect target or log.
- The authenticated frontend must request a short-lived ticket from `POST /system/jimu/ticket` and append only that ticket to the engine URL.
- Ticket consumption must remain atomic and one-time. Do not replace Redis get-and-delete with separate get and delete operations.
- Engine paths may be permitted through the main security filter only because `JimuReportTokenService` performs engine authentication. Preserve that boundary.
- Do not log ticket values or credentials. User IDs are sufficient for operational logs.

## Change guidance

- Keep JimuReport and JimuBI starter versions aligned.
- Check both report and dashboard iframe flows when changing URL construction or authentication.
- Keep engine-owned tables separate from application-owned `report_*` and `collect_*` tables.
- Treat sample engine SQL as third-party schema/data. Put application upgrade logic in the appropriate AllinOne SQL script.
- When changing production exposure of the designer, review `application-prod.yml` and proxy rules together.

## Validation

- Run the full Maven reactor for dependency, scanning or security changes.
- Run frontend contract tests and the production build for iframe/config changes.
- In an integrated environment, verify ticket expiry, one-time consumption, report view, dashboard view and access by a user without designer permissions.
