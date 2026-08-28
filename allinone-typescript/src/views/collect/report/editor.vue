<template>
  <div class="report-editor">
    <div class="editor-title-bar">
      <el-button type="primary" size="small" plain :loading="saving" @click="handleSave" style="margin-right:12px;">
        保存
      </el-button>
      <el-button v-if="canManage" type="info" size="small" plain @click="openPermissionDialog" style="margin-right:12px;">
        权限
      </el-button>
      <span class="editor-title">{{ reportName }}</span>
    </div>
    <div id="luckysheet" class="luckysheet-container" v-loading="initLoading"></div>
    <SheetPermissionDialog ref="permDialogRef" />
  </div>
</template>

<script setup lang="ts">
import { getReport, saveSheet, getSheet, loadCells, saveCells } from '@/api/collect/report'
import useUserStore from '@/store/modules/user'
import SheetPermissionDialog from './SheetPermissionDialog.vue'

import 'luckysheet/dist/plugins/css/pluginsCss.css'
import 'luckysheet/dist/plugins/plugins.css'
import 'luckysheet/dist/css/luckysheet.css'

const route = useRoute()
const { proxy } = getCurrentInstance()

const reportId = ref('')
const reportName = ref('')
const saving = ref(false)
const initLoading = ref(true)
const canManage = ref(false)
const permDialogRef = ref<InstanceType<typeof SheetPermissionDialog> | null>(null)
const userStore = useUserStore()

const CELL_PAGE_ROWS = 100
const CELL_PAGE_COLS = 30
const MAX_CELLS_PER_REQUEST = 5000
const loadedRanges = new Map<string, Set<string>>()
const cellSnapshots = new Map<string, Map<string, string>>()
const initialSheetIds = new Set<string>()
let scrollLoadTimer: number | undefined

function makeCellKey(r: number, c: number): string { return `${r},${c}` }

function loadLuckysheet(): Promise<void> {
  return (async () => {
    await import('luckysheet/dist/plugins/js/plugin.js')
    await import('luckysheet')
  })()
}

function getSerializedSheets(): any[] {
  const sheets = luckysheet.getAllSheets?.()
  return Array.isArray(sheets) ? sheets : []
}

function getRawCellValue(cell: any): any {
  return cell?.v && typeof cell.v === 'object' && 'v' in cell.v ? cell.v.v : cell?.v
}

function findIndexAtOffset(boundaries: number[] | undefined, offset: number, fallbackSize: number): number {
  if (!boundaries || boundaries.length === 0) return Math.max(0, Math.floor(offset / fallbackSize))
  let low = 0
  let high = boundaries.length - 1
  while (low < high) {
    const mid = Math.floor((low + high) / 2)
    if (boundaries[mid] < offset) low = mid + 1
    else high = mid
  }
  return low
}

function scheduleVisibleCellLoad(position: { scrollLeft?: number; scrollTop?: number }) {
  if (scrollLoadTimer !== undefined) window.clearTimeout(scrollLoadTimer)
  scrollLoadTimer = window.setTimeout(() => {
    const file = luckysheet.getluckysheetfile?.() || []
    const activeSheet = file.find((sheet: any) => sheet.status === '1')
    if (!activeSheet?._sheetDbId) return
    const row = findIndexAtOffset(activeSheet.visibledatarow, Number(position.scrollTop || 0), 20)
    const col = findIndexAtOffset(activeSheet.visibledatacolumn, Number(position.scrollLeft || 0), 73)
    const startRow = Math.floor(row / CELL_PAGE_ROWS) * CELL_PAGE_ROWS
    const startCol = Math.floor(col / CELL_PAGE_COLS) * CELL_PAGE_COLS
    void loadVisibleCellsAndSnapshot(
      activeSheet._sheetDbId,
      startRow,
      startRow + CELL_PAGE_ROWS - 1,
      startCol,
      startCol + CELL_PAGE_COLS - 1
    )
  }, 120)
}

