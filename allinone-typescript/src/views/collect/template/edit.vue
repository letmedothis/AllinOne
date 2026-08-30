<template>
  <div class="app-container">
    <!-- 页面标题 -->
    <page-header
      :title="pageTitle"
      :subtitle="'使用Luckysheet在线设计填报模板'"
      @back="handleBack"
    >
      <template #extra>
        <el-space>
          <el-button @click="handleBack">返回</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">保存模板</el-button>
          <el-button
            v-if="!readonly && form.status !== '1'"
            type="success"
            :loading="publishing"
            @click="handlePublish"
          >发布模板</el-button>
        </el-space>
      </template>
    </page-header>

    <!-- 模板基本信息 -->
    <el-card shadow="never" class="info-card" v-if="!readonly">
      <el-form :model="form" label-width="80px" size="small">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="模板名称">
              <el-input v-model="form.templateName" placeholder="请输入模板名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="模板编码">
              <el-input v-model="form.templateCode" placeholder="请输入模板编码" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="所属分类">
              <el-tree-select
                v-model="form.categoryId"
                :data="categoryOptions"
                :props="{ value: 'categoryId', label: 'categoryName', children: 'children' }"
                placeholder="选择分类"
                check-strictly
                clearable
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 状态提示 -->
    <el-alert
      v-if="readonly"
      :title="'当前为只读模式，无法编辑'"
      type="info"
      :closable="false"
      show-icon
      class="mb8"
    />

    <!-- Luckysheet 编辑器 -->
    <el-card shadow="never" class="sheet-card">
      <template #header>
        <div class="sheet-card-header">
          <span>模板内容</span>
          <el-space v-if="!readonly">
            <el-tooltip content="选中单个单元格后，将其绑定为 RuoYi 字典下拉（填报端下拉选择，提交端校验取值）">
              <el-button size="small" icon="Link" @click="handleBindDict">绑定字典</el-button>
            </el-tooltip>
            <el-tooltip content="清除所选单元格的字典/下拉绑定">
              <el-button size="small" icon="Delete" @click="handleUnbindDict">清除绑定</el-button>
            </el-tooltip>
          </el-space>
        </div>
      </template>
      <CollectSheet
        ref="sheetRef"
        :sheetData="form.templateJson"
        :readonly="readonly"
        :height="700"
      />
    </el-card>

    <!-- 字典绑定弹窗 -->
    <el-dialog v-model="dictDialog.visible" title="绑定字典下拉" width="480px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="当前单元格">
          <el-tag>{{ dictDialog.cellLabel }}</el-tag>
        </el-form-item>
        <el-form-item label="字典类型">
          <el-select
            v-model="dictDialog.dictType"
            placeholder="请选择 RuoYi 字典类型"
            filterable
            style="width: 100%"
            @change="onDictTypeChange"
          >
            <el-option v-for="t in dictTypeOptions" :key="t.dictId" :label="`${t.dictName}（${t.dictType}）`" :value="t.dictType" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="dictDialog.previewLabels.length" label="下拉选项">
          <el-tag v-for="label in dictDialog.previewLabels" :key="label" class="dict-preview-tag">{{ label }}</el-tag>
        </el-form-item>
        <el-form-item label="必填">
          <el-switch v-model="dictDialog.required" />
          <span class="dict-required-tip">开启后，提交时该单元格不允许为空</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :disabled="!dictDialog.dictType" @click="confirmBindDict">确 定</el-button>
          <el-button @click="dictDialog.visible = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="CollectTemplateEdit">
import { useRouter, useRoute } from 'vue-router'
import type { CollectTemplate } from '@/types/api/collect/template'
import { getTemplate, addTemplate, updateTemplate, publishTemplate } from '@/api/collect/template'
import { listCategory } from '@/api/collect/category'
import { optionselect as listDictTypeOptions } from '@/api/system/dict/type'
import { getDicts } from '@/api/system/dict/data'
import CollectSheet from '@/components/CollectSheet/index.vue'

const { proxy } = getCurrentInstance()!
const router = useRouter()
const route = useRoute()

const sheetRef = ref<InstanceType<typeof CollectSheet> | null>(null)
const saving = ref<boolean>(false)
const publishing = ref<boolean>(false)
const readonly = ref<boolean>(route.query.readonly === '1')
const categoryOptions = ref<any[]>([])

/** 字典类型下拉（optionselect 接口仅需登录态） */
const dictTypeOptions = ref<any[]>([])

/** 字典绑定表：key = "工作表序号!行_列"，保存时合并为 dataVerification 的 collectDict/collectRequired 标记 */
const dictBindings = ref<Record<string, { dictType: string; required: boolean }>>({})

