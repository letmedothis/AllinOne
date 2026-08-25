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
            <el-form-item label="报表名称" prop="reportName">
              <el-input v-model="form.reportName" placeholder="请输入报表名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="报表编码" prop="reportCode">
              <el-input v-model="form.reportCode" placeholder="请输入报表编码（唯一标识）" maxlength="64" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="报表类型" prop="reportType">
              <el-radio-group v-model="form.reportType" @change="handleReportTypeChange">
                <el-radio value="0">报表</el-radio>
                <el-radio value="1">大屏</el-radio>
                <el-radio value="2">仪表盘</el-radio>
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

        <el-form-item v-if="form.reportType === '0'" label="JimuReport ID" prop="jimuReportId">
          <el-input v-model="form.jimuReportId" placeholder="请输入 JimuReport 报表ID" maxlength="64" />
        </el-form-item>
        <el-form-item v-else label="JimuBI ID" prop="jmbiId">
          <el-input v-model="form.jmbiId" placeholder="请输入 JimuBI 大屏/仪表盘ID" maxlength="64" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="图标" prop="icon">
              <el-input v-model="form.icon" placeholder="请输入图标名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示顺序" prop="orderNum">
              <el-input-number v-model="form.orderNum" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
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
  reportName: undefined,
  reportCode: undefined,
  reportType: '0',
  jimuReportId: undefined,
  jmbiId: undefined,
  icon: undefined,
  orderNum: 0,
  status: '0',
  remark: undefined
})

const rules = reactive({
  reportName: [{ required: true, message: '报表名称不能为空', trigger: 'blur' }],
  reportCode: [
    { required: true, message: '报表编码不能为空', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/, message: '编码必须以字母开头，仅允许字母数字下划线', trigger: 'blur' }
  ],
  reportType: [{ required: true, message: '报表类型不能为空', trigger: 'change' }],
  jimuReportId: [{ required: true, message: 'JimuReport ID不能为空', trigger: 'blur' }],
  jmbiId: [{ required: true, message: 'JimuBI ID不能为空', trigger: 'blur' }]
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

function handleReportTypeChange(reportType: string | number | boolean) {
  if (String(reportType) === '0') {
    form.value.jmbiId = undefined
  } else {
    form.value.jimuReportId = undefined
  }
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
