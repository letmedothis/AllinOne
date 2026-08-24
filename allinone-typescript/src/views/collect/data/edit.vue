<template>
  <div class="app-container">
    <!-- 页面标题 -->
    <page-header :title="pageTitle" @back="handleBack">
      <template #extra>
        <el-space>
          <el-button @click="handleBack">返回</el-button>
          <el-button v-if="!isEdit" type="primary" :loading="saving" @click="handleSaveDraft">
            保存草稿
          </el-button>
          <el-button
            v-if="!isEdit"
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
              :key="item.id"
              :label="item.name"
              :value="item.id"
            >
              <span>{{ item.name }}</span>
              <span class="template-code">[{{ item.code }}]</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item v-if="selectedTemplate">
          <el-tag>{{ selectedTemplate.categoryName || '未分类' }}</el-tag>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 已选模板信息 -->
    <el-card v-if="templateId" shadow="never" class="mb8">
      <el-descriptions :column="4" size="small">
        <el-descriptions-item label="模板名称">{{ templateInfo.name }}</el-descriptions-item>
        <el-descriptions-item label="模板编码">{{ templateInfo.code }}</el-descriptions-item>
        <el-descriptions-item label="版本号">v{{ templateInfo.version }}</el-descriptions-item>
        <el-descriptions-item label="填报状态">
          <el-tag :type="submitStatus === '1' ? 'success' : 'warning'" disable-transitions>
            {{ submitStatus === '1' ? '已提交' : '草稿' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 数据标题 -->
    <el-card v-if="templateId && !isEdit" shadow="never" class="mb8">
      <el-form :inline="true">
        <el-form-item label="数据标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入数据标题" style="width: 400px" maxlength="200" />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 提交状态提示 -->
    <el-alert
      v-if="submitStatus === '1'"
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
        :sheetData="form.data"
        :readonly="isEdit || submitStatus === '1'"
        :height="700"
        @change="onSheetChange"
        @save="onSheetSave"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts" name="CollectDataEdit">
import { useRouter, useRoute } from 'vue-router'
import { listTemplate } from '@/api/collect/template'
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
  return publishedTemplates.value.find(t => t.id === selectedTemplateId.value)
})

/** 模板信息（编辑时） */
const templateInfo = ref<CollectTemplate>({})

/** 提交状态 */
const submitStatus = ref<string>('0')

/** 页面标题 */
const pageTitle = computed(() => {
  if (isEdit.value) return '编辑填报数据'
  return '新增填报'
})

/** 表单数据 */
const form = ref<CollectData>({
  title: undefined,
  templateId: undefined,
  data: undefined,
  status: '0'
})

/** 加载已发布的模板列表 */
function loadPublishedTemplates() {
  listTemplate({ pageNum: 1, pageSize: 100, status: '1' }).then(response => {
    publishedTemplates.value = response.rows
  })
}

/** 模板切换 */
function onTemplateChange(id: number) {
  templateId.value = id
  form.value.templateId = id
  const tmpl = selectedTemplate.value
  if (tmpl?.config) {
    form.value.data = tmpl.config
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
    submitStatus.value = data.status || '0'

    // 加载模板信息
    if (data.templateId) {
      listTemplate({ pageNum: 1, pageSize: 100 }).then(res => {
        const tmpl = res.rows.find(t => t.id === data.templateId)
        if (tmpl) {
          templateInfo.value = tmpl
        }
      })
    }
  })
}

/** 表格数据变更 */
function onSheetChange(data: any) {
  form.value.data = typeof data === 'string' ? data : JSON.stringify(data)
}

/** 表格保存事件 */
function onSheetSave(data: any) {
  form.value.data = typeof data === 'string' ? data : JSON.stringify(data)
}

/** 保存草稿 */
async function handleSaveDraft() {
  if (!templateId.value) {
    proxy.$modal.msgWarning('请选择模板')
    return
  }
  if (!form.value.title) {
    proxy.$modal.msgWarning('请输入数据标题')
    return
  }

  saving.value = true
  try {
    // 从Luckysheet获取数据
    if (sheetRef.value) {
      const sheetData = sheetRef.value.getData()
      form.value.data = typeof sheetData === 'string' ? sheetData : JSON.stringify(sheetData)
    }
    form.value.status = '0'

    if (form.value.id) {
      await updateData(form.value)
      proxy.$modal.msgSuccess('草稿已更新')
    } else {
      const res: any = await addData(form.value)
      form.value.id = res?.data?.id
      router.replace({ query: { ...route.query, id: form.value.id } })
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
  if (!form.value.title) {
    proxy.$modal.msgWarning('请输入数据标题')
    return
  }

  proxy.$modal.confirm('确认提交该填报数据？提交后不可修改。').then(async () => {
    submitting.value = true
    try {
      // 保存数据
      if (sheetRef.value) {
        const sheetData = sheetRef.value.getData()
        form.value.data = typeof sheetData === 'string' ? sheetData : JSON.stringify(sheetData)
      }

      let dataId = form.value.id
      if (dataId) {
        await updateData(form.value)
      } else {
        const res: any = await addData(form.value)
        dataId = res?.data?.id
        form.value.id = dataId
      }

      // 执行提交
      if (dataId) {
        await submitData(dataId)
        submitStatus.value = '1'
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