/** 字典绑定弹窗状态 */
const dictDialog = reactive({
  visible: false,
  dictType: '',
  required: false,
  previewLabels: [] as string[],
  cellLabel: ''
})

/** 绑定目标（打开弹窗时捕获） */
let bindTarget: { sheetOrder: number; row: number; col: number } | null = null

/** 页面标题 */
const pageTitle = computed(() => {
  if (readonly.value) return '模板预览'
  return form.value.templateId ? '编辑模板' : '新建模板'
})

/** 表单数据 */
const form = ref<CollectTemplate>({
  templateName: undefined,
  templateCode: undefined,
  categoryId: undefined,
  remark: undefined,
  templateJson: undefined,
  status: '0'
})

/** 加载模板信息 */
function loadTemplate() {
  const id = route.query.id as string
  if (!id) return

  getTemplate(Number(id)).then(response => {
    const data = response.data!
    form.value = data
    // 恢复既有字典绑定标记
    loadDictBindings(data.templateJson)
    // 将config配置加载到Luckysheet
    if (data.templateJson && sheetRef.value) {
      sheetRef.value.loadData(data.templateJson)
    }
  })
}

/** 从模板 JSON 恢复字典绑定标记到页面绑定表 */
function loadDictBindings(templateJson?: string) {
  dictBindings.value = {}
  if (!templateJson) return
  try {
    const sheets = JSON.parse(templateJson)
    if (!Array.isArray(sheets)) return
    sheets.forEach((sheet: any, order: number) => {
      const verification = sheet?.dataVerification ?? {}
      Object.entries(verification).forEach(([cellKey, conf]: [string, any]) => {
        if (conf?.collectDict) {
          dictBindings.value[`${order}!${cellKey}`] = {
            dictType: conf.collectDict,
            required: !!conf.collectRequired
          }
        }
      })
    })
  } catch {
    // 模板 JSON 结构异常时忽略绑定恢复（提交校验同样会跳过）
  }
}

/** 保存前把绑定表合并进工作簿 JSON：先清标记再按当前绑定重建，保证解绑不残留 */
function applyDictMarkers(sheets: any[]) {
  for (const sheet of sheets) {
    const verification = sheet?.dataVerification
    if (verification) {
      for (const cellKey of Object.keys(verification)) {
        if (verification[cellKey]) {
          delete verification[cellKey].collectDict
          delete verification[cellKey].collectRequired
        }
      }
    }
  }
  for (const [key, binding] of Object.entries(dictBindings.value)) {
    const bang = key.indexOf('!')
    const order = Number(key.slice(0, bang))
    const cellKey = key.slice(bang + 1)
    const conf = sheets[order]?.dataVerification?.[cellKey]
    if (!conf) continue
    conf.collectDict = binding.dictType
    if (binding.required) conf.collectRequired = true
  }
}

/** 捕获当前选中单元格作为绑定目标，并打开绑定弹窗 */
function handleBindDict() {
  const luckysheet = (window as any).luckysheet
  const selection = luckysheet?.getluckysheet_select_save?.() ?? []
  const range = selection[selection.length - 1]
  if (!range) {
    proxy.$modal.msgWarning('请先在表格中选中要绑定的单元格')
    return
  }
  const row = range.row?.[0]
  const col = range.column?.[0]
  if (row == null || col == null || range.row[0] !== range.row[1] || range.column[0] !== range.column[1]) {
    proxy.$modal.msgWarning('请选中单个单元格')
    return
  }
  const currentSheet = luckysheet.getSheet?.() ?? {}
  const files: any[] = luckysheet.getluckysheetfile?.() ?? []
  const sheetOrder = files.findIndex((f: any) => f.index === currentSheet.index)
  if (sheetOrder < 0) {
    proxy.$modal.msgError('未定位到当前工作表')
    return
  }
  bindTarget = { sheetOrder, row, col }

  const existing = dictBindings.value[`${sheetOrder}!${row}_${col}`]
  dictDialog.dictType = existing?.dictType ?? ''
  dictDialog.required = existing?.required ?? false
  dictDialog.previewLabels = []
  const columnLabel = String.fromCharCode(65 + col)
  dictDialog.cellLabel = `${columnLabel}${row + 1}`
  dictDialog.visible = true

  if (!dictTypeOptions.value.length) {
    listDictTypeOptions().then((response: any) => {
      dictTypeOptions.value = response.data ?? []
    })
  }
  if (dictDialog.dictType) {
    loadDictPreview(dictDialog.dictType)
  }
}

/** 拉取字典选项预览（后端校验同样使用标签+键值） */
function loadDictPreview(dictType: string) {
  getDicts(dictType).then((response: any) => {
    const list: any[] = response.data ?? []
    dictDialog.previewLabels = list.map((d: any) => d.dictLabel).filter(Boolean)
  })
}

