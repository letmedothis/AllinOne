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
async function initSheet() {
  loading.value = true
  error.value = ''

  try {
    // 确保 Luckysheet 已加载
    if (typeof (window as any).luckysheet === 'undefined') {
      // 动态加载 Luckysheet
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
      myFolderUrl: '',
      hook: {
        cellUpdated: (cell: any, r: number, c: number) => {
          handleSheetChange()
        }
      }
    }

    // 如果有初始数据，解析并设置
    if (props.sheetData) {
      const data = typeof props.sheetData === 'string'
        ? JSON.parse(props.sheetData)
        : props.sheetData

      if (data.sheets) {
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

/** 动态加载 Luckysheet 依赖 */
  function loadLuckysheet(): Promise<void> {
    return (async () => {
      await import('luckysheet/dist/plugins/js/plugin.js')
      await import('luckysheet')
    })()
  }

/** 获取当前表格数据 */
function getData(): any {
  const luckysheet = (window as any).luckysheet
  if (!luckysheet) return null
  return luckysheet.getLuckysheetfile()
}

/** 从外部加载数据 */
function loadData(data: string | Record<string, any>) {
  const luckysheet = (window as any).luckysheet
  if (!luckysheet) return
  luckysheet.destroy()
  loading.value = true
  nextTick(() => {
    initSheet()
  })
}

/** 表格内容变更处理 */
function handleSheetChange() {
  const data = getData()
  emit('change', data)
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
