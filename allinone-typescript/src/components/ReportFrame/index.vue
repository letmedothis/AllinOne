<template>
  <div class="report-frame">
    <!-- Loading状态 -->
    <div v-if="loading" class="frame-loading">
      <el-skeleton :rows="5" animated />
      <p class="loading-text">正在加载报表...</p>
    </div>

    <!-- 加载失败 -->
    <div v-if="error" class="frame-error">
      <el-result
        icon="warning"
        title="报表加载失败"
        :sub-title="error"
      >
        <template #extra>
          <el-button type="primary" @click="handleRetry">重新加载</el-button>
        </template>
      </el-result>
    </div>

    <!-- iframe -->
    <iframe
      v-show="!loading && !error"
      ref="iframeRef"
      :src="currentSrc"
      class="frame-iframe"
      :style="iframeStyle"
      frameborder="0"
      @load="handleLoad"
    />
  </div>
</template>

<script setup lang="ts" name="ReportFrame">
import { computed } from 'vue'

const props = defineProps({
  /** 报表URL */
  src: {
    type: String,
    default: ''
  },
  /** 是否自动加载 */
  autoLoad: {
    type: Boolean,
    default: true
  },
  /** 高度（默认100%） */
  height: {
    type: String,
    default: '100%'
  },
  /** 宽度（默认100%） */
  width: {
    type: String,
    default: '100%'
  }
})

const emit = defineEmits<{
  load: [iframe: HTMLIFrameElement]
}>()

const iframeRef = ref<HTMLIFrameElement | null>(null)
const loading = ref<boolean>(true)
const error = ref<string>('')
const loadErrorTimer = ref<ReturnType<typeof setTimeout> | null>(null)
const retryCount = ref<number>(0)
const maxRetry = ref<number>(3)
const loadSuccess = ref<boolean>(false)

/** 当前加载的src（带时间戳防缓存） */
const currentSrc = ref('')

function buildSrc(val: string): string {
  const separator = val.includes('?') ? '&' : '?'
  return `${val}${separator}_t=${Date.now()}`
}

// src 必须保持稳定：加载成功后改变 computed src 会触发 iframe 二次导航，
// 导致一次性 ticket 被重复消费。仅在与 src 绑定变化或显式重试/刷新时重新生成。
watch(() => props.src, (val) => {
  currentSrc.value = val ? buildSrc(val) : ''
}, { immediate: true })

/** iframe样式 */
const iframeStyle = computed(() => ({
  height: props.height,
  width: props.width
}))

/** 处理iframe加载完成 */
function handleLoad() {
  if (!iframeRef.value) return

  try {
    // 检查iframe内容是否有效
    const doc = iframeRef.value.contentDocument || iframeRef.value.contentWindow?.document
    if (!doc || !doc.body) {
      throw new Error('iframe 内容为空')
    }
  } catch (e: any) {
    // 跨域时无法检查内容，假定加载成功
    console.warn('[ReportFrame] 无法访问iframe内容，可能是跨域限制:', e.message)
  }

  loading.value = false
  error.value = ''
  loadSuccess.value = true
  emit('load', iframeRef.value)
}

/** 加载超时处理 */
function startLoadTimer() {
  clearLoadTimer()
  loadErrorTimer.value = setTimeout(() => {
    if (loading.value) {
      // 30秒超时
      error.value = '报表加载超时，请检查网络或报表服务是否正常'
      loading.value = false
    }
  }, 30000)
}

function clearLoadTimer() {
  if (loadErrorTimer.value) {
    clearTimeout(loadErrorTimer.value)
    loadErrorTimer.value = null
  }
}

/** 重试加载 */
function handleRetry() {
  if (retryCount.value >= maxRetry.value) {
    error.value = '重试次数已用完，请稍后再试'
    return
  }
  retryCount.value++
  loading.value = true
  error.value = ''
  loadSuccess.value = false
  if (props.src) currentSrc.value = buildSrc(props.src)
  startLoadTimer()
}

/** 手动重新加载 */
function reload() {
  loading.value = true
  error.value = ''
  loadSuccess.value = false
  if (props.src) currentSrc.value = buildSrc(props.src)
  startLoadTimer()
}

onMounted(() => {
  if (props.autoLoad && props.src) {
    startLoadTimer()
  }
})

onUnmounted(() => {
  clearLoadTimer()
})

defineExpose({
  reload,
  getIframe: () => iframeRef.value
})
</script>

<style scoped>
.report-frame {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 400px;
  overflow: hidden;
}

.frame-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px;
  background: #fff;
  z-index: 1;
}

.loading-text {
  margin-top: 16px;
  color: #909399;
  font-size: 14px;
}

.frame-error {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  z-index: 1;
}

.frame-iframe {
  width: 100%;
  height: 100%;
  border: none;
  background: #fff;
}
</style>
