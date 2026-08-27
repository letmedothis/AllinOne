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
import { requestJimuTicket } from '@/api/report/ticket'
import { getToken } from '@/utils/auth'
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

/**
 * 拼接 iframe 地址：后端返回引擎访问路径，此处先向服务端换取一次性票据（ticket）拼到 URL。
 * JimuReportTokenService 会消费 ticket 换出登录令牌完成鉴权，避免把长期 JWT 暴露在 URL 中；
 * 票据换取失败时降级为旧的 token 参数方式。
 */
async function buildSrc(url: string): Promise<string> {
  try {
    const response = await requestJimuTicket()
    const ticket = response.data
    if (ticket) {
      return url + (url.includes('?') ? '&' : '?') + 'ticket=' + encodeURIComponent(ticket)
    }
  } catch (e) {
    console.warn('获取JimuReport票据失败，降级为token方式', e)
  }
  const token = getToken()
  if (!token) return url
  return url + (url.includes('?') ? '&' : '?') + 'token=' + encodeURIComponent(token)
}

/** 加载大屏 */
function loadDashboard() {
  const id = route.query.id as string
  if (!id) {
    error.value = '缺少大屏ID参数'
    loading.value = false
    return
  }

  getConfig(Number(id)).then(async response => {
    const data = response.data
    if (!data) {
      error.value = '大屏不存在'
      return
    }
    if (data.status !== '0') {
      error.value = '大屏已停用'
      return
    }
    if (!data.url) {
      error.value = '该大屏未配置引擎ID，无法加载'
      return
    }
    src.value = await buildSrc(data.url)
  }).catch((e: any) => {
    error.value = e.message || '加载大屏信息失败'
  }).finally(() => {
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
  router.push({ path: '/report/config' })
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
