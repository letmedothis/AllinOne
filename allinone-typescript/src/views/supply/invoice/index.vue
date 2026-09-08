<template>
  <div class="app-container">
    <el-card class="mb16"><template #header>发票登记列表</template><el-form :inline="true" :model="query"><el-form-item label="发票号码"><el-input v-model="query.invoiceNumber" clearable /></el-form-item><el-form-item label="状态"><el-select v-model="query.approvalStatus" clearable><el-option label="草稿" value="DRAFT" /><el-option label="审批中" value="IN_REVIEW" /><el-option label="退回" value="RETURNED" /><el-option label="已通过" value="APPROVED" /><el-option label="已作废" value="VOID" /></el-select></el-form-item><el-form-item><el-button type="primary" @click="loadList">查询</el-button></el-form-item></el-form><el-table v-loading="listLoading" :data="invoiceList"><el-table-column prop="number" label="系统单号" min-width="180" /><el-table-column prop="invoiceNumber" label="发票号码" width="150" /><el-table-column prop="sellerTaxId" label="销方税号" width="180" /><el-table-column prop="approvalStatus" label="状态" width="100" /><el-table-column prop="currentNode" label="当前节点" width="100" /><el-table-column label="操作" width="150"><template #default="s"><el-button link type="primary" @click="openInvoice(s.row)">{{ ["DRAFT", "RETURNED"].includes(s.row.approvalStatus) ? "打开 / 继续办理" : "查看" }}</el-button></template></el-table-column></el-table><pagination v-show="total > 0" :total="total" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="loadList" /></el-card>
    <el-alert title="支持 DemoInvoice v1（演示）与数电票蓝字 XML；解析结果必须人工核对并补充订单行关联，不代表税务验真。" type="info" show-icon class="mb8" />
    <el-button class="mb8" type="primary" v-hasPermi="['supply:invoice:add']" @click="newInvoice">新建下一张发票</el-button><el-alert v-if="form.documentId" :title="`当前单据：${form.number || form.invoiceNumber}；状态：${form.approvalStatus}`" :closable="false" class="mb8" /><el-form :model="form" label-width="110px" :disabled="readonly || busy">
      <el-form-item label="原始票据附件"><el-upload :auto-upload="false" :show-file-list="false" accept=".xml,.ofd,.pdf" :on-change="queueAttachment"><el-button>选择 XML/PDF/OFD</el-button></el-upload><span class="ml8">{{ pendingFiles.length ? `待上传 ${pendingFiles.length} 个` : '提交前至少上传一个附件' }}</span></el-form-item>
      <el-row>
        <el-col :span="8"><el-form-item label="关联订单"><el-select v-model="form.orderId" filterable :disabled="!!form.documentId" @change="loadOrder"><el-option v-for="order in orders" :key="order.documentId" :value="order.documentId" :label="`${order.number} · ${order.supplierName}`" /></el-select></el-form-item></el-col>
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
      <el-divider>发票明细（选择对应订单商品）</el-divider>
      <el-table :data="form.lines" border>
        <el-table-column label="订单商品" width="230"><template #default="s"><el-select v-model="s.row.orderLineId" filterable><el-option v-for="line in orderLines" :key="line.id" :value="line.id" :label="`${line.name} · ${line.unit} · 数量 ${line.quantity}`" /></el-select></template></el-table-column>
        <el-table-column label="商品"><template #default="s"><el-input v-model="s.row.name" /></template></el-table-column>
        <el-table-column label="单位" width="100"><template #default="s"><el-input v-model="s.row.unit" /></template></el-table-column>
        <el-table-column label="数量" width="130"><template #default="s"><el-input-number v-model="s.row.quantity" :min="0" :precision="4" /></template></el-table-column>
        <el-table-column label="单价" width="140"><template #default="s"><el-input-number v-model="s.row.price" :min="0" :precision="6" /></template></el-table-column>
        <el-table-column label="税率（比例）" width="120"><template #default="s"><el-input-number v-model="s.row.rate" :min="0" :precision="4" :max="1" :step="0.01" /></template></el-table-column>
        <el-table-column label="操作" width="70"><template #default="s"><el-button link type="danger" @click="form.lines?.splice(s.$index, 1)">删除</el-button></template></el-table-column>
      </el-table>
      <el-button class="mt8" @click="form.lines?.push({ name: '', unit: '', quantity: 1, price: 0, rate: 0.13 })">新增明细</el-button>
      <el-button type="primary" class="mt8" @click="save">保存发票草稿</el-button>
      <el-button class="mt8" :disabled="!form.documentId || readonly" @click="confirm">确认当前内容</el-button>
      <el-button type="success" class="mt8" :disabled="!form.documentId" @click="submit">提交审批</el-button>
    </el-form>
    <el-card v-if="!readonly" class="mt8"><template #header>XML 解析导入（上传 XML 时自动解析，也可粘贴）</template><el-input v-model="xml" type="textarea" :rows="10" placeholder="粘贴数电票蓝字 XML（演示可贴 DemoInvoice XML）" /><el-button type="primary" class="mt8" @click="parse">解析并回填</el-button></el-card>
  </div>
</template>

