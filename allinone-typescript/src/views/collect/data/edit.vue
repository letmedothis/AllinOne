<template>
  <div class="app-container">
    <!-- 页面标题 -->
    <page-header :title="pageTitle" @back="handleBack">
      <template #extra>
        <el-space>
          <el-button @click="handleBack">返回</el-button>
          <el-button v-if="submitStatus !== 'submitted'" type="primary" :loading="saving" @click="handleSaveDraft">
            保存草稿
          </el-button>
          <el-button
            v-if="submitStatus !== 'submitted'"
            type="success"
            :loading="submitting"
            @click="handleSubmit"
          >提交填报</el-button>
        </el-space>
      </template>
    </page-header>

    <!-- 选择模板（新建时显示） -->
    <el-card v-if="!templateId" shadow="never" class="mb8">
      <template #header>
        <span>选择填报模板</span>
      </template>
      <el-form :inline="true">
        <el-form-item label="模板">
          <el-select v-model="selectedTemplateId" placeholder="请选择模板" style="width: 300px" @change="onTemplateChange">
            <el-option
              v-for="item in publishedTemplates"
              :key="item.templateId"
              :label="item.templateName"
              :value="item.templateId"
            >
              <span>{{ item.templateName }}</span>
              <span class="template-code">[{{ item.templateCode }}]</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item v-if="selectedTemplate">
          <el-tag>分类ID：{{ selectedTemplate.categoryId || '-' }}</el-tag>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 已选模板信息 -->
    <el-card v-if="templateId" shadow="never" class="mb8">
      <el-descriptions :column="4" size="small">
        <el-descriptions-item label="模板名称">{{ templateInfo.templateName }}</el-descriptions-item>
        <el-descriptions-item label="模板编码">{{ templateInfo.templateCode }}</el-descriptions-item>
        <el-descriptions-item label="版本号">v{{ templateInfo.version }}</el-descriptions-item>
        <el-descriptions-item label="填报状态">
          <el-tag :type="submitStatus === 'submitted' ? 'success' : 'warning'" disable-transitions>
            {{ submitStatus === 'submitted' ? '已提交' : '草稿' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 业务编码 -->
    <el-card v-if="templateId" shadow="never" class="mb8">
      <el-form :inline="true">
        <el-form-item label="业务编码" prop="dataCode">
          <el-input v-model="form.dataCode" placeholder="可选，用于关联业务数据" style="width: 400px" maxlength="64" />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 提交状态提示 -->
    <el-alert
      v-if="submitStatus === 'submitted'"
      title="该数据已提交，无法编辑。如需修改请先联系管理员。"
      type="success"
      :closable="false"
      show-icon
      class="mb8"
    />

    <!-- Luckysheet 填报区域 -->
    <el-card v-if="templateId" shadow="never" class="sheet-card">
      <CollectSheet
        v-if="sheetKey"
        :key="sheetKey"
        ref="sheetRef"
        :sheetData="form.formData"
        :readonly="submitStatus === 'submitted'"
        :height="700"
        @change="onSheetChange"
        @save="onSheetSave"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts" name="CollectDataEdit">
import { useRouter, useRoute } from 'vue-router'
import { getTemplate, listTemplate } from '@/api/collect/template'
import { getData, addData, updateData, submitData } from '@/api/collect/data'
import type { CollectTemplate } from '@/types/api/collect/template'
import type { CollectData } from '@/types/api/collect/data'
import CollectSheet from '@/components/CollectSheet/index.vue'

const { proxy } = getCurrentInstance()!
const router = useRouter()
const route = useRoute()

const sheetRef = ref<InstanceType<typeof CollectSheet> | null>(null)
const saving = ref<boolean>(false)
const submitting = ref<boolean>(false)
const sheetKey = ref<number>(0)

/** 是否为只读查看模式（从数据列表查看进入） */
const isEdit = computed(() => !!route.query.id)

/** 模板ID */
const templateId = ref<number | undefined>(undefined)

/** 已发布的模板列表 */
const publishedTemplates = ref<CollectTemplate[]>([])
const selectedTemplateId = ref<number | undefined>(undefined)

/** 当前选中的模板信息 */
const selectedTemplate = computed(() => {
  return publishedTemplates.value.find(t => t.templateId === selectedTemplateId.value)
})

/** 模板信息（编辑时） */
const templateInfo = ref<CollectTemplate>({})

/** 提交状态 */
const submitStatus = ref<'draft' | 'submitted'>('draft')

/** 页面标题 */
const pageTitle = computed(() => {
  if (isEdit.value) return '编辑填报数据'
  return '新增填报'
})

/** 表单数据 */
const form = ref<CollectData>({
  templateId: undefined,
  formData: undefined,
  bizStatus: 'draft',
  version: 1
})

/** 加载已发布的模板列表 */
function loadPublishedTemplates() {
  listTemplate({ pageNum: 1, pageSize: 100, status: '1' }).then(response => {
    publishedTemplates.value = response.rows
  })
}

/** 模板切换 */
async function onTemplateChange(id: number) {
  templateId.value = id
  form.value.templateId = id
  const response = await getTemplate(id)
  const tmpl = response.data
  if (tmpl) templateInfo.value = tmpl
  if (tmpl?.templateJson) {
    form.value.formData = tmpl.templateJson
  }
  // 刷新sheet
  sheetKey.value++
}

/** 加载已有数据 */
function loadData() {
  const id = route.query.id as string
  if (!id) return

  getData(Number(id)).then(response => {
    const data = response.data!
    form.value = data
    templateId.value = data.templateId
    submitStatus.value = data.bizStatus || 'draft'
    sheetKey.value++

    // 加载模板信息
    if (data.templateId) {
      listTemplate({ pageNum: 1, pageSize: 100 }).then(res => {
        const tmpl = res.rows.find(t => t.templateId === data.templateId)
        if (tmpl) {
          templateInfo.value = tmpl
        }
      })
    }
  })
}

/** 表格数据变更 */
function onSheetChange(data: any) {
  form.value.formData = typeof data === 'string' ? data : JSON.stringify(data)
}

/** 表格保存事件 */
function onSheetSave(data: any) {
  form.value.formData = typeof data === 'string' ? data : JSON.stringify(data)
}

/** 保存草稿 */
async function handleSaveDraft() {
  if (!templateId.value) {
    proxy.$modal.msgWarning('请选择模板')
    return
  }
  saving.value = true
  try {
    // 从Luckysheet获取数据
    if (sheetRef.value) {
      const sheetData = sheetRef.value.getData()
      form.value.formData = typeof sheetData === 'string' ? sheetData : JSON.stringify(sheetData)
    }
    form.value.bizStatus = 'draft'

    if (form.value.dataId) {
      const response = await updateData(form.value)
      form.value = response.data || form.value
      proxy.$modal.msgSuccess('草稿已更新')
    } else {
      const res: any = await addData(form.value)
      form.value = res.data || form.value
      router.replace({ query: { ...route.query, id: form.value.dataId } })
      proxy.$modal.msgSuccess('草稿已保存')
    }
  } catch (e) {
    proxy.$modal.msgError('保存草稿失败')
    console.error('保存草稿失败:', e)
  } finally {
    saving.value = false
  }
}

/** 提交填报 */
async function handleSubmit() {
  if (!templateId.value) {
    proxy.$modal.msgWarning('请选择模板')
    return
  }
  proxy.$modal.confirm('确认提交该填报数据？提交后不可修改。').then(async () => {
    submitting.value = true
    try {
      // 保存数据
      if (sheetRef.value) {
        const sheetData = sheetRef.value.getData()
        form.value.formData = typeof sheetData === 'string' ? sheetData : JSON.stringify(sheetData)
      }

      let dataId = form.value.dataId
      if (dataId) {
        const response = await updateData(form.value)
        form.value = response.data || form.value
      } else {
        const res: any = await addData(form.value)
        form.value = res.data || form.value
        dataId = form.value.dataId
      }

      // 执行提交
      if (dataId) {
        await submitData(dataId)
        submitStatus.value = 'submitted'
        proxy.$modal.msgSuccess('提交成功')
      }
    } catch (e) {
      proxy.$modal.msgError('提交失败')
      console.error('提交失败:', e)
    } finally {
      submitting.value = false
    }
  }).catch(() => {})
}

/** 返回 */
function handleBack() {
  router.push({ path: '/collect/data' })
}

onMounted(() => {
  if (route.query.id) {
    loadData()
  } else {
    loadPublishedTemplates()
  }
})
</script>

<style scoped>
.sheet-card {
  min-height: 600px;
}

.template-code {
  color: #909399;
  margin-left: 8px;
  font-size: 12px;
}
</style>
