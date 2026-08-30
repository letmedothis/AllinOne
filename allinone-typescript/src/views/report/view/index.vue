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
    <el-empty v-if="!loading && !src" :description="emptyDesc" />
  </div>
</template>

<script setup lang="ts" name="ReportView">
import { useRouter, useRoute } from 'vue-router'
import { getConfig } from '@/api/report/config'
import { requestJimuTicket } from '@/api/report/ticket'
import ReportFrame from '@/components/ReportFrame/index.vue'

const { proxy } = getCurrentInstance()!
const router = useRouter()
const route = useRoute()

const frameRef = ref<InstanceType<typeof ReportFrame> | null>(null)
const loading = ref<boolean>(true)
const src = ref<string>('')
const reportName = ref<string>('')
const emptyDesc = ref<string>('报表不存在或已停用')
const frameHeight = ref<string>('calc(100vh - 280px)')

/**
 * 拼接 iframe 地址：后端返回引擎访问路径，此处先向服务端换取一次性票据（ticket）拼到 URL。
 * JimuReportTokenService 会消费 ticket 换出登录令牌完成鉴权，长期 JWT 绝不进入 URL。
 */
async function buildSrc(url: string): Promise<string> {
  const response = await requestJimuTicket()
  const ticket = response.data
  if (!ticket) {
    throw new Error('无法获取报表访问票据，请稍后重试')
  }
  return url + (url.includes('?') ? '&' : '?') + 'ticket=' + encodeURIComponent(ticket)
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
    if (!data || !data.url || data.status !== '0') {
      // 配置缺失/停用：保持“报表不存在或已停用”的空态提示
      return
    }
    reportName.value = data.reportName || ''
    try {
      src.value = await buildSrc(data.url)
    } catch (e: any) {
      // 取票失败与配置缺失分开提示，避免误导排查方向
      emptyDesc.value = e.message || '无法获取报表访问票据，请稍后重试'
      console.error('获取JimuReport票据失败', e)
    }
  }).catch((e: any) => {
    proxy.$modal.msgError(e.message || '加载报表失败')
  }).finally(() => {
    loading.value = false
  })
}

/** iframe加载完成 */
function onLoad(iframe: HTMLIFrameElement) {
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