function onDictTypeChange(dictType: string) {
  dictDialog.previewLabels = []
  if (dictType) loadDictPreview(dictType)
}

/** 确认绑定：写入 Luckysheet 下拉验证 + 记录绑定表 */
function confirmBindDict() {
  if (!bindTarget || !dictDialog.dictType) return
  const luckysheet = (window as any).luckysheet
  try {
    luckysheet.setDataVerification(
      {
        type: 'dropdown',
        value1: dictDialog.previewLabels.join(','),
        prohibitInput: true,
        hintShow: true,
        hintText: '请从下拉列表选择'
      },
      {
        range: { row: [bindTarget.row, bindTarget.row], column: [bindTarget.col, bindTarget.col] }
      }
    )
    dictBindings.value[`${bindTarget.sheetOrder}!${bindTarget.row}_${bindTarget.col}`] = {
      dictType: dictDialog.dictType,
      required: dictDialog.required
    }
    proxy.$modal.msgSuccess('字典绑定成功')
    dictDialog.visible = false
  } catch (e) {
    proxy.$modal.msgError('写入下拉验证失败')
    console.error('绑定字典失败:', e)
  }
}

/** 清除当前选中单元格的绑定：删除 Luckysheet 验证 + 移除绑定表记录 */
function handleUnbindDict() {
  const luckysheet = (window as any).luckysheet
  const selection = luckysheet?.getluckysheet_select_save?.() ?? []
  const range = selection[selection.length - 1]
  if (!range) {
    proxy.$modal.msgWarning('请先在表格中选中要清除的单元格')
    return
  }
  const currentSheet = luckysheet.getSheet?.() ?? {}
  const files: any[] = luckysheet.getluckysheetfile?.() ?? []
  const sheetOrder = files.findIndex((f: any) => f.index === currentSheet.index)
  const row = range.row?.[0]
  const col = range.column?.[0]
  if (sheetOrder >= 0 && row != null && col != null) {
    delete dictBindings.value[`${sheetOrder}!${row}_${col}`]
  }
  try {
    luckysheet.deleteDataVerification?.({})
    proxy.$modal.msgSuccess('已清除该单元格的验证')
  } catch (e) {
    console.error('清除验证失败:', e)
  }
}

/** 加载分类树 */
function loadCategoryTree() {
  listCategory().then(response => {
    categoryOptions.value = proxy.handleTree(response.data, 'categoryId')
  })
}

/** 保存模板 */
async function handleSave(): Promise<boolean> {
  if (!form.value.templateName) {
    proxy.$modal.msgWarning('请输入模板名称')
    return false
  }
  if (!form.value.templateCode) {
    proxy.$modal.msgWarning('请输入模板编码')
    return false
  }

  saving.value = true

  try {
    // 从Luckysheet获取当前表格数据
    if (sheetRef.value) {
      const sheetData = sheetRef.value.getSheetData()
      if (Array.isArray(sheetData)) {
        applyDictMarkers(sheetData)
      }
      form.value.templateJson = typeof sheetData === 'string' ? sheetData : JSON.stringify(sheetData)
    }

    if (form.value.templateId) {
      const response = await updateTemplate(form.value)
      form.value = response.data || form.value
      proxy.$modal.msgSuccess('保存成功')
    } else {
      const res = await addTemplate(form.value)
      form.value = res.data || form.value
      // 更新URL参数
      router.replace({ query: { ...route.query, id: form.value.templateId } })
      proxy.$modal.msgSuccess('创建成功')
    }
    return true
  } catch (e) {
    proxy.$modal.msgError('保存模板失败')
    console.error('保存模板失败:', e)
    return false
  } finally {
    saving.value = false
  }
}

/** 发布模板 */
async function handlePublish() {
  if (!form.value.templateId) {
    proxy.$modal.msgWarning('请先保存模板')
    return
  }

  publishing.value = true
  try {
    // 先保存再发布
    const saved = await handleSave()
    if (!saved) return
    await publishTemplate(form.value.templateId, '1')
    form.value.status = '1'
    proxy.$modal.msgSuccess('发布成功')
  } catch (e) {
    proxy.$modal.msgError('发布模板失败')
    console.error('发布模板失败:', e)
  } finally {
    publishing.value = false
  }
}

/** 返回列表 */
function handleBack() {
  router.push({ path: '/collect/template' })
}

onMounted(() => {
  loadCategoryTree()
  loadTemplate()
})
</script>

<style scoped>
.info-card {
  margin-bottom: 16px;
}

.sheet-card {
  min-height: 600px;
}

.sheet-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.dict-preview-tag {
  margin-right: 6px;
  margin-bottom: 4px;
}

.dict-required-tip {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}

:deep(.page-header) {
  margin-bottom: 16px;
}
</style>
