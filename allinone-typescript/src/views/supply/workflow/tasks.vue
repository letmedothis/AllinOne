<template>
  <div class="app-container">
    <el-alert title="待办包含直接指派和角色候选任务；角色任务由首位处理人自动领取。已发起可查看完整节点轨迹。" type="info" show-icon class="mb16" />
    <el-card shadow="never">
      <el-tabs v-model="activeTab" @tab-change="loadActiveTab">
        <el-tab-pane label="我的待办" name="tasks">
          <el-table v-loading="loading" :data="tasks">
            <el-table-column prop="documentNumber" label="订单号" min-width="170" />
            <el-table-column prop="name" label="审批节点" min-width="150" />
            <el-table-column prop="createTime" label="到达时间" width="180" />
            <el-table-column label="操作" width="205"><template #default="scope">
              <el-button link type="primary" @click="openDetail(scope.row.processInstanceId)">详情</el-button>
              <el-button type="success" link @click="openDecision(scope.row, 'APPROVE')">通过</el-button>
              <el-button type="danger" link @click="openDecision(scope.row, 'REJECT')">退回</el-button>
            </template></el-table-column>
          </el-table>
          <el-empty v-if="!loading && tasks.length === 0" description="暂无流程待办" />
        </el-tab-pane>
        <el-tab-pane label="我发起的" name="started">
          <el-table v-loading="loadingStarted" :data="instances">
            <el-table-column prop="documentNumber" label="订单号" min-width="170" />
            <el-table-column prop="processDefinitionName" label="流程" min-width="150" />
            <el-table-column label="版本" width="70"><template #default="scope">V{{ scope.row.processDefinitionVersion }}</template></el-table-column>
            <el-table-column label="状态" width="90"><template #default="scope"><el-tag :type="scope.row.status === 'RUNNING' ? 'warning' : 'success'">{{ scope.row.status === 'RUNNING' ? '审批中' : '已结束' }}</el-tag></template></el-table-column>
            <el-table-column prop="startTime" label="发起时间" width="180" />
            <el-table-column label="操作" width="80"><template #default="scope"><el-button link type="primary" @click="openDetail(scope.row.id)">详情</el-button></template></el-table-column>
          </el-table>
          <el-empty v-if="!loadingStarted && instances.length === 0" description="暂无已发起流程" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
    <el-dialog v-model="visible" :title="action === 'APPROVE' ? '通过采购订单' : '退回采购订单'" width="480px">
      <el-input v-model="comment" type="textarea" :rows="4" maxlength="2000" show-word-limit :placeholder="action === 'APPROVE' ? '审批意见（选填）' : '退回原因（建议填写）'" />
      <template #footer><el-button @click="visible = false">取消</el-button><el-button :type="action === 'APPROVE' ? 'success' : 'danger'" :loading="submitting" @click="submitDecision">确认</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="SupplyWorkflowTasks">
import { completeWorkflowTask, listMyStartedWorkflowInstances, listMyWorkflowTasks } from '@/api/supply'
import type { WorkflowInstanceDetail, WorkflowTask } from '@/types/api/supply'

const router = useRouter()
const { proxy } = getCurrentInstance()!
const activeTab = ref('tasks')
const tasks = ref<WorkflowTask[]>([])
const instances = ref<WorkflowInstanceDetail[]>([])
const loading = ref(false)
const loadingStarted = ref(false)
const visible = ref(false)
const submitting = ref(false)
const activeTask = ref<WorkflowTask>()
const action = ref<'APPROVE' | 'REJECT'>('APPROVE')
const comment = ref('')

async function loadTasks() {
  loading.value = true
  try { const response = await listMyWorkflowTasks(); tasks.value = response.data || [] } finally { loading.value = false }
}

async function loadStarted() {
  loadingStarted.value = true
  try { const response = await listMyStartedWorkflowInstances(); instances.value = response.data || [] } finally { loadingStarted.value = false }
}

function loadActiveTab(name: string | number) { if (name === 'started') loadStarted(); else loadTasks() }
function openDetail(instanceId: string) { router.push({ path: '/supply/workflow/detail', query: { instanceId } }) }
function openDecision(task: WorkflowTask, value: 'APPROVE' | 'REJECT') { activeTask.value = task; action.value = value; comment.value = ''; visible.value = true }

async function submitDecision() {
  if (!activeTask.value) return
  submitting.value = true
  try {
    await completeWorkflowTask(activeTask.value.id, { action: action.value, comment: comment.value })
    proxy.$modal.msgSuccess(action.value === 'APPROVE' ? '已通过' : '已退回')
    visible.value = false
    await loadTasks()
  } finally { submitting.value = false }
}

loadTasks()
</script>
