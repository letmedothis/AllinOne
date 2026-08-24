# Data Integrity Repairs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate stale Luckysheet persistence, unauthorized state transitions, concurrent Sheet deletion, and single-sheet write-back behavior without running the memory-intensive application or build.

**Architecture:** Persist Luckysheet through its public serialization API, keep paged cell state synchronized through public mutation APIs, and separate ordinary edits from explicit state transitions. Make Sheet deletion explicit and carry the sheet index through field mappings so write-back keys remain unambiguous.

**Tech Stack:** Vue 3, TypeScript, Luckysheet, Spring Boot 3, Java 17, MyBatis, JUnit 5, Mockito.

---

### Task 1: Luckysheet persistence and paged report editing

**Files:**
- Modify: `allinone-typescript/src/components/CollectSheet/index.vue`
- Modify: `allinone-typescript/src/views/collect/report/editor.vue`
- Modify: `allinone-typescript/src/api/collect/report.ts`

- [ ] Replace raw `getLuckysheetfile()` persistence with `getAllSheets()`.
- [ ] Load server cells through `setCellValue` so Luckysheet `data` and rendering stay synchronized.
- [ ] Remove a range marker when its request fails, allowing retry.
- [ ] Derive dirty cells from `getAllSheets()` and send batches of at most 5000 cells.
- [ ] Add a visible-range loader hook so ranges beyond the initial 100 by 30 window can be requested.
- [ ] Statically inspect all Luckysheet API names against `allinone-luckysheet/src/global/api.js`.

### Task 2: State-transition isolation

**Files:**
- Modify: `allinone-collect/src/test/java/com/allinone/collect/service/impl/CollectDataServiceImplTest.java`
- Create: `allinone-collect/src/test/java/com/allinone/collect/service/impl/CollectTemplateServiceImplTest.java`
- Modify: `allinone-collect/src/main/java/com/allinone/collect/service/impl/CollectDataServiceImpl.java`
- Modify: `allinone-collect/src/main/resources/mapper/collect/CollectDataMapper.xml`
- Modify: `allinone-collect/src/main/java/com/allinone/collect/service/impl/CollectTemplateServiceImpl.java`
- Modify: `allinone-collect/src/main/resources/mapper/collect/CollectTemplateMapper.xml`

- [ ] Add regression assertions that ordinary edit input cannot change `bizStatus` or template `status`.
- [ ] Force new fill data to use the current user's department and clear client-supplied submit metadata.
- [ ] Remove `biz_status` and `status` assignments from ordinary update SQL.
- [ ] Force new templates to draft status and restrict status transitions to the publish endpoint.
- [ ] Require `del_flag = '0'` in the dedicated template status update.
- [ ] Statically parse both changed mapper XML files.

### Task 3: Explicit Sheet deletion and multi-sheet write-back

**Files:**
- Modify: `allinone-typescript/src/views/collect/report/editor.vue`
- Modify: `allinone-collect/src/main/java/com/allinone/collect/controller/WorkReportController.java`
- Modify: `allinone-collect/src/main/java/com/allinone/collect/domain/CollectFieldMapping.java`
- Modify: `allinone-collect/src/main/java/com/allinone/collect/service/impl/DataWriteBackServiceImpl.java`
- Modify: `allinone-collect/src/main/resources/mapper/collect/CollectFieldMappingMapper.xml`
- Modify: `sql/allinone_biz.sql`
- Modify: `sql/allinone_biz_update.sql`

- [ ] Send explicit deleted Sheet IDs rather than inferring deletion from omission.
- [ ] Validate every requested deletion against the current report and access scope.
- [ ] Add `sheet_index` to field mappings and use `sheetIndex,row,col` as the write-back lookup key.
- [ ] Parse scalar and object-form Luckysheet cell values from every Sheet.
- [ ] Preserve compatibility by defaulting existing mappings to sheet index zero.

### Task 4: Static verification

**Files:**
- Review: all files changed by Tasks 1-3

- [ ] Run `git diff --check` and require no whitespace errors.
- [ ] Parse changed XML files as UTF-8 XML.
- [ ] Search for remaining raw persistence calls and state assignments in ordinary update SQL.
- [ ] Review the final diff for accidental changes to `.claude/settings.local.json` or unrelated user files.
- [ ] Report that build, tests, application runtime, and SQL execution were not performed because of the explicit memory constraint.