<script setup lang="ts" name="SupplyInvoice">
import { getInvoice, invoiceOrderOptions, invoiceOrder, addInvoice, confirmInvoice, listInvoices, parseInvoiceXml, submitInvoice, updateInvoice, uploadDocumentAttachment } from '@/api/supply'
import type { Invoice, PurchaseOrder, PurchaseOrderLine } from '@/types/api/supply'

const { proxy } = getCurrentInstance()!
const xml = ref('')
const form = ref<Invoice>({ invoiceType: 'DEMO_GENERAL', lines: [] })
const invoiceList = ref<Invoice[]>([])
const listLoading = ref(false)
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, invoiceNumber: undefined as string | undefined, approvalStatus: undefined as string | undefined })
const pendingFiles = ref<File[]>([])
const orders = ref<PurchaseOrder[]>([])
const orderLines = ref<PurchaseOrderLine[]>([])
const busy = ref(false)
const savedContent = ref('')
const readonly = computed(() => !!form.value.documentId && !['DRAFT', 'RETURNED'].includes(form.value.approvalStatus || ''))
function content() { const { revision, manualConfirmed, ...fields } = form.value; return JSON.stringify(fields) }
async function loadOrder() { orderLines.value = form.value.orderId ? (await invoiceOrder(form.value.orderId)).data.lines || [] : [] }
async function openInvoice(row: Invoice) {
  if (busy.value) return
  form.value = (await getInvoice(row.documentId!)).data
  pendingFiles.value = []
  if (!readonly.value) { orders.value = (await invoiceOrderOptions()).data || []; await loadOrder() }
  savedContent.value = content()
}
async function newInvoice() {
  if (busy.value) return
  if (form.value.lines?.length && !readonly.value && savedContent.value !== content()) await proxy.$modal.confirm('当前有未保存内容，确认开始新发票？')
  form.value = { invoiceType: 'DIGITAL_GENERAL', lines: [] }; xml.value = ''; pendingFiles.value = []; orderLines.value = []; savedContent.value = ''
  orders.value = (await invoiceOrderOptions()).data || []
}

async function loadList() { listLoading.value = true; try { const response = await listInvoices(query); invoiceList.value = response.rows; total.value = response.total } finally { listLoading.value = false } }

async function parse() {
  if (!xml.value.trim()) return proxy.$modal.msgError('XML 内容不能为空')
  const response = await parseInvoiceXml(xml.value)
  const parsed: any = response.data
  form.value = { ...form.value, invoiceNumber: parsed.invoiceNumber, invoiceType: parsed.invoiceType, issueDate: parsed.issueDate, sellerName: parsed.sellerName, sellerTaxId: parsed.sellerTaxId, buyerName: parsed.buyerName, buyerTaxId: parsed.buyerTaxId, amountCents: moneyCents(parsed.amount), taxCents: moneyCents(parsed.taxAmount), totalCents: moneyCents(parsed.totalAmount), lines: (parsed.lines || []).map((line: any) => ({ name: line.name, unit: line.unit, quantity: Number(line.quantity), price: Number(line.unitPrice), rate: Number(line.taxRate) })) }
  proxy.$modal.msgSuccess('解析成功，请补充订单和订单行ID')
}

async function save() {
  if (busy.value || readonly.value) return
  if (!form.value.orderId || !form.value.invoiceNumber || !form.value.invoiceType || !form.value.issueDate || !form.value.sellerTaxId || !form.value.sellerName || !form.value.buyerTaxId || !form.value.buyerName || !form.value.lines?.length) return proxy.$modal.msgError('请填写完整票面信息、关联订单和明细')
  busy.value = true
  try {
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
  form.value = (await getInvoice(form.value.documentId!)).data
  savedContent.value = content()
  await loadList()
  proxy.$modal.msgSuccess('发票草稿已保存，请确认当前内容后提交')
  } finally { busy.value = false }
}

async function confirm() {
  if (busy.value || readonly.value) return
  if (savedContent.value !== content() || pendingFiles.value.length) return proxy.$modal.msgError('内容有修改或附件未上传，请先保存再确认')
  if (!form.value.documentId) return proxy.$modal.msgError('请先保存草稿')
  await confirmInvoice(form.value)
  form.value.manualConfirmed = true
  form.value.revision = (form.value.revision || 0) + 1
  proxy.$modal.msgSuccess('当前发票内容已确认')
}

async function submit() {
  if (busy.value || readonly.value) return
  if (savedContent.value !== content() || pendingFiles.value.length) return proxy.$modal.msgError('请先保存并确认当前内容')
  if (!form.value.documentId) return proxy.$modal.msgError('请先保存草稿')
  await submitInvoice({ documentId: form.value.documentId, revision: form.value.revision })
  form.value = (await getInvoice(form.value.documentId)).data
  await loadList()
  proxy.$modal.msgSuccess('已提交采购审核')
}

function moneyCents(value: string | undefined) { const amount = Number(value); return Number.isFinite(amount) ? Math.round(amount * 100) : undefined }
async function queueAttachment(uploadFile: any) {
  if (!uploadFile.raw) return
  const file = uploadFile.raw as File
  if (file.size > 20 * 1024 * 1024) return proxy.$modal.msgError('文件不能超过 20MB')
  pendingFiles.value.push(file)
  if (/\.xml$/i.test(file.name)) { xml.value = await file.text(); await parse() }
}

loadList()
</script>
