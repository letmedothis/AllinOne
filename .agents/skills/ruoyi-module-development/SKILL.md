---
name: ruoyi-module-development
description: Develop or modify AllinOne RuoYi business modules, CRUD endpoints, MyBatis mappings, permissions, menus, or generated code. Use for backend and matching frontend module work in this repository.
---

# AllinOne RuoYi module development

Apply the repository's existing patterns instead of generic RuoYi examples. Inspect a nearby implemented module before adding a new one.

## Project boundaries

- Java 17 and Spring Boot 3.5.x are defined by the root `pom.xml`.
- Business collection code belongs in `allinone-collect`. (The legacy WorkReport implementation was decommissioned on 2026-08-30; history under tag `archive/workreport-20260830`.)
- Report configuration code belongs in `allinone-report`.
- Shared primitives belong in `allinone-common`; security and Redis integration belong in `allinone-framework`.
- HTTP entry points are assembled by `allinone-admin`.
- Vue 3 and TypeScript pages and APIs belong in `allinone-typescript/src`.
- Database definitions and upgrade scripts belong in `sql/`. Keep fresh-install SQL and upgrade SQL consistent.

## Required conventions

- Preserve Controller → Service → Mapper interface/XML layering used by adjacent modules.
- Add explicit permission annotations and matching menu permission strings for protected endpoints.
- Treat snowflake IDs as strings at the browser boundary; do not coerce them through JavaScript `Number`.
- Keep MyBatis `resultMap` properties aligned with real Java properties. Do not map logical-delete columns to nonexistent fields.
- Validate dynamic identifiers such as table and column names separately from JDBC values. Values must remain bound parameters.
- Put multi-table state changes inside a service transaction and check affected-row counts when state transitions require a specific prior state.
- Preserve logical-delete behavior for business rows, but explicitly handle physical child/snapshot cleanup when the lifecycle requires it.
- Do not edit generated output blindly. Compare generated code with current Spring Boot 3, MyBatis and Vue 3 patterns first.

## Before finishing

- Check Mapper interface method names, XML statement IDs, parameter names and result mappings together.
- Check backend DTO/domain fields against frontend TypeScript contracts.
- When SQL changes, check `sql/allinone_biz.sql` and `sql/allinone_biz_update.sql` for the applicable install paths.
- Run focused module tests first, then the full reactor when shared code changed:

```bash
mvn -Dmaven.repo.local=/tmp/allinone-m2 -pl <module> -am test -DskipTests=false
mvn -Dmaven.repo.local=/tmp/allinone-m2 test -DskipTests=false
```

- For frontend changes, run from `allinone-typescript`:

```bash
npm run test:contracts
npm run build:prod
```
