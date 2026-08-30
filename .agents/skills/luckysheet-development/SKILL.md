---
name: luckysheet-development
description: Modify AllinOne Luckysheet source, its Vue editor integration, workbook persistence, cell snapshots, field mappings, data submission, or the linked frontend build.
---

# AllinOne Luckysheet development

This repository maintains a local Luckysheet 2.1.13 fork. The main frontend consumes it through `file:../allinone-luckysheet`; changes to the fork are not available to the app until Luckysheet is built.

## Integration map

- Fork source and build: `allinone-luckysheet`.
- Main integration component: `allinone-typescript/src/components/CollectSheet/index.vue` (the former multi-user editor `views/collect/report/editor.vue` and the WorkReport backend were decommissioned on 2026-08-30; history under tag `archive/workreport-20260830`).
- Collection edit and submit flows: `allinone-typescript/src/views/collect/data`.
- Workbook persistence and snapshot extraction: `CollectDataServiceImpl` in `allinone-collect`.
- Cell snapshot Mapper: `CollectDataCellMapper.java` and its XML.
- Write-back mapping and typed conversion: `CollectFieldMapping`, `DataWriteBackServiceImpl` and `WriteBackValueConverter`.
- Unified build: `scripts/build-frontend.sh`.

## Data invariants

- `form_data` is the workbook source of truth. Preserve every sheet and its sheet index.
- Submitted `collect_data_cell` rows are a complete snapshot, not an incremental patch. Delete the existing rows for the `data_id` and then batch-write the current snapshot in one transaction.
- An empty workbook snapshot must clear old cell rows.
- Deleting a draft must also clean its physical cell snapshot.
- Field mapping keys include sheet, row and column. Do not collapse multiple sheets into sheet zero.
- For typed write-back, use Luckysheet raw value `v` for numbers and dates. Display value `m` is only suitable for text presentation.
- Keep write-back target tables allowlisted through `allinone.collect.write-back.allowed-tables`; table and column identifiers require strict validation.
- Preserve exact numeric values with `BigDecimal`, and reject null converted primary keys.

## Frontend lifecycle

- Serialize through Luckysheet's public workbook APIs already used by the editor.
- Destroy the instance when the Vue page unmounts and avoid attaching duplicate hooks on remount.
- Do not edit generated `dist` files as source.
- Luckysheet uses legacy jQuery-dependent plugins. After dependency or plugin changes, perform browser interaction regression rather than relying only on a successful bundle.

## Build and validation

Run the unified build from the repository root so the local dependency is built in the correct order:

```bash
./scripts/build-frontend.sh
```

For focused checks:

```bash
mvn -Dmaven.repo.local=/tmp/allinone-m2 -pl allinone-collect -am test -DskipTests=false
cd allinone-typescript
npm run test:contracts
```

For release-facing changes, also run `./scripts/audit-frontend.sh`. Browser smoke coverage should include load, edit, formulas, copy/paste, right-click actions, save, submit and reopen.
