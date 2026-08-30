<template>
  <div class="app-container">
    <!-- 页面标题 -->
    <page-header :title="'填报数据详情'" @back="handleBack">
      <template #extra>
        <el-button @click="handleBack">返回</el-button>
      </template>
    </page-header>

    <!-- Loading -->
    <el-skeleton v-if="loading" :rows="8" animated />

    <!-- 详情内容 -->
    <template v-if="!loading">
      <!-- 基本信息 -->
      <el-card shadow="never" class="mb8">
        <template #header>
          <span>基本信息</span>
        </template>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="业务编码" :span="2">{{ detail.dataCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="填报状态">
            <el-tag :type="detail.bizStatus === 'submitted' ? 'success' : 'warning'" disable-transitions>
              {{ detail.bizStatus === 'submitted' ? '已提交' : '草稿' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="模板名称">{{ detail.templateName }}</el-descriptions-item>
          <el-descriptions-item label="模板编码">{{ detail.templateCode }}</el-descriptions-item>
          <el-descriptions-item label="模板版本">{{ detail.templateVersion ? 'v' + detail.templateVersion : '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{ detail.createBy }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ parseTime(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ parseTime(detail.submitTime) || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 填报数据（只读） -->
      <el-card shadow="never" class="sheet-card">
        <template #header>
          <span>填报数据</span>
        </template>
        <CollectSheet
          v-if="detail.formData"
          :key="'detail-' + detail.dataId"
          ref="sheetRef"
          :sheetData="detail.formData"
          :readonly="true"
          :height="600"
        />
        <el-empty v-else description="暂无填报数据" />
      </el-card>
    </template>

    <!-- 数据为空 -->
    <el-empty v-if="!loading && !detail.dataId" description="数据不存在" />
  </div>
</template>

<script setup lang="ts" name="CollectDataDetail">
import { useRouter, useRoute } from 'vue-router'
import { getData } from '@/api/collect/data'
import type { CollectData } from '@/types/api/collect/data'
import CollectSheet from '@/components/CollectSheet/index.vue'

const { proxy } = getCurrentInstance()!
const router = useRouter()
const route = useRoute()

const sheetRef = ref<InstanceType<typeof CollectSheet> | null>(null)
const loading = ref<boolean>(true)

const detail = ref<CollectData>({})

/** 加载数据 */
function loadDetail() {
  const id = route.query.id as string
  if (!id) {
    loading.value = false
    return
  }

  getData(Number(id)).then(response => {
    detail.value = response.data || {}
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

/** 返回 */
function handleBack() {
  router.push({ path: '/collect/data' })
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.sheet-card {
  min-height: 500px;
}
</style>
