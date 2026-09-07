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
              <el-input v-model="form.reportCode" placeholder="唯一编码，用于URL访问" maxlength="64" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="报表类型" prop="reportType">
              <el-radio-group v-model="form.reportType">
                <el-radio
                  v-for="dict in report_config_type"
                  :key="dict.value"
                  :value="dict.value"
                >{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属分类" prop="categoryId">
              <el-tree-select
                v-model="form.categoryId"
                :data="categoryOptions"
                :props="{ value: 'categoryId', label: 'categoryName', children: 'children' }"
                placeholder="选择报表分类"
                check-strictly
                clearable
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item v-if="form.reportType === '0'" label="JimuReport ID" prop="jimuReportId">
          <el-select v-model="form.jimuReportId" filterable clearable placeholder="搜索并选择 JimuReport 报表" style="width: 100%">
            <el-option v-for="report in jimuReports" :key="report.id" :label="jimuReportLabel(report)" :value="report.id">
              <div class="jimu-report-option">
                <span>{{ report.name || '未命名报表' }}</span>
                <small>{{ report.code || '无编码' }} · {{ report.id }}</small>
              </div>
            </el-option>
          </el-select>
          <div class="form-item-tip">从已保存的 JimuReport 报表中选择；系统会在保存时再次校验该报表仍存在。</div>
        </el-form-item>
        <el-form-item v-else label="JimuBI ID" prop="jmbiId">
          <el-input v-model="form.jmbiId" placeholder="请输入 JimuBI 大屏/仪表盘ID" maxlength="64" />
          <div class="form-item-tip">填写 JimuBI 大屏/仪表盘页面的 pageId（大屏设计器保存的页面 ID）；注意它不是积木报表的报表 ID</div>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="显示排序" prop="orderNum">
              <el-input-number v-model="form.orderNum" controls-position="right" :min="0" style="width: 100%" />
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

        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts" name="ReportConfigEdit">
import { useRouter, useRoute } from 'vue-router'
import { getConfig, addConfig, updateConfig, listJimuReports } from '@/api/report/config'
import { listCategory } from '@/api/report/category'
import type { JimuReportDefinition, ReportConfig } from '@/types/api/report/config'
import type { ReportCategory } from '@/types/api/report/category'

const { proxy } = getCurrentInstance()!
const router = useRouter()
const route = useRoute()
const { sys_normal_disable } = useDict('sys_normal_disable')
const { report_config_type } = useDict('report_config_type')

const saving = ref<boolean>(false)
const isEdit = computed(() => !!route.query.id)
const categoryOptions = ref<ReportCategory[]>([])
const jimuReports = ref<JimuReportDefinition[]>([])

const pageTitle = computed(() => isEdit.value ? '编辑报表配置' : '新增报表配置')

const form = ref<ReportConfig>({
  reportId: undefined,
  reportName: undefined,
  reportCode: undefined,
  reportType: '0',
  jimuReportId: undefined,
  jmbiId: undefined,
  categoryId: undefined,
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
  jimuReportId: [{ required: true, message: '请选择 JimuReport 报表', trigger: 'change' }]
})

function jimuReportLabel(report: JimuReportDefinition): string {
  return `${report.name || '未命名报表'}${report.code ? `（${report.code}）` : ''}`
}

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

/** 加载报表分类树 */
function loadCategoryTree() {
  listCategory().then(response => {
    categoryOptions.value = proxy.handleTree(response.data, 'categoryId')
  })
}

function loadJimuReports() {
  listJimuReports().then(response => {
    jimuReports.value = response.data || []
  })
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
  loadCategoryTree()
  loadJimuReports()
})
</script>

<style scoped>
.form-item-tip {
  width: 100%;
  font-size: 12px;
  line-height: 1.5;
  color: var(--el-text-color-secondary);
}

.jimu-report-option {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.jimu-report-option small {
  color: var(--el-text-color-secondary);
}
</style>
