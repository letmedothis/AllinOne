<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="报表名称" prop="reportName">
        <el-input
          v-model="queryParams.reportName"
          placeholder="请输入报表名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="报表类型" prop="reportType">
        <el-select v-model="queryParams.reportType" placeholder="报表类型" clearable style="width: 150px">
          <el-option label="报表" value="0" />
          <el-option label="大屏" value="1" />
          <el-option label="仪表盘" value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 150px">
          <el-option
            v-for="dict in sys_normal_disable"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['report:config:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['report:config:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['report:config:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="configList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编号" align="center" prop="reportId" min-width="150" />
      <el-table-column label="报表名称" align="center" prop="reportName" min-width="160" :show-overflow-tooltip="true" />
      <el-table-column label="报表编码" align="center" prop="reportCode" width="140" />
      <el-table-column label="关联ID" align="center" min-width="180" :show-overflow-tooltip="true">
        <template #default="scope">
          {{ getRelationId(scope.row) || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="类型" align="center" prop="reportType" width="100">
        <template #default="scope">
          <el-tag :type="getReportTypeMeta(scope.row.reportType).tagType" disable-transitions>
            {{ getReportTypeMeta(scope.row.reportType).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="260" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleView(scope.row)" v-hasPermi="['report:config:query']">
            查看
          </el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['report:config:edit']">
            修改
          </el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['report:config:remove']">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改对话框 -->
    <el-dialog :title="title" v-model="open" width="650px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="报表名称" prop="reportName">
          <el-input v-model="form.reportName" placeholder="请输入报表名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="报表编码" prop="reportCode">
          <el-input v-model="form.reportCode" placeholder="请输入报表编码（唯一标识）" maxlength="64" />
        </el-form-item>
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
        <el-form-item label="显示顺序" prop="orderNum">
          <el-input-number v-model="form.orderNum" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="ReportConfig">
import type { ReportConfig, ReportConfigQueryParams } from '@/types/api/report/config'
import { listConfig, getConfig, delConfig, addConfig, updateConfig } from '@/api/report/config'
import { useRouter } from 'vue-router'
import type { FormInstance } from 'element-plus'

const { proxy } = getCurrentInstance()!
const router = useRouter()
const { sys_normal_disable } = useDict('sys_normal_disable')

const formRef = ref<FormInstance>()

const configList = ref<ReportConfig[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>('')

const reportTypeMeta: Record<string, { label: string; tagType: '' | 'success' | 'warning' }> = {
  '0': { label: '报表', tagType: '' },
  '1': { label: '大屏', tagType: 'success' },
  '2': { label: '仪表盘', tagType: 'warning' }
}

const unknownReportTypeMeta = { label: '未知', tagType: 'info' as const }

const data = reactive({
  form: {} as ReportConfig,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    reportName: undefined,
    reportType: undefined,
    status: undefined
  } as ReportConfigQueryParams,
  rules: {
    reportName: [{ required: true, message: '报表名称不能为空', trigger: 'blur' }],
    reportCode: [
      { required: true, message: '报表编码不能为空', trigger: 'blur' },
      { pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/, message: '编码必须以字母开头，仅允许字母数字下划线', trigger: 'blur' }
    ],
    reportType: [{ required: true, message: '报表类型不能为空', trigger: 'change' }],
    jimuReportId: [{ required: true, message: 'JimuReport ID不能为空', trigger: 'blur' }],
    jmbiId: [{ required: true, message: 'JimuBI ID不能为空', trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询报表列表 */
function getList() {
  loading.value = true
  listConfig(queryParams.value).then(response => {
    configList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 取消 */
function cancel() {
  open.value = false
  reset()
}

/** 重置表单 */
function reset() {
  form.value = {
    reportId: undefined,
    reportName: undefined,
    reportCode: undefined,
    reportType: '0',
    jimuReportId: undefined,
    jmbiId: undefined,
    orderNum: 0,
    status: '0',
    remark: undefined
  }
  proxy.resetForm('formRef')
}

/** 搜索 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置搜索 */
function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

/** 多选变化 */
function handleSelectionChange(selection: ReportConfig[]) {
  ids.value = selection.map(item => item.reportId!)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

/** 新增 */
function handleAdd() {
  reset()
  open.value = true
  title.value = '新增报表配置'
}

/** 修改 */
function handleUpdate(row: ReportConfig) {
  reset()
  const id = row.reportId || ids.value[0]
  getConfig(id).then(response => {
    form.value = response.data!
    open.value = true
    title.value = '修改报表配置'
  })
}

/** 提交 */
async function submitForm() {
  try {
    await formRef.value?.validate()
    if (form.value.reportId !== undefined) {
      await updateConfig(form.value)
      proxy.$modal.msgSuccess('修改成功')
    } else {
      await addConfig(form.value)
      proxy.$modal.msgSuccess('新增成功')
    }
    open.value = false
    getList()
  } catch (fields) {
    // 验证失败
  }
}

/** 删除 */
function handleDelete(row: ReportConfig) {
  const delIds = row.reportId || ids.value
  proxy.$modal.confirm(`是否确认删除报表编号为"${delIds}"的数据项？`).then(() => {
    return delConfig(delIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

/** 查看报表 */
function handleView(row: ReportConfig) {
  if (row.reportType === '0') {
    router.push({ path: '/report/view', query: { id: row.reportId } })
  } else {
    router.push({ path: '/report/dashboard/view', query: { id: row.reportId } })
  }
}

function getRelationId(row: ReportConfig): string | undefined {
  return row.reportType === '0' ? row.jimuReportId : row.jmbiId
}

function getReportTypeMeta(reportType?: string) {
  return reportType ? reportTypeMeta[reportType] || unknownReportTypeMeta : unknownReportTypeMeta
}

function handleReportTypeChange(reportType: string | number | boolean) {
  if (String(reportType) === '0') {
    form.value.jmbiId = undefined
  } else {
    form.value.jimuReportId = undefined
  }
}

getList()
</script>
