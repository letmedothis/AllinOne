<template>
  <el-dialog v-model="visible" title="导出任务" width="780px" @open="startPolling" @close="stopPolling">
    <el-table :data="tasks" v-loading="loading" size="small" max-height="420">
      <el-table-column label="任务" prop="taskName" min-width="150" show-overflow-tooltip />
      <el-table-column label="状态" width="90" align="center">
        <template #default="scope">
          <el-tag :type="statusType(scope.row.status)" disable-transitions>{{ statusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="说明" min-width="140" show-overflow-tooltip>
        <template #default="scope">{{ taskNote(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="创建时间" width="165" align="center">
        <template #default="scope"><span>{{ parseTime(scope.row.createTime) }}</span></template>
      </el-table-column>
      <el-table-column label="完成时间" width="165" align="center">
        <template #default="scope"><span>{{ parseTime(scope.row.finishTime) }}</span></template>
      </el-table-column>
      <el-table-column label="操作" width="80" align="center">
        <template #default="scope">
          <el-button
            v-if="scope.row.status === 'success'"
            link
            type="primary"
            @click="handleDownload(scope.row)"
          >下载</el-button>
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts" name="ExportTaskDialog">
import { listExportTask, downloadExportTask } from '@/api/collect/exportTask'
import type { CollectExportTask } from '@/api/collect/exportTask'

const { proxy } = getCurrentInstance()!

const visible = ref(false)
const loading = ref(false)
const tasks = ref<CollectExportTask[]>([])
let pollTimer: number | null = null

function open() {
  visible.value = true
}
defineExpose({ open })

/** 拉取任务列表；存在进行中的任务时 3 秒后自动刷新 */
async function fetchTasks() {
  loading.value = tasks.value.length === 0
  try {
    const res = await listExportTask({ pageNum: 1, pageSize: 20 })
    tasks.value = res.rows
    if (visible.value && res.rows.some(t => t.status === 'pending' || t.status === 'running')) {
      schedulePoll()
    }
  } finally {
    loading.value = false
  }
}

function schedulePoll() {
  if (pollTimer !== null) return
  pollTimer = window.setTimeout(() => {
    pollTimer = null
    fetchTasks()
  }, 3000)
}

function startPolling() {
  fetchTasks()
}

function stopPolling() {
  if (pollTimer !== null) {
    window.clearTimeout(pollTimer)
    pollTimer = null
  }
}

function statusText(status: string) {
  const map: Record<string, string> = { pending: '排队中', running: '导出中', success: '已完成', failed: '失败' }
  return map[status] || status
}

function statusType(status: string) {
  const map: Record<string, string> = { pending: 'warning', running: 'primary', success: 'success', failed: 'danger' }
  return (map[status] || 'info') as any
}

function taskNote(task: CollectExportTask) {
  if (task.errorMsg) return task.errorMsg
  if (task.status === 'success') return task.fileName || ''
  return '-'
}

async function handleDownload(task: CollectExportTask) {
  try {
    await downloadExportTask(task)
  } catch (e: any) {
    proxy.$modal.msgError(e?.message || '下载失败')
  }
}

onUnmounted(stopPolling)
</script>