async function initEditor() {
  initLoading.value = true
  try {
    reportId.value = route.params.id as string
    const res = await getReport(reportId.value)
    if (!res.data) {
      proxy?.$modal.msgError('未获取到报表数据')
      return
    }
    reportName.value = res.data.reportName
    canManage.value = Number(userStore.id) === Number(res.data.userId)

    const sheetRes = await getSheet(reportId.value)
    const data = sheetRes.data && sheetRes.data.length > 0 ? sheetRes.data : []
    initialSheetIds.clear()
    for (const sheet of data) {
      if (sheet._sheetDbId) initialSheetIds.add(String(sheet._sheetDbId))
    }

    if (typeof (window as any).luckysheet === 'undefined') {
      await loadLuckysheet()
    }

    luckysheet.create({
      container: 'luckysheet',
      data,
      title: reportName.value,
      lang: 'zh',
      allowUpdate: false,
      showtoolbar: true,
      showinfobar: false,
      showsheetbar: true,
      loadRowNum: 100,
      loadCellNum: 30,
      pageSize: 200,
      hook: {
        workbookCreated: async () => {
          const file = luckysheet.getluckysheetfile()
          for (const sheet of file) {
            if (!sheet._sheetDbId) continue
            await loadVisibleCellsAndSnapshot(sheet._sheetDbId, 0, CELL_PAGE_ROWS - 1, 0, CELL_PAGE_COLS - 1)
          }
          initLoading.value = false
        },
        scroll: (position: { scrollLeft?: number; scrollTop?: number }) => scheduleVisibleCellLoad(position)
      }
    })
  } finally {
    initLoading.value = false
  }
}

async function loadVisibleCellsAndSnapshot(
  sheetDbId: string, startRow: number, endRow: number, startCol: number, endCol: number
) {
  const rangeKey = `${startRow}-${endRow}-${startCol}-${endCol}`
  const rangeSet = loadedRanges.get(sheetDbId) || new Set<string>()
  if (rangeSet.has(rangeKey)) return
  rangeSet.add(rangeKey)
  loadedRanges.set(sheetDbId, rangeSet)

  try {
    const res = await loadCells(sheetDbId, startRow, endRow, startCol, endCol)
    if (!res.data || res.data.length === 0) return
    const file = luckysheet.getluckysheetfile()
    const sheetIndex = file.findIndex((sheet: any) => sheet._sheetDbId === sheetDbId)
    if (sheetIndex < 0) {
      rangeSet.delete(rangeKey)
      return
    }

    for (let index = 0; index < res.data.length; index++) {
      const cell = res.data[index]
      let value = cell.cellValue
      try { value = cell.cellValue == null ? null : JSON.parse(cell.cellValue) } catch { /* 兼容旧的纯文本数据 */ }
      luckysheet.setCellValue(cell.rowIndex, cell.colIndex, value, {
        order: sheetIndex,
        isRefresh: index === res.data.length - 1
      })
    }

    const serializedSheet = getSerializedSheets().find((sheet: any) => sheet._sheetDbId === sheetDbId)
    const currentCells = new Map<string, any>()
    for (const cell of serializedSheet?.celldata || []) currentCells.set(makeCellKey(cell.r, cell.c), cell)
    const snapMap = cellSnapshots.get(sheetDbId) || new Map<string, string>()
    for (const cell of res.data) {
      const current = currentCells.get(makeCellKey(cell.rowIndex, cell.colIndex))
      snapMap.set(makeCellKey(cell.rowIndex, cell.colIndex), JSON.stringify(current?.v ?? null))
    }
    cellSnapshots.set(sheetDbId, snapMap)
  } catch (e) {
    rangeSet.delete(rangeKey)
    console.warn('load cells failed:', e)
  }
}

