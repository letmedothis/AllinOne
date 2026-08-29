<template>
  <div class="collect-sheet" ref="sheetContainerRef">
    <!-- Loading状态 -->
    <div v-if="loading" class="sheet-loading">
      <el-skeleton :rows="10" animated />
      <p class="loading-text">正在加载电子表格...</p>
    </div>

    <!-- 错误状态 -->
    <div v-if="error" class="sheet-error">
      <el-result icon="error" title="表格加载失败" :sub-title="error">
        <template #extra>
          <el-button type="primary" @click="handleRetry">重新加载</el-button>
        </template>
      </el-result>
    </div>

    <!-- Luckysheet 容器 -->
    <div id="luckysheet" ref="luckysheetRef" class="luckysheet-container"></div>

    <!-- 操作栏 -->
    <div v-if="!readonly" class="sheet-toolbar">
      <el-space>
        <el-button type="primary" icon="Upload" @click="handleSave" :loading="saving">
          保存
        </el-button>
        <el-button icon="RefreshRight" @click="handleUndo">撤销</el-button>
        <el-button icon="Refresh" @click="handleRedo">重做</el-button>
      </el-space>
    </div>
  </div>
</template>

<script setup lang="ts" name="CollectSheet">

// ===== 本地 Luckysheet 引入（从 allinone-luckysheet/ 本地源码构建）=====
import 'luckysheet/dist/plugins/css/pluginsCss.css'
import 'luckysheet/dist/plugins/plugins.css'
import 'luckysheet/dist/css/luckysheet.css'
import { loadLuckysheet } from '@/utils/luckysheetLoader'

const props = defineProps({
  /** 表格数据（JSON字符串或对象） */
  sheetData: {
    type: [String, Object] as PropType<string | Record<string, any> | null>,
    default: null
  },
  /** 是否只读 */
  readonly: {
    type: Boolean,
    default: false
  },
  /** 容器高度 */
  height: {
    type: Number,
    default: 500
  }
})

const emit = defineEmits<{
  /** 即时编辑信号（无数据）：让父组件第一时间置脏，用于离开提示等场景 */
  touch: []
  /** 防抖后的全量数据变更 */
  change: [data: any]
  save: [data: any]
}>()

const { proxy } = getCurrentInstance()!

const sheetContainerRef = ref<HTMLElement | null>(null)
const luckysheetRef = ref<HTMLElement | null>(null)
const loading = ref<boolean>(true)
const saving = ref<boolean>(false)
const error = ref<string>('')

/** 初始化 Luckysheet */
async function initSheet(initialData: string | Record<string, any> | null = props.sheetData) {
  loading.value = true
  error.value = ''

  try {
    // 确保 Luckysheet 已加载
    if (typeof (window as any).luckysheet?.create !== 'function') {
      await loadLuckysheet()
    }

    const luckysheet = (window as any).luckysheet
    if (!luckysheet) {
      throw new Error('Luckysheet 库加载失败')
    }

    const options: any = {
      container: 'luckysheet',
      title: props.readonly ? '数据查看' : '数据填报',
      lang: 'zh',
      allowUpdate: !props.readonly,
      showtoolbar: !props.readonly,
      showinfobar: !props.readonly,
      showsheetbar: !props.readonly,
      showstatisticBar: !props.readonly,
      sheetFormulaBar: !props.readonly,
      allowCopy: true,
      // 图表插件（chartmix）依赖约 1.8MB 的 vue2/element-ui/echarts 本地库：
      // 仅在可编辑、或工作簿本身包含图表（只读时需要渲染）时才加载
      plugins: props.readonly && !workbookHasCharts(initialData) ? [] : ['chart'],
      myFolderUrl: '',
      hook: {
        cellUpdated: (cell: any, r: number, c: number) => {
          handleSheetChange()
        }
      }
    }

    // 如果有初始数据，解析并设置
    if (initialData) {
      const data = typeof initialData === 'string'
        ? JSON.parse(initialData)
        : initialData

      if (Array.isArray(data)) {
        options.data = data
      } else if (data.sheets) {
        options.data = data.sheets
      }
      if (data.title) {
        options.title = data.title
      }
    }

    luckysheet.create(options)
    loading.value = false
  } catch (e: any) {
    error.value = e.message || '表格初始化异常'
    loading.value = false
    console.error('[CollectSheet] init error:', e)
  }
}

