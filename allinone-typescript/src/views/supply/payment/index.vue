<template>
  <div class="app-container">
    <el-card class="mb8"><template #header>付款单列表</template>
      <el-form :inline="true" :model="query">
        <el-form-item label="单号"><el-input v-model="query.number" clearable placeholder="PAY-…" style="width: 200px" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable style="width: 140px">
            <el-option label="草稿" value="DRAFT" /><el-option label="审批中" value="IN_REVIEW" />
            <el-option label="退回" value="RETURNED" /><el-option label="待付款" value="READY_TO_PAY" /><el-option label="已支付" value="PAID" /><el-option label="历史已付款" value="APPROVED" /><el-option label="作废" value="VOID" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="getList">查询</el-button></el-form-item>
        <el-form-item><el-button type="primary" plain @click="openAp" v-hasPermi="['supply:payment:query']">应付查询</el-button></el-form-item>
      </el-form>
      <el-button type="primary" plain class="mb8" @click="openAdd" v-hasPermi="['supply:payment:add']">新增付款单</el-button>
      <el-table v-loading="loading" :data="list">
        <el-table-column prop="number" label="付款单号" min-width="180" />
        <el-table-column prop="supplierName" label="供应商" min-width="160" />
        <el-table-column label="金额(元)" width="130"><template #default="s">{{ yuan(s.row.amountCents) }}</template></el-table-column>
        <el-table-column label="状态" width="120"><template #default="s">{{ statusText(s.row.status) }}</template></el-table-column>
        <el-table-column label="当前节点" width="120"><template #default="s">{{ s.row.currentNode === 'FINANCE_DIRECTOR' ? '总监终审' : s.row.currentNode === 'FINANCE_REVIEW' ? '财务复核' : '—' }}</template></el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="s">
            <el-button link type="primary" @click="openDetail(s.row)">详情</el-button>
            <el-button link type="success" v-if="s.row.status === 'READY_TO_PAY'" v-hasPermi="['supply:payment:pay']" @click="openExecution(s.row)">记录支付</el-button>
            <el-button link type="primary" @click="openEdit(s.row)" v-if="['DRAFT', 'RETURNED'].includes(s.row.status)" v-hasPermi="['supply:payment:edit']">编辑</el-button>
            <el-button link @click="handleSubmit(s.row)" v-if="['DRAFT', 'RETURNED'].includes(s.row.status)" v-hasPermi="['supply:payment:edit']">提交</el-button>
            <el-button link type="warning" @click="openDecision(s.row)" v-if="s.row.status === 'IN_REVIEW'" v-hasPermi="['supply:payment:approve']">{{ s.row.currentNode === 'FINANCE_DIRECTOR' ? '总监终审' : '财务复核' }}</el-button>
            <el-button link type="danger" @click="handleVoid(s.row)" v-if="['DRAFT', 'RETURNED', 'IN_REVIEW'].includes(s.row.status)" v-hasPermi="['supply:payment:edit']">作废</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="getList" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑付款单' : '新增付款单'" width="860px">
      <el-form ref="formRef" :model="form" label-width="110px">
        <el-form-item label="收款供应商" prop="supplierId"><el-select v-model="form.supplierId" filterable style="width: 100%" @change="loadAp">
          <el-option v-for="s in supplierOptions" :key="s.documentId" :label="`${s.name}（${s.taxId}）`" :value="s.documentId" /></el-select></el-form-item>
        <el-form-item label="申请说明"><el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" /></el-form-item>
        <el-divider>核销明细（金额单位：元）</el-divider>
        <el-table :data="form.lines" border>
          <el-table-column label="发票" width="300"><template #default="s"><el-select v-model="s.row.invoiceId" filterable>
            <el-option v-for="a in apRows" :key="a.invoiceId" :label="`${a.invoiceNumber} 剩余¥${yuan(a.balanceCents)}`" :value="a.invoiceId" /></el-select></template></el-table-column>
          <el-table-column label="核销金额(元)" width="180"><template #default="s"><el-input-number v-model="s.row.allocatedYuan" :min="0.01" :precision="2" style="width: 150px" /></template></el-table-column>
          <el-table-column label="操作" width="80"><template #default="s"><el-button link type="danger" @click="form.lines?.splice(s.$index, 1)">删</el-button></template></el-table-column>
        </el-table>
        <el-button class="mt8" @click="addLine">新增核销行</el-button>
        <div class="mt8">合计：<b>¥{{ totalYuan }}</b></div>
      </el-form>
      <template #footer><el-button type="primary" :loading="saving" @click="save">保存</el-button><el-button @click="dialogVisible = false">取消</el-button></template>
    </el-dialog>

    <el-dialog v-model="decisionVisible" :title="decisionTitle" width="780px">
      <el-descriptions :column="2" border v-if="decisionTarget">
        <el-descriptions-item label="付款单">{{ decisionTarget.number }}</el-descriptions-item>
        <el-descriptions-item label="收款供应商">{{ decisionTarget.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="金额">¥{{ yuan(decisionTarget.amountCents) }}</el-descriptions-item>
        <el-descriptions-item label="申请说明">{{ decisionTarget.remark || '—' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="decisionTarget?.lines || []" class="mb8">
        <el-table-column prop="invoiceNumber" label="核销发票" />
        <el-table-column label="本次核销金额"><template #default="s">¥{{ yuan(s.row.allocatedCents) }}</template></el-table-column>
      </el-table>
      <el-form label-width="90px">
        <el-form-item label="意见"><el-input v-model="decision.comment" type="textarea" :rows="3" maxlength="500" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button type="danger" :loading="saving" @click="decide(false)">退回</el-button>
        <el-button type="success" :loading="saving" @click="decide(true)">通过</el-button>
        <el-button @click="decisionVisible = false">取消</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="apVisible" title="应付与可申请余额（按开票日期排序）" width="960px">
      <el-form :inline="true"><el-form-item label="供应商"><el-select v-model="apFilter.supplierId" filterable clearable style="width: 260px" @change="loadApRows">
        <el-option v-for="s in supplierOptions" :key="s.documentId" :label="s.name" :value="s.documentId" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="loadApRows">查询</el-button></el-form-item></el-form>
      <el-table :data="apRows" border>
        <el-table-column prop="invoiceNumber" label="发票号码" width="170" />
        <el-table-column prop="issueDate" label="开票日期" width="110" />
        <el-table-column label="票面价税合计" width="120"><template #default="s">{{ yuan(s.row.totalCents) }}</template></el-table-column>
        <el-table-column label="已支付核销" width="120"><template #default="s">{{ yuan(s.row.paidCents) }}</template></el-table-column>
        <el-table-column label="在途占用" width="120"><template #default="s">{{ yuan(s.row.reservedCents) }}</template></el-table-column>
        <el-table-column label="实际未付" width="120"><template #default="s">{{ yuan(s.row.unpaidCents) }}</template></el-table-column>
        <el-table-column label="可继续申请" width="120"><template #default="s">{{ yuan(s.row.balanceCents) }}</template></el-table-column>
      </el-table>
    </el-dialog>
    <el-dialog v-model="detailVisible" title="付款详情与操作记录" width="860px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="付款单">{{ detail.number }}</el-descriptions-item><el-descriptions-item label="供应商">{{ detail.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="金额">¥{{ yuan(detail.amountCents) }}</el-descriptions-item><el-descriptions-item label="状态">{{ statusText(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="说明" :span="2">{{ detail.remark || '—' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail?.lines || []"><el-table-column prop="invoiceNumber" label="核销发票" /><el-table-column label="核销金额"><template #default="s">¥{{ yuan(s.row.allocatedCents) }}</template></el-table-column></el-table>
      <el-divider>逐次操作记录</el-divider>
      <el-timeline><el-timeline-item v-for="event in detail?.events || []" :key="event.id" :timestamp="event.createdAt">
        {{ event.actorName }} · {{ actionText(event.action) }}<p>{{ event.comment }}</p>
        <div v-if="event.action === 'PAID'">{{ executionText(event.snapshot) }}</div>
      </el-timeline-item></el-timeline>
    </el-dialog>
    <el-dialog v-model="executionVisible" title="记录已完成的线下支付" width="560px">
      <el-alert title="请在实际支付成功后填写；保存后核销生效。" type="info" :closable="false" class="mb8" />
      <el-form label-width="110px">
        <el-form-item label="支付日期"><el-date-picker v-model="execution.paidAt" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="付款账户"><el-input v-model="execution.payerAccount" maxlength="120" /></el-form-item>
        <el-form-item label="支付凭据编号"><el-input v-model="execution.reference" maxlength="120" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="executionVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveExecution">确认已支付</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="SupplyPayment">
import { addPayment, getPayment, listApBalance, listPaymentSupplierOptions, listPayments, reviewPayment, directorDecisionPayment, submitPayment, updatePayment, voidPayment } from '@/api/supply'
import type { ApBalanceRow, Payment, SupplierOption } from '@/types/api/supply'
import { recordPaymentExecution } from '@/api/supply'

const { proxy } = getCurrentInstance()!
const list = ref<Payment[]>([])
const supplierOptions = ref<SupplierOption[]>([])
const apRows = ref<ApBalanceRow[]>([])
const loading = ref(false)
const total = ref(0)
const dialogVisible = ref(false)
const decisionVisible = ref(false)
const apVisible = ref(false)
const saving = ref(false)
const detailVisible = ref(false)
const detail = ref<Payment>()
const executionVisible = ref(false)
const execution = reactive({ revision: 0, paidAt: '', payerAccount: '', reference: '' })
const executionId = ref('')
const statusText = (status?: string) => ({ DRAFT: '草稿', RETURNED: '退回', IN_REVIEW: '审批中', READY_TO_PAY: '待付款', PAID: '已支付', APPROVED: '历史已付款', VOID: '作废' }[status || ''] || status)
const actionText = (action: string) => ({ CREATE: '创建', UPDATE: '修改', SUBMIT: '提交', REVIEW_APPROVE: '复核通过', REVIEW_RETURN: '复核退回', DIRECTOR_APPROVE: '终审通过', DIRECTOR_RETURN: '终审退回', VOID: '作废', PAID: '记录实际付款' }[action] || action)
function executionText(snapshot: string) { try { const value = JSON.parse(snapshot); return `支付日期：${value.paidAt}；付款账户：${value.payerAccount}；凭据：${value.reference}` } catch { return '支付凭据读取失败' } }
async function openDetail(row: Payment) { detail.value = (await getPayment(row.id!)).data; detailVisible.value = true }
async function openExecution(row: Payment) { const latest = (await getPayment(row.id!)).data; executionId.value = latest.id!; Object.assign(execution, { revision: latest.revision, paidAt: '', payerAccount: '', reference: '' }); executionVisible.value = true }
async function saveExecution() {
  if (!execution.paidAt || !execution.payerAccount.trim() || !execution.reference.trim()) return proxy.$modal.msgError('请填写完整支付凭据')
  if (saving.value) return
  saving.value = true
  try { await recordPaymentExecution(executionId.value, execution); executionVisible.value = false; proxy.$modal.msgSuccess('已记录支付并核销'); await getList() } finally { saving.value = false }
}
const formRef = ref()
const form = ref<Payment>({ lines: [] })
const decision = ref<{ approved: boolean; comment: string }>({ approved: false, comment: '' })
const decisionTarget = ref<Payment>()
const apFilter = ref<{ supplierId?: string }>({})

const query = reactive({ pageNum: 1, pageSize: 20, number: undefined as string | undefined, status: undefined as string | undefined })

const yuan = (cents?: number | null) => (cents ? (cents / 100).toFixed(2) : '0.00')
const totalYuan = computed(() => ((form.value.lines || []).reduce((sum: number, line: any) => sum + Number(line.allocatedYuan || 0), 0)).toFixed(2))
const decisionTitle = computed(() => decisionTarget.value?.currentNode === 'FINANCE_DIRECTOR' ? '财务总监终审' : '财务复核')

async function getList() {
  loading.value = true
  try { const r = await listPayments(query); list.value = r.rows; total.value = r.total } finally { loading.value = false }
}
async function openAdd() {
  form.value = { lines: [] }
  supplierOptions.value = (await listPaymentSupplierOptions()).data || []
  dialogVisible.value = true
}
async function openEdit(row: Payment) {
  supplierOptions.value = (await listPaymentSupplierOptions()).data || []
  const detail = (await getPayment(row.id!)).data
  form.value = { ...detail }
  form.value.lines = (detail.lines || []).map((line) => ({ ...line, allocatedYuan: line.allocatedCents ? line.allocatedCents / 100 : 0 }))
  await loadAp(detail.supplierId)
  dialogVisible.value = true
}
async function loadAp(supplierId?: string) {
  apRows.value = supplierId ? (await listApBalance(supplierId)).data || [] : []
}
async function loadApRows() { apRows.value = (await listApBalance(apFilter.value.supplierId)).data || [] }
async function openAp() { supplierOptions.value = (await listPaymentSupplierOptions()).data || []; apVisible.value = true; await loadApRows() }
function addLine() { (form.value.lines ||= []).push({ invoiceId: undefined as any, allocatedYuan: 0 } as any) }
function buildPayload(): Payment {
  const payload: Payment = { ...form.value }
  payload.amountCents = Math.round(Number(totalYuan.value) * 100)
  payload.lines = (form.value.lines || []).filter((l: any) => l.invoiceId).map((l: any) => ({ invoiceId: l.invoiceId, allocatedCents: Math.round(Number(l.allocatedYuan || 0) * 100) }))
  return payload
}
async function save() {
  if (saving.value) return
  await formRef.value.validate().catch(() => Promise.reject())
  const payload = buildPayload()
  if (!payload.lines?.length) return proxy.$modal.msgError('请至少填写一行核销明细')
  saving.value = true
  try {
    if (payload.id) await updatePayment(payload); else { const r = await addPayment(payload); payload.id = r.data?.id }
    proxy.$modal.msgSuccess('保存成功'); dialogVisible.value = false; await getList()
  } finally { saving.value = false }
}
function handleSubmit(row: Payment) {
  proxy.$modal.confirm(`确认提交付款单“${row.number}”（金额 ¥${yuan(row.amountCents)}）吗？提交后将占用发票核销额度。`).then(() => submitPayment(row.id!))
    .then(() => { proxy.$modal.msgSuccess('已提交'); getList() }).catch(() => {})
}
async function openDecision(row: Payment) { decisionTarget.value = (await getPayment(row.id!)).data; decision.value = { approved: false, comment: '' }; decisionVisible.value = true }
function decide(approved: boolean) {
  const target = decisionTarget.value
  if (!target || saving.value) return
  if (!approved && !decision.value.comment.trim()) return proxy.$modal.msgError('退回原因不能为空')
  saving.value = true
  const call = target.currentNode === 'FINANCE_DIRECTOR' ? directorDecisionPayment : reviewPayment
  call(target.id!, { approved, comment: decision.value.comment, revision: target.revision! })
    .then(() => { proxy.$modal.msgSuccess(approved ? '已通过' : '已退回'); decisionVisible.value = false; getList() })
    .catch(() => {}).finally(() => { saving.value = false })
}
function handleVoid(row: Payment) {
  proxy.$prompt('请输入作废原因', '作废付款单', { inputPlaceholder: '原因必填', inputValidator: (v: string) => (v && v.trim() ? true : '原因不能为空') } as any)
    .then(({ value }: any) => voidPayment(row.id!, value)).then(() => { proxy.$modal.msgSuccess('已作废'); getList() }).catch(() => {})
}
getList()
</script>
