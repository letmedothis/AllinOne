<template>
  <div class="app-container">
    <!-- 页面标题 -->
    <page-header :title="pageTitle" @back="handleBack">
      <template #extra>
        <el-space>
          <el-text v-if="lastAutoSaveTime" size="small" type="info">{{ lastAutoSaveTime }}</el-text>
          <el-button @click="handleBack">返回</el-button>
          <el-button v-if="submitStatus !== 'submitted'" type="primary" :loading="saving" @click="handleSaveDraft">
            保存草稿
          </el-button>
          <el-button
            v-if="submitStatus !== 'submitted'"
            type="success"
            :loading="submitting"
            @click="handleSubmit"
          >提交填报</el-button>
        </el-space>
      </template>
    </page-header>

    <!-- 选择模板（新建时显示） -->
    <el-card v-if="!templateId" shadow="never" class="mb8">
      <template #header>
        <span>选择填报模板</span>
      </template>
      <el-form :inline="true">
        <el-form-item label="模板">
          <el-select
            v-model="selectedTemplateId"
            :placeholder="templatesLoading ? '模板列表加载中...' : '请选择模板'"
            :disabled="templatesLoading"
            style="width: 300px"
            @change="onTemplateChange"
          >
            <el-option
              v-for="item in publishedTemplates"
              :key="item.templateId"
              :label="item.templateName"
              :value="item.templateId"
            >
              <span>{{ item.templateName }}</span>
              <span class="template-code">[{{ item.templateCode }}]</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item v-if="selectedTemplate">
          <el-tag>分类ID：{{ selectedTemplate.categoryId || '-' }}</el-tag>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 已选模板信息 -->
    <el-card v-if="templateId" shadow="never" class="mb8">
      <el-descriptions :column="4" size="small">
        <el-descriptions-item label="模板名称">{{ templateInfo.templateName }}</el-descriptions-item>
        <el-descriptions-item label="模板编码">{{ templateInfo.templateCode }}</el-descriptions-item>
        <el-descriptions-item label="版本号">v{{ templateInfo.version }}</el-descriptions-item>
        <el-descriptions-item label="填报状态">
          <el-tag :type="submitStatus === 'submitted' ? 'success' : 'warning'" disable-transitions>
            {{ submitStatus === 'submitted' ? '已提交' : '草稿' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 业务编码 -->
    <el-card v-if="templateId" shadow="never" class="mb8">
      <el-form :inline="true">
        <el-form-item label="业务编码" prop="dataCode">
          <el-input v-model="form.dataCode" placeholder="可选，用于关联业务数据" style="width: 400px" maxlength="64" />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 提交状态提示 -->
    <el-alert
      v-if="submitStatus === 'submitted'"
      title="该数据已提交，无法编辑。如需修改请先联系管理员。"
      type="success"
      :closable="false"
      show-icon
      class="mb8"
    />

    <!-- Luckysheet 填报区域 -->
    <el-card v-if="templateId" shadow="never" class="sheet-card">
      <template #header>
        <div class="sheet-card-header">
          <span>填报内容</span>
          <el-button
            v-if="submitStatus !== 'submitted'"
            size="small"
            icon="Upload"
            @click="triggerImportExcel"
          >导入 Excel</el-button>
        </div>
      </template>
      <CollectSheet
        v-if="sheetKey"
        :key="sheetKey"
        ref="sheetRef"
        :sheetData="form.formData"
        :readonly="submitStatus === 'submitted'"
        :height="700"
        @touch="onSheetTouch"
        @change="onSheetChange"
        @save="onSheetSave"
      />
      <input
        ref="importInputRef"
        type="file"
        accept=".xlsx, .xls"
        style="display: none"
        @change="handleImportExcel"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts" name="CollectDataEdit">
import { useRouter, useRoute } from 'vue-router'
import { getTemplate, listTemplate } from '@/api/collect/template'
import { getData, addData, updateData, submitData } from '@/api/collect/data'
import type { CollectTemplate } from '@/types/api/collect/template'
import type { CollectData } from '@/types/api/collect/data'
import CollectSheet from '@/components/CollectSheet/index.vue'

const { proxy } = getCurrentInstance()!
const router = useRouter()
const route = useRoute()

const sheetRef = ref<InstanceType<typeof CollectSheet> | null>(null)
const saving = ref<boolean>(false)
const submitting = ref<boolean>(false)
const sheetKey = ref<number>(0)

/** 自动保存间隔（毫秒） */
const AUTO_SAVE_INTERVAL = 30 * 1000
/** 表格自上次保存后是否有变更（脏标记） */
const dirty = ref<boolean>(false)
/** 最近一次自动保存的状态文本（如“已自动保存 14:30”） */
const lastAutoSaveTime = ref<string>('')
/** 自动保存定时器 */
let autoSaveTimer: number | null = null
/** 自动保存失败是否已提示过（连续失败只提示一次，恢复成功后重新允许提示） */
let autoSaveErrorNotified = false
/** 表格（重）加载后忽略 change 事件的静默窗口时长：等待 Luckysheet 初始化及模板公式重算 */
const LOAD_CHANGE_SKIP_MS = 5 * 1000
/** 加载静默窗口标记：窗口内的 change 视为加载引起，不算用户修改 */
let pendingLoadChange = false
let loadChangeSkipTimer: number | null = null

/** 是否为只读查看模式（从数据列表查看进入） */
const isEdit = computed(() => !!route.query.id)

/** 模板ID */
const templateId = ref<number | undefined>(undefined)

/** 已发布的模板列表 */
const publishedTemplates = ref<CollectTemplate[]>([])
const selectedTemplateId = ref<number | undefined>(undefined)

/** 当前选中的模板信息 */
const selectedTemplate = computed(() => {
  return publishedTemplates.value.find((t: CollectTemplate) => t.templateId === selectedTemplateId.value)
})

/** 模板信息（编辑时） */
const templateInfo = ref<CollectTemplate>({})

/** 提交状态 */
const submitStatus = ref<'draft' | 'submitted'>('draft')

/** 页面标题 */
const pageTitle = computed(() => {
  if (isEdit.value) return '编辑填报数据'
  return '新增填报'
})

/** 表单数据 */
const form = ref<CollectData>({
  templateId: undefined,
  formData: undefined,
  bizStatus: 'draft',
  version: 1
})

/** 模板列表加载中（请求未返回前禁用下拉，避免误认为无数据） */
const templatesLoading = ref<boolean>(false)

/** 表格（重）加载后开启 change 静默窗口；若加载未触发 change 则超时自动解除，避免吞掉用户首次编辑 */
function armLoadChangeSkip() {
  pendingLoadChange = true
  if (loadChangeSkipTimer !== null) window.clearTimeout(loadChangeSkipTimer)
  loadChangeSkipTimer = window.setTimeout(() => {
    pendingLoadChange = false
    loadChangeSkipTimer = null
  }, LOAD_CHANGE_SKIP_MS)
}

/** 加载已发布的模板列表 */
function loadPublishedTemplates() {
  templatesLoading.value = true
  listTemplate({ pageNum: 1, pageSize: 100, status: '1' }).then(response => {
    publishedTemplates.value = response.rows
  }).finally(() => {
    templatesLoading.value = false
  })
}

/** 模板切换 */
async function onTemplateChange(id: number) {
  templateId.value = id
  form.value.templateId = id
  const response = await getTemplate(id)
  const tmpl = response.data
  if (tmpl) templateInfo.value = tmpl
  if (tmpl?.templateJson) {
    form.value.formData = tmpl.templateJson
  }
  // 新模板内容尚未产生用户修改，重置脏标记并跳过加载引起的 change
  dirty.value = false
  armLoadChangeSkip()
  // 刷新sheet
  sheetKey.value++
}

/** 加载已有数据 */
function loadData() {
  const id = route.query.id as string
  if (!id) return

  getData(Number(id)).then(response => {
    const data = response.data!
    form.value = data
    templateId.value = data.templateId
    submitStatus.value = data.bizStatus || 'draft'
    // 已有数据重新加载，重置脏标记并跳过加载引起的 change
    dirty.value = false
    armLoadChangeSkip()
    sheetKey.value++

    // 加载模板信息
    if (data.templateId) {
      listTemplate({ pageNum: 1, pageSize: 100 }).then(res => {
        const tmpl = (res.rows as CollectTemplate[]).find((t: CollectTemplate) => t.templateId === data.templateId)
        if (tmpl) {
          templateInfo.value = tmpl
        }
      })
    }
  })
}

/** 表格数据变更（CollectSheet 已做 300ms 防抖） */
function onSheetChange(data: any) {
  form.value.formData = typeof data === 'string' ? data : JSON.stringify(data)
  // 初始加载模板也会触发一次 change，不算作用户修改
  if (pendingLoadChange) {
    pendingLoadChange = false
    return
  }
  dirty.value = true
}

/** 用户开始编辑（即时信号，先于防抖的 change）：立刻置脏以驱动离开提示 */
function onSheetTouch() {
  if (pendingLoadChange) return
  dirty.value = true
}

/** 表格保存事件 */
function onSheetSave(data: any) {
  form.value.formData = typeof data === 'string' ? data : JSON.stringify(data)
}

/** ===== Excel 导入 ===== */

/** 导入单表行列上限，防止超大文件把工作簿撑爆 */
const IMPORT_MAX_ROWS = 500
const IMPORT_MAX_COLS = 50

const importInputRef = ref<HTMLInputElement | null>(null)
const importing = ref<boolean>(false)

function triggerImportExcel() {
  importInputRef.value?.click()
}

/**
 * 本地解析 Excel 并写入当前工作簿的第一个工作表（自 A1 起，仅覆盖网格内的单元格）。
 * 客户端解析避免新增后端导入面；导入后走既有 loadData 通道重建表格。
 */
async function handleImportExcel(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  // 允许重复选择同一文件：先清空 value
  input.value = ''
  if (!file) return
  if (importing.value) return

  importing.value = true
  try {
    const XLSX = await import('xlsx')
    const workbook = XLSX.read(await file.arrayBuffer(), { type: 'array' })
    const worksheet = workbook.Sheets[workbook.SheetNames[0]]
    if (!worksheet) {
      proxy.$modal.msgError('Excel 文件中没有工作表')
      return
    }
    // raw:false 取格式化后的显示文本；defval 填充空单元格保证行对齐
    const grid: string[][] = XLSX.utils.sheet_to_json(worksheet, { header: 1, raw: false, defval: '' })
    if (!grid.length) {
      proxy.$modal.msgError('Excel 文件中没有数据')
      return
    }
    const applyRows = grid.slice(0, IMPORT_MAX_ROWS).map((row: any[]) =>
      (row.length ? row : ['']).slice(0, IMPORT_MAX_COLS).map((cell: any) => (cell == null ? '' : String(cell)))
    )
    const truncated = grid.length > IMPORT_MAX_ROWS || applyRows.some((row: string[]) => row.length > IMPORT_MAX_COLS)

    const maxCol = Math.max(...applyRows.map((row: string[]) => row.length))
    const confirmed = await proxy.$modal.confirm(
      `将从 Excel 导入 ${applyRows.length} 行 × ${maxCol} 列到第一个工作表（自 A1 起，覆盖原有内容）${truncated ? '，超出部分将被截断' : ''}。是否继续？`
    ).then(() => true).catch(() => false)
    if (!confirmed) return

    const sheets = sheetRef.value?.getSheetData()
    if (!Array.isArray(sheets) || !sheets.length) {
      proxy.$modal.msgError('表格尚未初始化完成，请稍后重试')
      return
    }
    const target = { ...sheets[0] }
    const cellMap = new Map<string, any>()
    for (const cellData of (target.celldata as any[]) ?? []) {
      cellMap.set(`${cellData.r}_${cellData.c}`, cellData)
    }
    applyRows.forEach((row: string[], r: number) => {
      row.forEach((value: string, c: number) => {
        if (value === '') return
        const key = `${r}_${c}`
        const existing = cellMap.get(key)
        const v = { ...(existing?.v ?? {}) }
        v.v = value
        v.m = value
        cellMap.set(key, { r, c, v })
      })
    })
    target.celldata = Array.from(cellMap.values())
    // data 网格由 celldata 重建，避免旧 data 覆盖导入内容
    delete target.data

    const merged = [...sheets]
    merged[0] = target
    await sheetRef.value?.loadData(JSON.stringify(merged))
    proxy.$modal.msgSuccess(`已导入 ${applyRows.length} 行数据`)
  } catch (e: any) {
    proxy.$modal.msgError('导入 Excel 失败：' + (e?.message || '文件解析异常'))
    console.error('导入Excel失败:', e)
  } finally {
    importing.value = false
  }
}

/** 手动保存草稿（带成功/失败提示） */
function handleSaveDraft() {
  return saveDraft(false)
}

/**
 * 合并服务端返回的元数据（dataId/version/updateTime 等）到本地表单。
 * 后端不再回传大体积 formData，本地副本以刚提交的内容为准，不可覆盖。
 */
function mergeServerMeta(data: CollectData | undefined | null) {
  if (!data) return
  const { formData: _ignored, ...meta } = data
  form.value = { ...form.value, ...meta }
}

/** 保存草稿（silent=true 时为自动保存：不弹成功提示，失败提示去重） */
async function saveDraft(silent = false) {
  if (!templateId.value) {
    if (!silent) proxy.$modal.msgWarning('请选择模板')
    return
  }
  saving.value = true
  try {
    // 从Luckysheet获取数据
    if (sheetRef.value) {
      const sheetData = sheetRef.value.getSheetData()
      form.value.formData = typeof sheetData === 'string' ? sheetData : JSON.stringify(sheetData)
    }
    // 注意：不要在前端设置 bizStatus —— 后端 insert 强制 'draft'、update 忽略该字段

    if (form.value.dataId) {
      const response = await updateData(form.value)
      mergeServerMeta(response.data)
      if (!silent) proxy.$modal.msgSuccess('草稿已更新')
    } else {
      const res: any = await addData(form.value)
      mergeServerMeta(res.data)
      router.replace({ query: { ...route.query, id: form.value.dataId } })
      if (!silent) proxy.$modal.msgSuccess('草稿已保存')
    }
    // 保存成功：清脏；自动保存记录状态文本并恢复失败提示
    dirty.value = false
    if (silent) {
      autoSaveErrorNotified = false
      lastAutoSaveTime.value = formatAutoSaveTime(new Date())
    }
  } catch (e) {
    if (silent) {
      // 自动保存连续失败只提示一次，恢复成功后重新允许提示
      if (!autoSaveErrorNotified) {
        autoSaveErrorNotified = true
        proxy.$modal.msgError('自动保存草稿失败，请检查网络后手动保存')
      }
    } else {
      proxy.$modal.msgError('保存草稿失败')
    }
    console.error('保存草稿失败:', e)
  } finally {
    saving.value = false
  }
}

/** ===== 草稿自动保存 ===== */

/** 格式化自动保存状态文本 HH:mm */
function formatAutoSaveTime(date: Date): string {
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `已自动保存 ${hh}:${mm}`
}

/** 是否满足自动保存条件：已选模板且非已提交（只读）状态 */
const autoSaveEnabled = computed(() => !!templateId.value && submitStatus.value !== 'submitted')

/** 启动自动保存定时器 */
function startAutoSave() {
  if (autoSaveTimer !== null) return
  autoSaveTimer = window.setInterval(() => {
    // 仅当自上次保存后表格有变更时才静默保存，保存/提交进行中跳过
    if (!dirty.value || saving.value || submitting.value) return
    saveDraft(true)
  }, AUTO_SAVE_INTERVAL)
}

/** 停止自动保存定时器 */
function stopAutoSave() {
  if (autoSaveTimer !== null) {
    window.clearInterval(autoSaveTimer)
    autoSaveTimer = null
  }
}

// 模板已选择且为草稿态时启动定时器；提交成功或只读状态停止
watch(autoSaveEnabled, (enabled) => {
  if (enabled) startAutoSave()
  else stopAutoSave()
})

/** 关闭/刷新页面前，存在未保存修改时提示用户 */
function handleBeforeUnload(e: BeforeUnloadEvent) {
  if (dirty.value && submitStatus.value !== 'submitted') {
    e.preventDefault()
    // Chrome 需要 returnValue 才会弹出确认框
    e.returnValue = ''
  }
}

/** 提交填报 */
async function handleSubmit() {
  if (!templateId.value) {
    proxy.$modal.msgWarning('请选择模板')
    return
  }
  proxy.$modal.confirm('确认提交该填报数据？提交后不可修改。').then(async () => {
    submitting.value = true
    try {
      // 保存数据
      if (sheetRef.value) {
        const sheetData = sheetRef.value.getSheetData()
        form.value.formData = typeof sheetData === 'string' ? sheetData : JSON.stringify(sheetData)
      }

      let dataId = form.value.dataId
      if (dataId) {
        const response = await updateData(form.value)
        mergeServerMeta(response.data)
      } else {
        const res: any = await addData(form.value)
        mergeServerMeta(res.data)
        dataId = form.value.dataId
      }
      // 草稿已随提交落库，清脏
      dirty.value = false

      // 执行提交
      if (dataId) {
        await submitData(dataId)
        submitStatus.value = 'submitted'
        // 提交成功后数据只读，停止草稿自动保存
        stopAutoSave()
        proxy.$modal.msgSuccess('提交成功')
      }
    } catch (e) {
      proxy.$modal.msgError('提交失败')
      console.error('提交失败:', e)
    } finally {
      submitting.value = false
    }
  }).catch(() => {})
}

/** 返回 */
function handleBack() {
  router.push({ path: '/collect/data' })
}

onMounted(() => {
  window.addEventListener('beforeunload', handleBeforeUnload)
  if (route.query.id) {
    loadData()
  } else {
    loadPublishedTemplates()
  }
})

onUnmounted(() => {
  stopAutoSave()
  if (loadChangeSkipTimer !== null) {
    window.clearTimeout(loadChangeSkipTimer)
    loadChangeSkipTimer = null
  }
  window.removeEventListener('beforeunload', handleBeforeUnload)
})
</script>

<style scoped>
.sheet-card {
  min-height: 600px;
}

.sheet-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.template-code {
  color: #909399;
  margin-left: 8px;
  font-size: 12px;
}
</style>
