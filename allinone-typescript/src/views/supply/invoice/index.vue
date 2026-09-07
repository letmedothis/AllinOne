<template>
  <div class="app-container">
    <el-card class="mb16"><template #header>发票登记列表</template><el-form :inline="true" :model="query"><el-form-item label="发票号码"><el-input v-model="query.invoiceNumber" clearable /></el-form-item><el-form-item label="状态"><el-select v-model="query.approvalStatus" clearable><el-option label="草稿" value="DRAFT" /><el-option label="审批中" value="IN_REVIEW" /><el-option label="退回" value="RETURNED" /><el-option label="已通过" value="APPROVED" /><el-option label="已作废" value="VOID" /></el-select></el-form-item><el-form-item><el-button type="primary" @click="loadList">查询</el-button></el-form-item></el-form><el-table v-loading="listLoading" :data="invoiceList"><el-table-column prop="number" label="系统单号" min-width="180" /><el-table-column prop="invoiceNumber" label="发票号码" width="150" /><el-table-column prop="sellerTaxId" label="销方税号" width="180" /><el-table-column prop="approvalStatus" label="状态" width="100" /><el-table-column prop="currentNode" label="当前节点" width="100" /></el-table><pagination v-show="total > 0" :total="total" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="loadList" /></el-card>
    <el-alert title="支持 DemoInvoice v1（演示）与数电票蓝字 XML；解析结果必须人工核对并补充订单行关联，不代表税务验真。" type="info" show-icon class="mb8" />
    <el-form :model="form" label-width="110px">
      <el-form-item label="原始票据附件"><el-upload :auto-upload="false" :show-file-list="false" accept=".xml,.ofd,.pdf" :on-change="queueAttachment"><el-button>选择 XML/PDF/OFD</el-button></el-upload><span class="ml8">{{ pendingFiles.length ? `待上传 ${pendingFiles.length} 个` : '提交前至少上传一个附件' }}</span></el-form-item>
      <el-row>
        <el-col :span="8"><el-form-item label="关联订单ID"><el-input v-model="form.orderId" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="发票号码"><el-input v-model="form.invoiceNumber" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="发票类型"><el-input v-model="form.invoiceType" /></el-form-item></el-col>
      </el-row>
      <el-row>
        <el-col :span="8"><el-form-item label="销方税号"><el-input v-model="form.sellerTaxId" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="销方名称"><el-input v-model="form.sellerName" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="开票日期"><el-date-picker v-model="form.issueDate" type="date" value-format="YYYY-MM-DD" /></el-form-item></el-col>
      </el-row>
      <el-row>
        <el-col :span="8"><el-form-item label="购方税号"><el-input v-model="form.buyerTaxId" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="购方名称"><el-input v-model="form.buyerName" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="差异说明"><el-input v-model="form.differenceNote" /></el-form-item></el-col>
      </el-row>
      <el-divider>发票明细（订单行 ID 必填）</el-divider>
      <el-table :data="form.lines" border>
        <el-table-column label="订单行ID" width="160"><template #default="s"><el-input v-model="s.row.orderLineId" /></template></el-table-column>
        <el-table-column label="商品"><template #default="s"><el-input v-model="s.row.name" /></template></el-table-column>
        <el-table-column label="单位" width="100"><template #default="s"><el-input v-model="s.row.unit" /></template></el-table-column>
        <el-table-column label="数量" width="130"><template #default="s"><el-input-number v-model="s.row.quantity" :min="0" :precision="4" /></template></el-table-column>
        <el-table-column label="单价" width="140"><template #default="s"><el-input-number v-model="s.row.price" :min="0" :precision="6" /></template></el-table-column>
        <el-table-column label="税率（比例）" width="120"><template #default="s"><el-input-number v-model="s.row.rate" :min="0" :precision="4" :max="1" :step="0.01" /></template></el-table-column>
      </el-table>
      <el-button class="mt8" @click="form.lines?.push({ name: '', unit: '', quantity: 1, price: 0, rate: 0.13 })">新增明细</el-button>
      <el-button type="primary" class="mt8" @click="save">保存发票草稿</el-button>
      <el-button class="mt8" :disabled="!form.documentId" @click="confirm">确认当前内容</el-button>
      <el-button type="success" class="mt8" :disabled="!form.documentId" @click="submit">提交审批</el-button>
    </el-form>
    <el-card class="mt8"><template #header>XML 解析导入</template><el-input v-model="xml" type="textarea" :rows="10" placeholder="粘贴数电票蓝字 XML（演示可贴 DemoInvoice XML）" /><el-button type="primary" class="mt8" @click="parse">解析并回填</el-button></el-card>
  </div>
