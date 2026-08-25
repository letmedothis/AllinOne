import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const typeSource = readFileSync(new URL('../src/types/api/report/config.ts', import.meta.url), 'utf8')
const configViewSource = readFileSync(new URL('../src/views/report/config/index.vue', import.meta.url), 'utf8')

test('report config frontend uses the backend field contract', () => {
  for (const field of ['reportId', 'reportName', 'reportCode', 'reportType', 'jimuReportId', 'jmbiId']) {
    assert.match(typeSource, new RegExp(`\\b${field}\\?\\s*:`), `missing ${field} in ReportConfig`)
  }

  assert.match(configViewSource, /form\.reportName/)
  assert.match(configViewSource, /form\.reportCode/)
  assert.match(configViewSource, /form\.reportType/)
  assert.match(configViewSource, /form\.(jimuReportId|jmbiId)/)
  assert.doesNotMatch(configViewSource, /form\.(name|code|url|type)\b/)
})
