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

const route = useRoute()
const { proxy } = getCurrentInstance()

const reportId = ref('')
const reportName = ref('')
const saving = ref(false)
const initLoading = ref(true)
const canManage = ref(false)  // 当前用户是创建�? 可管理权�?
const permDialogRef = ref<InstanceType<typeof SheetPermissionDialog> | null>(null)

const userStore = useUserStore()

const CELL_PAGE_ROWS = 100
const CELL_PAGE_COLS = 30
const loadedRanges = new Map<string, Set<string>>()
const cellSnapshots = new Map<string, Map<string, any>>()

function makeCellKey(r: number, c: number): string { return `${r},${c}` }

async function initEditor() {
  initLoading.value = true
  reportId.value = route.params.id as string
  const res = await getReport(reportId.value)
  reportName.value = res.data.reportName

  // 判断当前用户是否是创建�?  const reportUserId = res.data.userId
  canManage.value = Number(userStore.id) === Number(reportUserId)

  const sheetRes = await getSheet(reportId.value)
  const data = sheetRes.data && sheetRes.data.length > 0 ? sheetRes.data : []

  luckysheet.create({
    container: 'luckysheet',
    data: data,
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
          const sheetDbId = sheet._sheetDbId
          if (!sheetDbId) continue
          await loadVisibleCellsAndSnapshot(sheetDbId, 0, CELL_PAGE_ROWS - 1, 0, CELL_PAGE_COLS - 1)
        }
        initLoading.value = false
      }
    }
  })
}

async function loadVisibleCellsAndSnapshot(
  sheetDbId: string, startRow: number, endRow: number, startCol: number, endCol: number
) {
  const key = `${startRow}-${endRow}-${startCol}-${endCol}`
  const rangeSet = loadedRanges.get(sheetDbId) || new Set()
  if (rangeSet.has(key)) return
  rangeSet.add(key)
  loadedRanges.set(sheetDbId, rangeSet)

  try {
    const res = await loadCells(sheetDbId, startRow, endRow, startCol, endCol)
    if (res.data && res.data.length > 0) {
      const file = luckysheet.getluckysheetfile()
      const sheetIndex = file.findIndex((s: any) => s._sheetDbId === sheetDbId)
      if (sheetIndex < 0) return
      const sheet = file[sheetIndex]
      if (!sheet.celldata) sheet.celldata = []
      const snapMap = cellSnapshots.get(sheetDbId) || new Map()
      for (const cell of res.data) {
        const cellObj = { r: cell.rowIndex, c: cell.colIndex, v: cell.cellValue }
        sheet.celldata.push(cellObj)
        snapMap.set(makeCellKey(cell.rowIndex, cell.colIndex), JSON.stringify(cell.cellValue))
      }
      cellSnapshots.set(sheetDbId, snapMap)
    }
  } catch (e) { console.warn('load cells failed:', e) }
}

function collectDirtyCells(): any[] {
  const dirty: any[] = []
  const file = luckysheet.getluckysheetfile()
  for (const sheet of file) {
    const sheetDbId = sheet._sheetDbId
    if (!sheetDbId) continue
    const snapMap = cellSnapshots.get(sheetDbId) || new Map()
    const currentCelldata: any[] = sheet.celldata || []
    const currMap = new Map<string, any>()
    for (const cell of currentCelldata) currMap.set(makeCellKey(cell.r, cell.c), cell)
    for (const [key, cell] of currMap) {
      const snapValue = snapMap.get(key)
      const currentValue = JSON.stringify(cell.v)
      if (snapValue === undefined || snapValue !== currentValue) {
        dirty.push({ sheetDbId, rowIndex: cell.r, colIndex: cell.c, cellValue: cell.v != null ? String(cell.v) : null, cellFormula: cell.f || null, cellType: typeof cell.v === 'number' ? 'number' : 'string' })
      }
    }
    for (const [key] of snapMap) {
      if (!currMap.has(key)) {
        const [r, c] = key.split(',').map(Number)
        dirty.push({ sheetDbId, rowIndex: r, colIndex: c, cellValue: null, cellFormula: null, cellType: 'string' })
      }
    }
  }
  return dirty
}

async function handleSave() {
  saving.value = true
  try {
    const file = luckysheet.getluckysheetfile()
    await saveSheet(reportId.value, file)
    const dirty = collectDirtyCells()
    if (dirty.length > 0) {
      await saveCells(dirty)
      for (const cell of dirty) {
        const snapMap = cellSnapshots.get(cell.sheetDbId)
        if (snapMap) snapMap.set(makeCellKey(cell.rowIndex, cell.colIndex), JSON.stringify(cell.cellValue))
      }
    }
    proxy.$modal.msgSuccess('保存成功')
  } catch (e) { proxy.$modal.msgError('保存失败') }
  finally { saving.value = false }
}

function openPermissionDialog() {
  // 取当�?sheet（活跃的 sheet�?  const file = luckysheet.getluckysheetfile()
  const activeSheet = file?.find((s: any) => s.status === '1')
  if (activeSheet?._sheetDbId) {
    permDialogRef.value?.open(activeSheet._sheetDbId, activeSheet.name || reportName.value, canManage.value)
  } else {
    proxy.$modal.msgWarning('请先选择一�?Sheet')
  }
}

function onKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') { e.preventDefault(); handleSave() }
}

onMounted(() => { initEditor(); document.addEventListener('keydown', onKeydown) })
onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
  try {
    luckysheet.destroy()
    loadedRanges.clear()
    cellSnapshots.clear()
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