function collectDirtyCells(): any[] {
  const dirty: any[] = []
  for (const sheet of getSerializedSheets()) {
    const sheetDbId = sheet._sheetDbId
    if (!sheetDbId) continue
    const snapMap = cellSnapshots.get(sheetDbId) || new Map<string, string>()
    const currMap = new Map<string, any>()
    for (const cell of sheet.celldata || []) currMap.set(makeCellKey(cell.r, cell.c), cell)
    for (const [key, cell] of currMap) {
      const currentValue = JSON.stringify(cell.v)
      if (snapMap.get(key) !== currentValue) {
        const rawValue = getRawCellValue(cell)
        dirty.push({
          sheetDbId,
          rowIndex: cell.r,
          colIndex: cell.c,
          cellValue: cell.v != null ? currentValue : null,
          cellFormula: cell.v?.f || null,
          cellType: typeof rawValue === 'number' ? 'number' : typeof rawValue === 'boolean' ? 'bool' : 'string'
        })
      }
    }
    for (const [key] of snapMap) {
      if (!currMap.has(key)) {
        const [rowIndex, colIndex] = key.split(',').map(Number)
        dirty.push({ sheetDbId, rowIndex, colIndex, cellValue: null, cellFormula: null, cellType: 'string' })
      }
    }
  }
  return dirty
}

async function handleSave() {
  saving.value = true
  try {
    const serializedSheets = getSerializedSheets()
    const currentSheetIds = new Set(
      serializedSheets.map((sheet: any) => sheet._sheetDbId).filter(Boolean).map(String)
    )
    const deletedSheetIds = [...initialSheetIds].filter(id => !currentSheetIds.has(id))
    const metadata = serializedSheets.map((sheet: any) => {
      const persistedMetadata = { ...sheet }
      delete persistedMetadata.celldata
      delete persistedMetadata.data
      return persistedMetadata
    })
    const saveResponse = await saveSheet(reportId.value, metadata, deletedSheetIds)

    const file = luckysheet.getluckysheetfile()
    for (const mapping of saveResponse.data || []) {
      const sheet = file.find((item: any) => String(item.index) === mapping.clientSheetId)
      if (sheet) sheet._sheetDbId = mapping.sheetDbId
    }

    const dirty = collectDirtyCells()
    for (let from = 0; from < dirty.length; from += MAX_CELLS_PER_REQUEST) {
      const batch = dirty.slice(from, from + MAX_CELLS_PER_REQUEST)
      await saveCells(batch)
      for (const cell of batch) {
        const snapMap = cellSnapshots.get(cell.sheetDbId) || new Map<string, string>()
        cellSnapshots.set(cell.sheetDbId, snapMap)
        const key = makeCellKey(cell.rowIndex, cell.colIndex)
        if (cell.cellValue == null) snapMap.delete(key)
        else snapMap.set(key, cell.cellValue)
      }
    }

    initialSheetIds.clear()
    for (const sheet of file) {
      if (sheet._sheetDbId) initialSheetIds.add(String(sheet._sheetDbId))
    }
    proxy.$modal.msgSuccess('保存成功')
  } catch (e) {
    proxy.$modal.msgError('保存失败')
  } finally {
    saving.value = false
  }
}

function openPermissionDialog() {
  const file = luckysheet.getluckysheetfile()
  const activeSheet = file?.find((sheet: any) => sheet.status === '1')
  if (activeSheet?._sheetDbId) {
    permDialogRef.value?.open(activeSheet._sheetDbId, activeSheet.name || reportName.value, canManage.value)
  } else {
    proxy.$modal.msgWarning('请先选择一个Sheet')
  }
}

function onKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') { e.preventDefault(); handleSave() }
}

onMounted(() => { initEditor(); document.addEventListener('keydown', onKeydown) })
onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
  if (scrollLoadTimer !== undefined) window.clearTimeout(scrollLoadTimer)
  try {
    luckysheet.destroy()
    loadedRanges.clear()
    cellSnapshots.clear()
    initialSheetIds.clear()
  } catch (e) {
    console.warn('Luckysheet destroy failed:', e)
  }
})
</script>

<style scoped>
.report-editor {
  position: relative;
  height: calc(100vh - 84px);
  margin: -20px;
  box-sizing: border-box;
}
.editor-title-bar {
  position: absolute;
  top: 20px;
  left: 0;
  width: 100%;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  z-index: 10;
  border-bottom: 1px solid #e8e8e8;
}
.editor-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.luckysheet-container {
  position: absolute;
  top: 40px;
  left: 0;
  width: 100%;
  height: calc(100% - 40px);
}
</style>