/** 动态加载 Luckysheet 依赖(共享加载器:plugin.js 内含 jQuery,先于主库执行) */

/** 判断工作簿（数组或 {sheets:[...]} 结构）是否包含图表定义 */
function workbookHasCharts(data: string | Record<string, any> | null): boolean {
  if (!data) return false
  try {
    const parsed = typeof data === 'string' ? JSON.parse(data) : data
    const sheets = Array.isArray(parsed) ? parsed : parsed?.sheets
    return Array.isArray(sheets) && sheets.some((s: any) => Array.isArray(s?.chart) && s.chart.length > 0)
  } catch {
    // 解析失败时按包含图表处理，避免只读页丢失图表渲染
    return true
  }
}

/** 获取当前表格数据 */
function getData(): any {
  const luckysheet = (window as any).luckysheet
  if (!luckysheet) return null
  const sheets = luckysheet.getAllSheets()
  if (!Array.isArray(sheets)) return sheets
  return sheets.map((sheet: any) => {
    const persistedSheet = { ...sheet }
    delete persistedSheet.data
    return persistedSheet
  })
}

/** 从外部加载数据 */
async function loadData(data: string | Record<string, any>) {
  try {
    // 模板接口可能先于经典脚本执行完成返回，此时 window.luckysheet 尚不可用
    // （存在未挂载完成的空对象），必须先等待库就绪再 destroy，否则重放数据丢失
    if (typeof (window as any).luckysheet?.create !== 'function') {
      await loadLuckysheet()
    }
    const luckysheet = (window as any).luckysheet
    luckysheet.destroy()
    loading.value = true
    nextTick(() => {
      initSheet(data)
    })
  } catch (e: any) {
    error.value = e.message || '表格加载失败'
    loading.value = false
  }
}

/** 表格内容变更防抖计时器：全量序列化整本工作簿开销大，尾沿合并连续击键 */
let changeDebounceTimer: number | null = null

/** 表格内容变更处理 */
function handleSheetChange() {
  // 即时信号：父组件据此置脏（离开提示依赖它），不等防抖
  emit('touch')
  if (changeDebounceTimer !== null) window.clearTimeout(changeDebounceTimer)
  changeDebounceTimer = window.setTimeout(() => {
    changeDebounceTimer = null
    emit('change', getData())
  }, 300)
}

/** 保存操作 */
function handleSave() {
  const data = getData()
  emit('save', data)
}

/** 撤销 */
function handleUndo() {
  const luckysheet = (window as any).luckysheet
  if (luckysheet?.undo) luckysheet.undo()
}

/** 重做 */
function handleRedo() {
  const luckysheet = (window as any).luckysheet
  if (luckysheet?.redo) luckysheet.redo()
}

/** 重试加载 */
function handleRetry() {
  error.value = ''
  loading.value = true
  nextTick(() => initSheet())
}

/** 生命周期钩子 */
onMounted(() => {
  nextTick(() => initSheet())
})

onUnmounted(() => {
  if (changeDebounceTimer !== null) {
    window.clearTimeout(changeDebounceTimer)
    changeDebounceTimer = null
  }
  const luckysheet = (window as any).luckysheet
  if (luckysheet?.destroy) luckysheet.destroy()
})

/** 暴露方法给父组件 */
defineExpose({
  getData,
  loadData
})
</script>

<style scoped>
.collect-sheet {
  position: relative;
  width: 100%;
  min-height: v-bind('height + "px"');
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.sheet-loading {
  padding: 40px;
  text-align: center;
}

.loading-text {
  margin-top: 16px;
  color: #909399;
  font-size: 14px;
}

.sheet-error {
  padding: 40px;
}

.luckysheet-container {
  width: 100%;
  height: v-bind('height + "px"');
}

.sheet-toolbar {
  padding: 12px 16px;
  border-top: 1px solid #ebeef5;
  background: #fafafa;
}
</style>