</template>

<script setup lang="ts" name="SupplyInvoice">
import { addInvoice, confirmInvoice, listInvoices, parseInvoiceXml, submitInvoice, updateInvoice, uploadDocumentAttachment } from '@/api/supply'
import type { Invoice } from '@/types/api/supply'

const { proxy } = getCurrentInstance()!
const xml = ref('')
const form = ref<Invoice>({ invoiceType: 'DEMO_GENERAL', lines: [] })
const invoiceList = ref<Invoice[]>([])
const listLoading = ref(false)
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, invoiceNumber: undefined as string | undefined, approvalStatus: undefined as string | undefined })
const pendingFiles = ref<File[]>([])

async function loadList() { listLoading.value = true; try { const response = await listInvoices(query); invoiceList.value = response.rows; total.value = response.total } finally { listLoading.value = false } }

async function parse() {
  if (!xml.value.trim()) return proxy.$modal.msgError('XML 内容不能为空')
  const response = await parseInvoiceXml(xml.value)
  const parsed: any = response.data
  form.value = { ...form.value, invoiceNumber: parsed.invoiceNumber, invoiceType: parsed.invoiceType, issueDate: parsed.issueDate, sellerName: parsed.sellerName, sellerTaxId: parsed.sellerTaxId, buyerName: parsed.buyerName, buyerTaxId: parsed.buyerTaxId, amountCents: moneyCents(parsed.amount), taxCents: moneyCents(parsed.taxAmount), totalCents: moneyCents(parsed.totalAmount), lines: (parsed.lines || []).map((line: any) => ({ name: line.name, unit: line.unit, quantity: Number(line.quantity), price: Number(line.unitPrice), rate: Number(line.taxRate) })) }
  proxy.$modal.msgSuccess('解析成功，请补充订单和订单行ID')
}

async function save() {
  if (!form.value.orderId || !form.value.invoiceNumber || !form.value.invoiceType || !form.value.issueDate || !form.value.sellerTaxId || !form.value.sellerName || !form.value.buyerTaxId || !form.value.buyerName || !form.value.lines?.length) return proxy.$modal.msgError('请填写完整票面信息、关联订单和明细')
  if (form.value.documentId) {
    await updateInvoice(form.value)
    form.value.revision = (form.value.revision || 0) + 1
  } else {
    const response = await addInvoice(form.value)
    form.value.documentId = response.data?.documentId
    form.value.revision = response.data?.revision
  }
  if (form.value.documentId && pendingFiles.value.length) {
    await Promise.all(pendingFiles.value.map(file => uploadDocumentAttachment(form.value.documentId!, file, 'INVOICE_ORIGINAL')))
    pendingFiles.value = []
  }
  form.value.manualConfirmed = false
  proxy.$modal.msgSuccess('发票草稿已保存，请确认当前内容后提交')
}

async function confirm() {
  if (!form.value.documentId) return proxy.$modal.msgError('请先保存草稿')
  await confirmInvoice(form.value)
  form.value.manualConfirmed = true
  form.value.revision = (form.value.revision || 0) + 1
  proxy.$modal.msgSuccess('当前发票内容已确认')
}

async function submit() {
  if (!form.value.documentId) return proxy.$modal.msgError('请先保存草稿')
  await submitInvoice({ documentId: form.value.documentId, revision: form.value.revision })
  proxy.$modal.msgSuccess('已提交采购审核')
}

function moneyCents(value: string | undefined) { const amount = Number(value); return Number.isFinite(amount) ? Math.round(amount * 100) : undefined }
function queueAttachment(uploadFile: any) { if (uploadFile.raw) pendingFiles.value.push(uploadFile.raw as File) }

loadList()
</script>
