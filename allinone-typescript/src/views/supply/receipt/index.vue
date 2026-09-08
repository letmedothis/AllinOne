<template>
  <div class="app-container">
    <el-form :inline="true">
      <el-form-item label="待收货订单"><el-select v-model="form.orderId" filterable style="width: 400px" @change="load"><el-option v-for="item in orders" :key="item.id" :value="item.id" :label="item.label" /></el-select></el-form-item>
      <el-button :loading="busy" @click="refreshOptions">刷新待收货</el-button>
    </el-form>
    <el-form v-if="lines.length" :model="form" label-width="100px" :disabled="busy">
      <el-form-item label="入库仓库"><el-select v-model="form.warehouseId" filterable><el-option v-for="item in warehouses" :key="item.id" :value="item.id" :label="item.label" /></el-select></el-form-item>
      <el-form-item label="入库日期"><el-date-picker v-model="form.businessDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" maxlength="500" /></el-form-item>
      <el-table :data="lines" border><el-table-column prop="itemSnapshot" label="商品" /><el-table-column prop="orderQuantity" label="订单数量" /><el-table-column prop="receivedQuantity" label="已入库" /><el-table-column prop="remainingQuantity" label="剩余可入库" /><el-table-column label="本次入库"><template #default="s"><el-input-number v-model="s.row.quantity" :min="0" :precision="4" :max="Number(s.row.remainingQuantity)" /></template></el-table-column></el-table>
      <el-button class="mt8" type="primary" :loading="busy" @click="confirm">核对并确认入库</el-button>
    </el-form>
    <el-divider>收货记录</el-divider>
    <el-button class="mb8" @click="loadHistory">刷新记录</el-button>
    <el-table :data="history" border><el-table-column prop="number" label="入库单号" min-width="190" /><el-table-column prop="orderNumber" label="订单号" min-width="190" /><el-table-column prop="warehouseName" label="仓库" /><el-table-column prop="itemName" label="商品" /><el-table-column prop="quantity" label="数量" /><el-table-column prop="businessDate" label="业务日期" /><el-table-column prop="operatorName" label="确认人" /></el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="loadHistory" />
  </div>
</template>
<script setup lang="ts" name="SupplyReceipt">
import { confirmReceipt, prepareReceipt } from '@/api/supply'
import request from '@/utils/request'
const { proxy } = getCurrentInstance()!
const form = reactive({ orderId: '', warehouseId: '', businessDate: '', remark: '' })
const lines = ref<any[]>([]), orders = ref<any[]>([]), warehouses = ref<any[]>([]), history = ref<any[]>([])
const busy = ref(false), total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 20 })
async function refreshOptions() {
  const [o, w] = await Promise.all([request({ url: '/supply/receipts/order-options' }), request({ url: '/supply/receipts/warehouse-options' })])
  orders.value = o.data || []; warehouses.value = w.data || []
}
async function load() {
  if (!form.orderId) return
  lines.value = []
  const res = await prepareReceipt(form.orderId)
  lines.value = (res.data?.lines || []).map((line: any) => ({ ...line, quantity: 0 }))
}
async function loadHistory() { const r = await request({ url: '/supply/receipts/history', params: query }); history.value = r.rows || []; total.value = r.total || 0 }
async function confirm() {
  if (busy.value) return
  const selected = lines.value.filter(line => Number(line.quantity) > 0).map(line => ({ orderLineId: line.orderLineId, quantity: line.quantity }))
  if (!selected.length || !form.warehouseId || !form.businessDate) return proxy.$modal.msgError('请选择仓库、入库日期并填写本次数量')
  await proxy.$modal.confirm(`本次将确认 ${selected.length} 行收货，请核对仓库、日期和数量。`)
  busy.value = true
  try { await confirmReceipt({ ...form, lines: selected }); proxy.$modal.msgSuccess('入库确认成功'); await load(); await refreshOptions(); await loadHistory() } finally { busy.value = false }
}
refreshOptions(); loadHistory()
</script>