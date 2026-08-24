<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="报表名称" prop="reportName">
        <el-input
          v-model="queryParams.reportName"
          placeholder="请输入报表名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="创建时间" prop="createTime">
        <el-date-picker clearable
          v-model="queryParams.createTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择创建时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['collect:report:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['collect:report:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['collect:report:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="reportList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键" align="center" prop="id" />
      <el-table-column label="报表名称" align="center" prop="reportName" />
      <el-table-column label="报表简介" align="center" prop="reportJianjie" />
      <el-table-column label="备注" align="center" prop="reportBeizhu" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
        <el-button link type="primary" icon="EditPen" @click="handleEditSheet(scope.row)" v-hasPermi="['collect:report:edit']">编辑表格</el-button>
          </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 报表详情抽屉 -->
    <report-view-drawer ref="reportViewRef" />
    <!-- 添加或修改报表对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="reportRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="报表名称" prop="reportName">
              <el-input v-model="form.reportName" placeholder="请输入报表名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="报表简介" prop="reportJianjie">
              <el-input v-model="form.reportJianjie" placeholder="请输入报表简介" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="reportBeizhu">
              <el-input v-model="form.reportBeizhu" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确定</el-button>
          <el-button @click="cancel">取消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="Report">
import type { WorkReport, ReportQueryParams } from "@/types/api/collect/report"
import { listReport, getReport, delReport, addReport, updateReport } from "@/api/collect/report"
import ReportViewDrawer from "./view"

const { proxy } = getCurrentInstance()

const reportList = ref<WorkReport[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<string[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>("")

const data = reactive({
  form: {} as WorkReport,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    reportName: undefined,
    reportJianjie: undefined,
    reportBeizhu: undefined,
    createTime: undefined,
    userId: undefined,
    deptId: undefined,
    delStatus: undefined
  } as ReportQueryParams,
  rules: {
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询报表列表 */
function getList() {
  loading.value = true
  listReport(queryParams.value).then(response => {
    reportList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    id: undefined,
    reportName: undefined,
    reportJianjie: undefined,
    reportBeizhu: undefined,
    userId: undefined,
    deptId: undefined,
    delStatus: undefined
  }
  proxy.resetForm("reportRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection: WorkReport[]) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加报表"
}

/** 修改按钮操作 */
function handleUpdate(row: WorkReport) {
  reset()
  const _id = row.id || ids.value[0]
  getReport(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改报表"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["reportRef"].validate((valid: boolean) => {
    if (valid) {
      if (form.value.id != null) {
        updateReport(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addReport(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: WorkReport) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除报表编号为"' + _ids + '"的数据项？').then(function() {
    return delReport(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 详情按钮操作 */
function handleViewData(row: WorkReport) {
  proxy.$refs["reportViewRef"].open(row.id)
}

/** 编辑表格按钮操作 */
function handleEditSheet(row: WorkReport) {
  proxy.$tab.openPage(row.reportName, '/collect/report-editor/index/' + row.id)
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('collect/report/export', {
    ...queryParams.value
  }, `report_${new Date().getTime()}.xlsx`)
}

getList()
</script>
