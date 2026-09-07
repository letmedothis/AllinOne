<template>
  <div class="app-container" v-loading="loading">
    <el-page-header content="供应商详情" @back="goBack" />
    <el-card v-if="supplier" class="mt16">
      <template #header><span>{{ supplier.number }} · {{ supplier.name }}</span></template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="统一社会信用代码">{{ supplier.taxId }}</el-descriptions-item>
        <el-descriptions-item label="审批状态">{{ supplier.approvalStatus }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ supplier.contact }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ supplier.phone }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ supplier.address }}</el-descriptions-item>
        <el-descriptions-item label="开户行">{{ supplier.bankName }}</el-descriptions-item>
        <el-descriptions-item label="账户名称">{{ supplier.accountName }}</el-descriptions-item>
        <el-descriptions-item label="银行账号">{{ supplier.bankAccount }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ supplier.remark }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts" name="SupplySupplierDetail">
import { getSupplier } from '@/api/supply'
import type { Supplier } from '@/types/api/supply'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const supplier = ref<Supplier>()

async function load() {
  loading.value = true
  try { supplier.value = (await getSupplier(String(route.params.documentId || route.params.id || route.query.documentId || route.query.id))).data } finally { loading.value = false }
}
function goBack() { router.back() }
load()
</script>
