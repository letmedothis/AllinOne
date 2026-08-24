<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="报表名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入报表名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="报表类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="报表类型" clearable style="width: 150px">
          <el-option label="iframe嵌入" value="1" />
          <el-option label="大屏" value="2" />
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
      <el-table-column label="编号" align="center" prop="id" width="80" />
      <el-table-column label="报表名称" align="center" prop="name" min-width="160" :show-overflow-tooltip="true" />
      <el-table-column label="报表编码" align="center" prop="code" width="140" />
      <el-table-column label="报表URL" align="center" prop="url" min-width="200" :show-overflow-tooltip="true">
        <template #default="scope">
          <el-link type="primary" :href="scope.row.url" target="_blank" :underline="false">
            {{ scope.row.url }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column label="类型" align="center" prop="type" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.type === '2' ? 'success' : ''" disable-transitions>
            {{ scope.row.type === '2' ? '大屏' : 'iframe嵌入' }}
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
        <el-form-item label="报表名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入报表名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="报表编码" prop="code">
          <el-input v-model="form.code" placeholder="请输入报表编码（唯一标识）" maxlength="64" />
        </el-form-item>
        <el-form-item label="报表URL" prop="url">
          <el-input v-model="form.url" placeholder="请输入报表URL地址" maxlength="500" />
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
          <el-col :span="12">
            <el-form-item label="宽度(px)" prop="width">
              <el-input-number v-model="form.width" :min="0" placeholder="默认100%" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="高度(px)" prop="height">
              <el-input-number v-model="form.height" :min="0" placeholder="默认100%" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
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
import type { FormInstance, FormRules } from 'element-plus'

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

const data = reactive({
  form: {} as ReportConfig,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    type: undefined,
    status: undefined
  } as ReportConfigQueryParams,
  rules: {
    name: [{ required: true, message: '报表名称不能为空', trigger: 'blur' }],
    code: [
      { required: true, message: '报表编码不能为空', trigger: 'blur' },
      { pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/, message: '编码必须以字母开头，仅允许字母数字下划线', trigger: 'blur' }
    ],
    url: [{ required: true, message: '报表URL不能为空', trigger: 'blur' }]
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
    id: undefined,
    name: undefined,
    code: undefined,
    url: undefined,
    type: '1',
    status: '0',
    description: undefined,
    width: undefined,
    height: undefined
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
  ids.value = selection.map(item => item.id!)
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
  const id = row.id || ids.value[0]
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
    if (form.value.id !== undefined) {
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
  const delIds = row.id || ids.value
  proxy.$modal.confirm(`是否确认删除报表编号为"${delIds}"的数据项？`).then(() => {
    return delConfig(delIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

/** 查看报表 */
function handleView(row: ReportConfig) {
  if (row.type === '2') {
    // 大屏跳转
    router.push({ path: '/report/dashboard', query: { id: row.id } })
  } else {
    // iframe嵌入
    router.push({ path: '/report/view', query: { id: row.id } })
  }
}

getList()
</script>
