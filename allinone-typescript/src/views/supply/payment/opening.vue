<template>
  <div class="app-container">
    <el-alert title="期初应付用于登记系统上线前已经存在的供应商欠款，不关联采购订单和入库单。保存后上传原始发票或余额依据，再提交财务审批。" type="info" show-icon class="mb16" />
    <el-card class="mb16">
      <template #header><div class="flex-between"><span>我的期初应付记录</span><el-button type="primary" link @click="newDraft">新建登记</el-button></div></template>
      <el-table :data="records" border><el-table-column prop="number" label="单据号" min-width="190" /><el-table-column prop="supplierName" label="供应商" min-width="180" /><el-table-column prop="invoiceNumber" label="原始票号" /><el-table-column prop="balanceCents" label="余额（元）"><template #default="s">{{ ((s.row.balanceCents || 0) / 100).toFixed(2) }}</template></el-table-column><el-table-column prop="approvalStatus" label="状态" /><el-table-column label="操作" width="100"><template #default="s"><el-button link type="primary" @click="openRecord(s.row.documentId)">打开</el-button></template></el-table-column></el-table>
    </el-card>
    <el-card>
      <template #header>期初应付登记</template>
      <el-form :model="form" label-width="120px" :disabled="busy || readonly">
        <el-form-item label="供应商" required><el-select v-model="form.supplierId" filterable style="width: 420px"><el-option v-for="item in suppliers" :key="item.documentId" :value="item.documentId" :label="`${item.name} · ${item.taxId || ''}`" /></el-select></el-form-item>
        <el-row>
          <el-col :span="8"><el-form-item label="原始发票号码" required><el-input v-model="form.invoiceNumber" maxlength="32" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="原票日期" required><el-date-picker v-model="form.issueDate" type="date" value-format="YYYY-MM-DD" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="期初日期" required><el-date-picker v-model="form.openingDate" type="date" value-format="YYYY-MM-DD" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="8"><el-form-item label="未付余额（元）" required><el-input-number v-model="balanceYuan" :min="0.01" :precision="2" :controls="false" style="width: 220px" /></el-form-item></el-col>
          <el-col :span="16"><el-form-item label="来源说明" required><el-input v-model="form.reason" maxlength="500" placeholder="例如：系统上线前已收货未付款" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="余额依据" required><el-upload :auto-upload="false" :show-file-list="false" accept=".xml,.ofd,.pdf,.jpg,.jpeg,.png" :on-change="queueAttachment"><el-button>选择原票或余额依据</el-button></el-upload><span class="ml8">{{ pendingFile ? pendingFile.name : '提交前必须上传一个附件' }}</span></el-form-item>
        <el-form-item><el-button type="primary" :loading="busy" @click="save">保存草稿</el-button><el-button type="success" :loading="busy" :disabled="!form.documentId || readonly" @click="submit">提交财务审批</el-button><span v-if="form.documentId" class="ml8">单据：{{ form.documentId }}，版本：{{ form.revision }}</span></el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts" name="SupplyOpeningPayable">
import { getOpeningPayable, listOpeningPayables, openingSupplierOptions, saveOpeningPayable, submitOpeningPayable, uploadDocumentAttachment } from '@/api/supply'
import type { OpeningPayable, SupplierOption } from '@/types/api/supply'

const { proxy } = getCurrentInstance()!
const busy = ref(false)
const readonly = ref(false)
const suppliers = ref<SupplierOption[]>([])
const records = ref<OpeningPayable[]>([])
const pendingFile = ref<File>()
const balanceYuan = ref<number | undefined>()
const form = reactive<OpeningPayable>({})
function newDraft() { Object.keys(form).forEach(key => delete (form as any)[key]); balanceYuan.value = undefined; pendingFile.value = undefined; readonly.value = false }
async function openRecord(id: string) {
  const response = await getOpeningPayable(id)
  Object.assign(form, response.data || {})
  balanceYuan.value = form.balanceCents ? form.balanceCents / 100 : undefined
  pendingFile.value = undefined
  readonly.value = !['DRAFT', 'RETURNED'].includes((response.data as any)?.approvalStatus || '')
}

async function queueAttachment(uploadFile: any) {
  if (!uploadFile.raw) return
  if (uploadFile.raw.size > 20 * 1024 * 1024) return proxy.$modal.msgError('文件不能超过 20MB')
  pendingFile.value = uploadFile.raw
}
async function save() {
  if (busy.value) return
  if (!form.supplierId || !form.invoiceNumber || !form.issueDate || !form.openingDate || !balanceYuan.value || !form.reason || !pendingFile.value && !form.documentId) return proxy.$modal.msgError('请填写完整信息并选择余额依据')
  busy.value = true
  try {
    const response = await saveOpeningPayable({ ...form, balanceCents: Math.round(balanceYuan.value * 100) })
    Object.assign(form, response.data || {})
    balanceYuan.value = form.balanceCents ? form.balanceCents / 100 : balanceYuan.value
    if (pendingFile.value && form.documentId) { await uploadDocumentAttachment(form.documentId, pendingFile.value, 'INVOICE_ORIGINAL'); pendingFile.value = undefined }
    await loadRecords()
    proxy.$modal.msgSuccess('期初应付草稿已保存')
  } finally { busy.value = false }
}
async function submit() {
  if (busy.value || !form.documentId || !form.revision) return
  if (pendingFile.value) return proxy.$modal.msgError('请先保存并上传余额依据')
  await proxy.$modal.confirm('提交后将进入财务审批，确认提交？')
  busy.value = true
  try { await submitOpeningPayable(form.documentId, form.revision); readonly.value = true; proxy.$modal.msgSuccess('已提交财务审批') } finally { busy.value = false }
}
async function loadRecords() { const response = await listOpeningPayables(); records.value = response.data || [] }
Promise.all([openingSupplierOptions(), loadRecords()]).then(([response]) => { suppliers.value = response.data || [] })
</script>
