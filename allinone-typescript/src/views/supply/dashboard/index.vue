<template><div class="app-container"><el-row :gutter="16"><el-col :span="6" v-for="item in cards" :key="item.label"><el-card><div class="label">{{ item.label }}</div><div class="value">{{ item.value }}</div></el-card></el-col></el-row><el-alert class="mt8" title="金额统计按当前状态和当前用户可见范围计算；审批通过不代表已付款。" type="info" show-icon/></div></template>
<script setup lang="ts" name="SupplyDashboard">
import request from '@/utils/request'
const summary=ref<any>({}); const cards=computed(()=>[{label:'有效订单金额',value:money(summary.value.validOrderCents)},{label:'已通过发票金额',value:money(summary.value.approvedInvoiceCents)},{label:'待审发票金额',value:money(summary.value.pendingInvoiceCents)},{label:'待办任务',value:`${summary.value.pendingTaskCount||0} 条`}])
function money(cents?:number){return `¥${((cents||0)/100).toFixed(2)}`};async function load(){const r=await request({url:'/supply/dashboard',method:'get'});summary.value=r.data||{}};load()
</script>
<style scoped>.label{color:#909399;font-size:14px}.value{margin-top:12px;color:#303133;font-size:26px;font-weight:600}</style>
