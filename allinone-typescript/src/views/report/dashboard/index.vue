<template>
  <div class="fullscreen-container">
    <!-- Loading -->
    <div v-if="loading" class="fullscreen-loading">
      <el-skeleton :rows="5" animated />
      <p class="loading-text">正在加载大屏...</p>
    </div>

    <!-- 错误状态 -->
    <div v-if="error" class="fullscreen-error">
      <el-result icon="warning" title="大屏加载失败" :sub-title="error">
        <template #extra>
          <el-button type="primary" @click="handleRetry">重新加载</el-button>
          <el-button @click="handleBack">返回</el-button>
        </template>
      </el-result>
    </div>

    <!-- 大屏iframe（全屏） -->
    <div v-show="!loading && !error" class="fullscreen-content">
      <ReportFrame
        ref="frameRef"
        :src="src"
        height="100vh"
        @load="onLoad"
      />
    </div>

    <!-- 顶部工具栏（悬浮） -->
    <transition name="toolbar-fade">
      <div v-show="showToolbar" class="floating-toolbar">
        <el-space>
          <el-button size="small" icon="Back" @click="handleBack">返回</el-button>
          <el-button size="small" icon="Refresh" @click="handleRefresh">刷新</el-button>
          <el-button size="small" icon="FullScreen" @click="handleFullscreen">
            {{ isFullscreen ? '退出全屏' : '全屏' }}
          </el-button>
        </el-space>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts" name="ReportDashboardView">
import { useRouter, useRoute } from 'vue-router'
import { getConfig } from '@/api/report/config'
import ReportFrame from '@/components/ReportFrame/index.vue'

const { proxy } = getCurrentInstance()!
const router = useRouter()
const route = useRoute()

const frameRef = ref<InstanceType<typeof ReportFrame> | null>(null)
const loading = ref<boolean>(true)
const error = ref<string>('')
const src = ref<string>('')
const showToolbar = ref<boolean>(true)
const isFullscreen = ref<boolean>(false)
let hideToolbarTimer: ReturnType<typeof setTimeout> | null = null

/** 加载大屏 */
function loadDashboard() {
  const id = route.query.id as string
  if (!id) {
    error.value = '缺少大屏ID参数'
    loading.value = false
    return
  }

  getConfig(Number(id)).then(response => {
    const data = response.data
    if (!data) {
      error.value = '大屏不存在'
      loading.value = false
      return
    }
    if (data.status !== '0') {
      error.value = '大屏已停用'
      loading.value = false
      return
    }
    src.value = data.url
    loading.value = false
  }).catch((e: any) => {
    error.value = e.message || '加载大屏信息失败'
    loading.value = false
  })
}

/** iframe加载完成 */
function onLoad(iframe: HTMLIFrameElement) {
  console.log('大屏加载完成')
  // 3秒后自动隐藏工具栏
  autoHideToolbar()
}

/** 自动隐藏工具栏 */
function autoHideToolbar() {
  if (hideToolbarTimer) clearTimeout(hideToolbarTimer)
  hideToolbarTimer = setTimeout(() => {
    showToolbar.value = false
  }, 5000)
}

/** 显示工具栏 */
function showToolbarTemporarily() {
  showToolbar.value = true
  autoHideToolbar()
}

/** 刷新 */
function handleRefresh() {
  if (frameRef.value) {
    frameRef.value.reload()
  }
  showToolbarTemporarily()
}

/** 切换全屏 */
function handleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
    isFullscreen.value = true
  } else {
    document.exitFullscreen()
    isFullscreen.value = false
  }
}

/** 重试 */
function handleRetry() {
  error.value = ''
  loading.value = true
  loadDashboard()
}

/** 返回 */
function handleBack() {
  if (isFullscreen.value) {
    document.exitFullscreen()
  }
  router.push({ path: '/report/dashboard' })
}

/** 监听全屏变化 */
function onFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement
}

/** 鼠标移动时显示工具栏 */
function onMouseMove() {
  showToolbarTemporarily()
}

onMounted(() => {
  loadDashboard()
  document.addEventListener('fullscreenchange', onFullscreenChange)
  document.addEventListener('mousemove', onMouseMove)
})

onUnmounted(() => {
  document.removeEventListener('fullscreenchange', onFullscreenChange)
  document.removeEventListener('mousemove', onMouseMove)
  if (hideToolbarTimer) clearTimeout(hideToolbarTimer)
  if (isFullscreen.value) {
    document.exitFullscreen()
  }
})
</script>

<style scoped>
.fullscreen-container {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: #000;
  overflow: hidden;
}

.fullscreen-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px;
  background: #fff;
}

.loading-text {
  margin-top: 16px;
  color: #909399;
}

.fullscreen-error {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
}

.fullscreen-content {
  width: 100vw;
  height: 100vh;
}

.floating-toolbar {
  position: fixed;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1001;
  background: rgba(0, 0, 0, 0.7);
  padding: 8px 16px;
  border-radius: 8px;
  backdrop-filter: blur(8px);
}

.toolbar-fade-enter-active,
.toolbar-fade-leave-active {
  transition: opacity 0.3s ease;
}

.toolbar-fade-enter-from,
.toolbar-fade-leave-to {
  opacity: 0;
}
</style>
