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
          <el-option
            v-for="dict in report_config_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
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
      <el-table-column label="编号" align="center" prop="reportId" width="80" />
      <el-table-column label="报表名称" align="center" prop="reportName" min-width="160" :show-overflow-tooltip="true" />
      <el-table-column label="报表编码" align="center" prop="reportCode" width="140" />
      <el-table-column label="报表类型" align="center" prop="reportType" width="100">
        <template #default="scope">
          <dict-tag :options="report_config_type" :value="scope.row.reportType" />
        </template>
      </el-table-column>
      <el-table-column label="所属分类" align="center" prop="categoryName" width="120" :show-overflow-tooltip="true" />
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
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
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
          <el-input v-model="form.jimuReportId" placeholder="请输入 JimuReport 报表ID" maxlength="64" />
        </el-form-item>
        <el-form-item v-else label="JimuBI ID" prop="jmbiId">
          <el-input v-model="form.jmbiId" placeholder="请输入 JimuBI 大屏/仪表盘ID" maxlength="64" />
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
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" />
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
import { listCategory } from '@/api/report/category'
import type { ReportCategory } from '@/types/api/report/category'
import { useRouter } from 'vue-router'

const { proxy } = getCurrentInstance()!
const router = useRouter()
const { sys_normal_disable } = useDict('sys_normal_disable')
const { report_config_type } = useDict('report_config_type')

const formRef = ref()

const configList = ref<ReportConfig[]>([])
const categoryOptions = ref<ReportCategory[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>('')

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
    reportType: [{ required: true, message: '报表类型不能为空', trigger: 'change' }]
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

/** 加载报表分类树 */
function loadCategoryTree() {
  listCategory().then(response => {
    categoryOptions.value = proxy.handleTree(response.data, 'categoryId')
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
    categoryId: undefined,
    icon: undefined,
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

/** 查看报表/大屏 */
function handleView(row: ReportConfig) {
  if (!row.url) {
    proxy.$modal.msgWarning('该报表未配置引擎ID，无法查看')
    return
  }
  if (row.reportType === '0') {
    router.push({ path: '/report/view', query: { id: row.reportId } })
  } else {
    router.push({ path: '/report/dashboard', query: { id: row.reportId } })
  }
}

getList()
loadCategoryTree()
</script>
