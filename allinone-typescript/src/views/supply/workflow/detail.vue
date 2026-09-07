<template>
  <div class="app-container">
    <el-page-header content="流程详情" @back="router.back()" class="mb16" />
    <el-card v-loading="loading" shadow="never">
      <el-descriptions v-if="detail" :column="3" border>
        <el-descriptions-item label="订单号">{{ detail.documentNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="流程版本">{{ detail.processDefinitionName }} V{{ detail.processDefinitionVersion }}</el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag :type="detail.status === 'RUNNING' ? 'warning' : 'success'">{{ detail.status === 'RUNNING' ? '审批中' : '已结束' }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="发起人">{{ detail.startedByName || detail.startedBy || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发起时间">{{ detail.startTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ detail.endTime || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">审批轨迹</el-divider>
      <el-timeline v-if="detail?.timeline?.length">
        <el-timeline-item v-for="item in detail.timeline" :key="item.taskId" :timestamp="item.endTime || item.startTime" placement="top" :type="timelineType(item)">
          <el-card shadow="never" class="timeline-card">
            <div class="timeline-title"><strong>{{ item.nodeName }}</strong><el-tag size="small" :type="actionType(item)">{{ actionText(item) }}</el-tag></div>
            <div class="timeline-meta">处理人：{{ item.assigneeName || (item.status === 'PENDING' ? '候选人待领取' : '-') }}</div>
            <div v-if="item.comment" class="timeline-comment">审批意见：{{ item.comment }}</div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else-if="!loading" description="暂无人工审批轨迹" />
    </el-card>
  </div>
</template>

<script setup lang="ts" name="SupplyWorkflowDetail">
import { getWorkflowInstanceDetail } from '@/api/supply'
import type { WorkflowInstanceDetail, WorkflowTimelineItem } from '@/types/api/supply'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref<WorkflowInstanceDetail>()

function actionText(item: WorkflowTimelineItem) { return item.status === 'PENDING' ? '待处理' : item.action === 'APPROVE' ? '已通过' : item.action === 'REJECT' ? '已退回' : '已完成' }
function actionType(item: WorkflowTimelineItem) { return item.status === 'PENDING' ? 'warning' : item.action === 'REJECT' ? 'danger' : 'success' }
function timelineType(item: WorkflowTimelineItem) { return item.status === 'PENDING' ? 'warning' : item.action === 'REJECT' ? 'danger' : 'success' }

onMounted(async () => {
  const instanceId = String(route.query.instanceId || '')
  if (!instanceId) { router.back(); return }
  loading.value = true
  try { const response = await getWorkflowInstanceDetail(instanceId); detail.value = response.data } finally { loading.value = false }
})
</script>

<style scoped>
.timeline-card { max-width:760px; }.timeline-title { display:flex;align-items:center;justify-content:space-between;gap:12px; }
.timeline-meta { margin-top:10px;color:var(--el-text-color-secondary); }.timeline-comment { margin-top:8px;white-space:pre-wrap; }
</style>
