<template>
  <div class="app-container">
    <el-alert title="统一展示当前用户的供应商、订单和发票待办；管理员或主管可将缺席人员的任务转交给在职接替人。" type="info" show-icon class="mb8" />
    <el-table v-loading="loading" :data="list">
      <el-table-column label="单据类型" width="130"><template #default="s">{{ typeText(s.row.documentType) }}</template></el-table-column>
      <el-table-column prop="documentNumber" label="单号" min-width="180" /><el-table-column prop="submitterName" label="提交人" width="120" /><el-table-column prop="versionNo" label="版本" width="80" /><el-table-column prop="submittedAt" label="提交时间" width="180" />
      <el-table-column label="操作" width="280"><template #default="s"><el-button type="success" link @click="decide(s.row, true)" v-hasPermi="['supply:approval:approve']">通过</el-button><el-button type="danger" link @click="decide(s.row, false)" v-hasPermi="['supply:approval:reject']">退回</el-button><el-button link @click="openTransfer(s.row)" v-hasPermi="['supply:approval:transfer']">转交</el-button></template></el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="pageNum" v-model:limit="pageSize" @pagination="getList" />
    <el-dialog v-model="dialogVisible" title="审批意见" width="480px"><el-input v-model="comment" type="textarea" :rows="4" placeholder="退回时必填，通过时可选" /><template #footer><el-button type="primary" @click="submitDecision">确认</el-button><el-button @click="dialogVisible = false">取消</el-button></template></el-dialog>
    <el-dialog v-model="transferVisible" title="转交审批任务" width="430px"><el-form label-width="110px"><el-form-item label="接替人用户ID"><el-input v-model="transferUserId" placeholder="请输入在职用户ID" /></el-form-item></el-form><template #footer><el-button @click="transferVisible = false">取消</el-button><el-button type="primary" @click="submitTransfer">确认转交</el-button></template></el-dialog>
  </div>
</template>
<script setup lang="ts" name="SupplyApproval">
import { approve, listPendingApprovals, reject, transferApproval } from '@/api/supply'
import type { ApprovalTask } from '@/types/api/supply'
const { proxy } = getCurrentInstance()!
const list = ref<ApprovalTask[]>([]); const loading = ref(false); const total = ref(0); const pageNum = ref(1); const pageSize = ref(20)
const dialogVisible = ref(false); const comment = ref(''); const current = ref<ApprovalTask | null>(null); const approving = ref(false)
const transferVisible = ref(false); const transferUserId = ref(''); const transferTaskId = ref('')
const typeText = (type?: string) => ({ SUPPLIER: '供应商准入', PURCHASE_ORDER: '采购订单', INVOICE: '发票审批' }[type || ''] || type || '未知单据')
async function getList() { loading.value = true; try { const res = await listPendingApprovals(); list.value = res.rows || []; total.value = res.total || 0 } finally { loading.value = false } }
function decide(task: ApprovalTask, isApprove: boolean) { current.value = task; approving.value = isApprove; comment.value = ''; if (isApprove) submitDecision(); else dialogVisible.value = true }
async function submitDecision() { if (!current.value || (!approving.value && !comment.value.trim())) { proxy.$modal.msgError('退回原因不能为空'); return }; const task = { ...current.value, taskRevision: current.value.revision, documentRevision: current.value.documentRevision, comment: comment.value }; if (approving.value) await approve(task); else await reject(task); proxy.$modal.msgSuccess(approving.value ? '审批通过' : '已退回'); dialogVisible.value = false; getList() }
function openTransfer(task: ApprovalTask) { transferTaskId.value = String(task.taskId); transferUserId.value = ''; transferVisible.value = true }
async function submitTransfer() { if (!transferUserId.value.trim()) return proxy.$modal.msgError('请输入接替人用户ID'); await transferApproval({ taskId: transferTaskId.value, assigneeId: transferUserId.value.trim() }); transferVisible.value = false; proxy.$modal.msgSuccess('审批任务已转交'); getList() }
getList()
</script>
