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
import ReportFrame from '@/components/ReportFrame/index.vue'

const { proxy } = getCurrentInstance()!
const router = useRouter()
const route = useRoute()

const frameRef = ref<InstanceType<typeof ReportFrame> | null>(null)
const loading = ref<boolean>(true)
const src = ref<string>('')
const reportName = ref<string>('')
const frameHeight = ref<string>('calc(100vh - 280px)')

/** 加载报表配置 */
function loadReport() {
  const id = route.query.id as string
  if (!id) {
    loading.value = false
    return
  }

  getConfig(Number(id)).then(response => {
    const data = response.data
    if (data && data.url && data.status === '0') {
      src.value = data.url
      reportName.value = data.name || ''
      if (data.height) {
        frameHeight.value = data.height + 'px'
      }
    }
    loading.value = false
  }).catch(() => {
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
