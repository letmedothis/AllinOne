<template>
  <div class="app-container">
    <!-- 页面标题 -->
    <page-header
      :title="pageTitle"
      :subtitle="'使用Luckysheet在线设计填报模板'"
      @back="handleBack"
    >
      <template #extra>
        <el-space>
          <el-button @click="handleBack">返回</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">保存模板</el-button>
          <el-button
            v-if="!readonly && form.status !== '1'"
            type="success"
            :loading="publishing"
            @click="handlePublish"
          >发布模板</el-button>
        </el-space>
      </template>
    </page-header>

    <!-- 模板基本信息 -->
    <el-card shadow="never" class="info-card" v-if="!readonly">
      <el-form :model="form" label-width="80px" size="small">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="模板名称">
              <el-input v-model="form.templateName" placeholder="请输入模板名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="模板编码">
              <el-input v-model="form.templateCode" placeholder="请输入模板编码" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="所属分类">
              <el-tree-select
                v-model="form.categoryId"
                :data="categoryOptions"
                :props="{ value: 'categoryId', label: 'categoryName', children: 'children' }"
                placeholder="选择分类"
                check-strictly
                clearable
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 状态提示 -->
    <el-alert
      v-if="readonly"
      :title="'当前为只读模式，无法编辑'"
      type="info"
      :closable="false"
      show-icon
      class="mb8"
    />

    <!-- Luckysheet 编辑器 -->
    <el-card shadow="never" class="sheet-card">
      <CollectSheet
        ref="sheetRef"
        :sheetData="form.templateJson"
        :readonly="readonly"
        :height="700"
        @save="handleSheetSave"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts" name="CollectTemplateEdit">
import { useRouter, useRoute } from 'vue-router'
import type { CollectTemplate } from '@/types/api/collect/template'
import { getTemplate, addTemplate, updateTemplate, publishTemplate } from '@/api/collect/template'
import { listCategory } from '@/api/collect/category'
import CollectSheet from '@/components/CollectSheet/index.vue'

const { proxy } = getCurrentInstance()!
const router = useRouter()
const route = useRoute()

const sheetRef = ref<InstanceType<typeof CollectSheet> | null>(null)
const saving = ref<boolean>(false)
const publishing = ref<boolean>(false)
const readonly = ref<boolean>(route.query.readonly === '1')
const categoryOptions = ref<any[]>([])

/** 页面标题 */
const pageTitle = computed(() => {
  if (readonly.value) return '模板预览'
  return form.value.templateId ? '编辑模板' : '新建模板'
})

/** 表单数据 */
const form = ref<CollectTemplate>({
  templateName: undefined,
  templateCode: undefined,
  categoryId: undefined,
  remark: undefined,
  templateJson: undefined,
  status: '0'
})

/** 加载模板信息 */
function loadTemplate() {
  const id = route.query.id as string
  if (!id) return

  getTemplate(Number(id)).then(response => {
    const data = response.data!
    form.value = data
    // 将config配置加载到Luckysheet
    if (data.templateJson && sheetRef.value) {
      sheetRef.value.loadData(data.templateJson)
    }
  })
}

/** 加载分类树 */
function loadCategoryTree() {
  listCategory().then(response => {
    categoryOptions.value = proxy.handleTree(response.data, 'categoryId')
  })
}

/** 保存模板 */
async function handleSave(): Promise<boolean> {
  if (!form.value.templateName) {
    proxy.$modal.msgWarning('请输入模板名称')
    return false
  }
  if (!form.value.templateCode) {
    proxy.$modal.msgWarning('请输入模板编码')
    return false
  }

  saving.value = true

  try {
    // 从Luckysheet获取当前表格数据
    if (sheetRef.value) {
      const sheetData = sheetRef.value.getData()
      form.value.templateJson = typeof sheetData === 'string' ? sheetData : JSON.stringify(sheetData)
    }

    if (form.value.templateId) {
      const response = await updateTemplate(form.value)
      form.value = response.data || form.value
      proxy.$modal.msgSuccess('保存成功')
    } else {
      const res = await addTemplate(form.value)
      form.value = res.data || form.value
      // 更新URL参数
      router.replace({ query: { ...route.query, id: form.value.templateId } })
      proxy.$modal.msgSuccess('创建成功')
    }
    return true
  } catch (e) {
    proxy.$modal.msgError('保存模板失败')
    console.error('保存模板失败:', e)
    return false
  } finally {
    saving.value = false
  }
}

/** 发布模板 */
async function handlePublish() {
  if (!form.value.templateId) {
    proxy.$modal.msgWarning('请先保存模板')
    return
  }

  publishing.value = true
  try {
    // 先保存再发布
    const saved = await handleSave()
    if (!saved) return
    await publishTemplate(form.value.templateId, '1')
    form.value.status = '1'
    proxy.$modal.msgSuccess('发布成功')
  } catch (e) {
    proxy.$modal.msgError('发布模板失败')
    console.error('发布模板失败:', e)
  } finally {
    publishing.value = false
  }
}

/** Luckysheet保存回调 */
function handleSheetSave(data: any) {
  // Luckysheet 自动保存回调
}

/** 返回列表 */
function handleBack() {
  router.push({ path: '/collect/template' })
}

onMounted(() => {
  loadCategoryTree()
  loadTemplate()
})
</script>

<style scoped>
.info-card {
  margin-bottom: 16px;
}

.sheet-card {
  min-height: 600px;
}

:deep(.page-header) {
  margin-bottom: 16px;
}
</style>
