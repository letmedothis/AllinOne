<template>
  <div class="app-container">
    <!-- 页面标题 -->
    <page-header :title="pageTitle" @back="handleBack">
      <template #extra>
        <el-button @click="handleBack">返回</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
      </template>
    </page-header>

    <!-- 编辑表单 -->
    <el-card shadow="never">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="报表名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入报表名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="报表编码" prop="code">
              <el-input v-model="form.code" placeholder="请输入报表编码（唯一标识）" maxlength="64" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="报表URL" prop="url">
          <el-input v-model="form.url" placeholder="请输入报表URL地址，如 http://reports.example.com/dashboard/1" maxlength="500">
            <template #append>
              <el-button icon="Link" @click="handleTestUrl">测试连接</el-button>
            </template>
          </el-input>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="报表类型" prop="type">
              <el-radio-group v-model="form.type">
                <el-radio value="1">iframe嵌入</el-radio>
                <el-radio value="2">大屏</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="dict in sys_normal_disable"
                  :key="dict.value"
                  :value="dict.value"
                >{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="宽度(px)" prop="width">
              <el-input-number v-model="form.width" :min="0" placeholder="默认100%" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="高度(px)" prop="height">
              <el-input-number v-model="form.height" :min="0" placeholder="默认100%" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="缩略图" prop="thumbnail">
              <image-upload v-model="form.thumbnail" :limit="1" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="配置信息" prop="config">
          <el-input
            v-model="form.config"
            type="textarea"
            :rows="4"
            placeholder="JSON格式的附加配置（可选）"
          />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入报表描述" />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts" name="ReportConfigEdit">
import { useRouter, useRoute } from 'vue-router'
import { getConfig, addConfig, updateConfig } from '@/api/report/config'
import type { ReportConfig } from '@/types/api/report/config'

const { proxy } = getCurrentInstance()!
const router = useRouter()
const route = useRoute()
const { sys_normal_disable } = useDict('sys_normal_disable')

const saving = ref<boolean>(false)
const isEdit = computed(() => !!route.query.id)

const pageTitle = computed(() => isEdit.value ? '编辑报表配置' : '新增报表配置')

const form = ref<ReportConfig>({
  name: undefined,
  code: undefined,
  url: undefined,
  type: '1',
  status: '0',
  description: undefined,
  config: undefined,
  thumbnail: undefined,
  width: undefined,
  height: undefined
})

const rules = reactive({
  name: [{ required: true, message: '报表名称不能为空', trigger: 'blur' }],
  code: [
    { required: true, message: '报表编码不能为空', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/, message: '编码必须以字母开头，仅允许字母数字下划线', trigger: 'blur' }
  ],
  url: [{ required: true, message: '报表URL不能为空', trigger: 'blur' }]
})

/** 加载报表配置 */
function loadConfig() {
  const id = route.query.id as string
  if (!id) return

  getConfig(Number(id)).then(response => {
    if (response.data) {
      form.value = response.data
    }
  })
}

/** 测试URL连接 */
function handleTestUrl() {
  if (!form.value.url) {
    proxy.$modal.msgWarning('请输入报表URL')
    return
  }
  proxy.$modal.loading('正在测试连接...')
  // 通过iframe尝试加载
  const img = new Image()
  img.onload = () => {
    proxy.$modal.closeLoading()
    proxy.$modal.msgSuccess('连接成功')
  }
  img.onerror = () => {
    proxy.$modal.closeLoading()
    proxy.$modal.msgWarning('无法直接访问该URL，请确认地址是否正确')
  }
  img.src = form.value.url + '?t=' + Date.now()
  setTimeout(() => {
    proxy.$modal.closeLoading()
    proxy.$modal.msgWarning('连接测试超时')
  }, 10000)
}

/** 保存 */
function handleSave() {
  proxy.$refs['formRef'].validate((valid: boolean) => {
    if (!valid) return

    saving.value = true
    if (isEdit.value) {
      updateConfig(form.value).then(() => {
        proxy.$modal.msgSuccess('保存成功')
        handleBack()
      }).finally(() => { saving.value = false })
    } else {
      addConfig(form.value).then(() => {
        proxy.$modal.msgSuccess('新增成功')
        handleBack()
      }).finally(() => { saving.value = false })
    }
  })
}

/** 返回 */
function handleBack() {
  router.push({ path: '/report/config' })
}

onMounted(() => {
  loadConfig()
})
</script>
