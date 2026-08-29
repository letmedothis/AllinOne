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
// Luckysheet 产物是依赖全局 jQuery 的经典脚本（plugin.js 内含 jQuery，须先于主库执行），
// 不能作为 ES Module 动态 import：模块作用域下顶层 $ 未定义会中断求值，window.luckysheet 永不挂载。
// 主库须经 /luckysheet/ 固定站点路径加载（vite 插件提供）：
// 其内部按相对路径加载 expendPlugins/chart/*，相对页面 URL 会 404。
const LUCKYSHEET_PLUGIN_JS = '/luckysheet/plugins/js/plugin.js'
const LUCKYSHEET_UMD_JS = '/luckysheet/luckysheet.umd.js'

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
      // 启用内置图表插件（chartmix）：由 luckysheet 按此清单动态加载，
      // 依赖公网 CDN 的 vue2/vuex/element-ui/echarts，离线环境不可用
      plugins: ['chart'],
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

/** 按序注入经典 <script>（同一地址只注入一次） */
function loadScriptOnce(src: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const marker = 'data-luckysheet-src'
    let el = document.querySelector(`script[${marker}="${src}"]`) as HTMLScriptElement | null
    if (el) {
      if (el.dataset.loaded === 'true') {
        resolve()
        return
      }
      el.addEventListener('load', () => resolve())
      el.addEventListener('error', () => reject(new Error('Luckysheet 脚本加载失败: ' + src)))
      return
    }
    el = document.createElement('script')
    el.src = src
    el.setAttribute(marker, src)
    el.onload = () => {
      el!.dataset.loaded = 'true'
      resolve()
    }
    el.onerror = () => reject(new Error('Luckysheet 脚本加载失败: ' + src))
    document.head.appendChild(el)
  })
}

/** 动态加载 Luckysheet 依赖：plugin.js（内含 jQuery）先于主库执行 */
async function loadLuckysheet(): Promise<void> {
  await loadScriptOnce(LUCKYSHEET_PLUGIN_JS)
  await loadScriptOnce(LUCKYSHEET_UMD_JS)
  if (typeof (window as any).luckysheet?.create !== 'function') {
    throw new Error('Luckysheet 库加载失败')
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
