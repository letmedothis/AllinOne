<template>
  <div class="app-container report-view-page">
    <!-- 页面标题 -->
    <page-header :title="reportName || '报表查看'" @back="handleBack">
      <template #extra>
        <el-button @click="handleBack">返回</el-button>
        <el-button @click="handleRefresh" :icon="'Refresh'">刷新</el-button>
      </template>
    </page-header>

    <!-- Loading -->
    <el-skeleton v-if="loading" :rows="10" animated />

    <!-- 报表iframe -->
    <el-card v-if="!loading && src" shadow="never" class="report-card">
      <ReportFrame
        ref="frameRef"
        :src="src"
        :height="frameHeight"
        :autoLoad="true"
        @load="onLoad"
      />
    </el-card>

    <!-- 无数据 -->
    <el-empty v-if="!loading && !src" :description="'报表不存在或已停用'" />
  </div>
</template>

<script setup lang="ts" name="ReportView">
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
const src = ref<string>('')
const reportName = ref<string>('')
const frameHeight = ref<string>('calc(100vh - 280px)')

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

/** 加载报表配置 */
function loadReport() {
  const id = route.query.id as string
  if (!id) {
    loading.value = false
    return
  }

  getConfig(Number(id)).then(async response => {
    const data = response.data
    if (data && data.url && data.status === '0') {
      reportName.value = data.reportName || ''
      src.value = await buildSrc(data.url)
    }
  }).catch(() => {
  }).finally(() => {
    loading.value = false
  })
}

/** iframe加载完成 */
function onLoad(iframe: HTMLIFrameElement) {
  console.log('报表加载完成')
}

/** 刷新 */
function handleRefresh() {
  if (frameRef.value) {
    frameRef.value.reload()
  }
}

/** 返回 */
function handleBack() {
  router.push({ path: '/report/config' })
}

onMounted(() => {
  loadReport()
})
</script>

<style scoped>
.report-view-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.report-card {
  flex: 1;
  min-height: 500px;
}

.report-card :deep(.el-card__body) {
  height: 100%;
  padding: 0;
}
</style>
