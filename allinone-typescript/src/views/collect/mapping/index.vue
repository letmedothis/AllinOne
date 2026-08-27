<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="所属模板" prop="templateId">
        <el-select v-model="queryParams.templateId" placeholder="选择模板" clearable filterable style="width: 220px">
          <el-option
            v-for="item in templateOptions"
            :key="item.templateId"
            :label="item.templateName"
            :value="item.templateId!"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="目标表名" prop="targetTable">
        <el-input
          v-model="queryParams.targetTable"
          placeholder="请输入目标表名"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
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
          v-hasPermi="['collect:mapping:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['collect:mapping:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['collect:mapping:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="mappingList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编号" align="center" prop="mappingId" width="150" />
      <el-table-column label="所属模板" align="center" prop="templateId" width="140">
        <template #default="scope">
          <span>{{ templateName(scope.row.templateId) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="单元格坐标" align="center" prop="cellRef" width="100" />
      <el-table-column label="Sheet序号" align="center" prop="sheetIndex" width="90" />
      <el-table-column label="行号" align="center" prop="rowIndex" width="70" />
      <el-table-column label="列号" align="center" prop="colIndex" width="70" />
      <el-table-column label="目标表" align="center" prop="targetTable" min-width="130" :show-overflow-tooltip="true" />
      <el-table-column label="目标列" align="center" prop="targetColumn" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="数据类型" align="center" prop="dataType" width="100" />
      <el-table-column label="转换类型" align="center" prop="transformType" width="90">
        <template #default="scope">
          <el-tag :type="transformTagType(scope.row.transformType)">{{ transformLabel(scope.row.transformType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="处理顺序" align="center" prop="orderNum" width="90" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['collect:mapping:edit']">
            修改
          </el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['collect:mapping:remove']">
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

    <!-- 添加或修改字段映射对话框 -->
    <el-dialog :title="title" v-model="open" width="680px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="所属模板" prop="templateId">
              <el-select v-model="form.templateId" placeholder="选择所属模板" filterable style="width: 100%">
                <el-option
                  v-for="item in templateOptions"
                  :key="item.templateId"
                  :label="item.templateName"
                  :value="item.templateId!"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标表名" prop="targetTable">
              <el-input v-model="form.targetTable" placeholder="请输入目标表名" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标列名" prop="targetColumn">
              <el-input v-model="form.targetColumn" placeholder="请输入目标列名" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单元格坐标" prop="cellRef">
              <el-input v-model="form.cellRef" placeholder="如 B3" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数据类型" prop="dataType">
              <el-input v-model="form.dataType" placeholder="如 varchar、number" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Sheet序号" prop="sheetIndex">
              <el-input-number v-model="form.sheetIndex" controls-position="right" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="行号" prop="rowIndex">
              <el-input-number v-model="form.rowIndex" controls-position="right" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="列号" prop="colIndex">
              <el-input-number v-model="form.colIndex" controls-position="right" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="主键顺序" prop="pkOrder">
              <el-input-number v-model="form.pkOrder" controls-position="right" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="处理顺序" prop="orderNum">
              <el-input-number v-model="form.orderNum" controls-position="right" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="默认值" prop="defaultValue">
              <el-input v-model="form.defaultValue" placeholder="请输入默认值" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="转换类型" prop="transformType">
              <el-select v-model="form.transformType" placeholder="选择转换类型" style="width: 100%">
                <el-option label="无" value="0" />
                <el-option label="格式化" value="1" />
                <el-option label="脚本" value="2" />
                <el-option label="Bean" value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="转换脚本" prop="transformScript">
              <el-input
                v-model="form.transformScript"
                type="textarea"
                :rows="3"
                placeholder="转换类型为脚本时生效"
                maxlength="500"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" maxlength="500" />
            </el-form-item>
          </el-col>
        </el-row>
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

<script setup lang="ts" name="CollectFieldMapping">
import type { CollectFieldMapping, FieldMappingQueryParams } from '@/api/collect/mapping'
import { listMapping, getMapping, addMapping, updateMapping, delMapping } from '@/api/collect/mapping'
import { listTemplate } from '@/api/collect/template'
import type { CollectTemplate } from '@/types/api/collect/template'

const { proxy } = getCurrentInstance()!

const mappingList = ref<CollectFieldMapping[]>([])
const templateOptions = ref<CollectTemplate[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>('')

/** 转换类型选项（0无 1格式化 2脚本 3Bean） */
const transformTypes: Record<string, { label: string; tag: 'info' | 'primary' | 'warning' | 'success' }> = {
  '0': { label: '无', tag: 'info' },
  '1': { label: '格式化', tag: 'primary' },
  '2': { label: '脚本', tag: 'warning' },
  '3': { label: 'Bean', tag: 'success' }
}

const data = reactive({
  form: {} as CollectFieldMapping,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    templateId: undefined,
    targetTable: undefined
  } as FieldMappingQueryParams,
  rules: {
    templateId: [{ required: true, message: '所属模板不能为空', trigger: 'change' }],
    targetTable: [{ required: true, message: '目标表名不能为空', trigger: 'blur' }],
    targetColumn: [{ required: true, message: '目标列名不能为空', trigger: 'blur' }],
    rowIndex: [{ required: true, message: '行号不能为空', trigger: 'blur' }],
    colIndex: [{ required: true, message: '列号不能为空', trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询字段映射列表 */
async function getList() {
  loading.value = true
  try {
    const response = await listMapping(queryParams.value)
    mappingList.value = response.rows
    total.value = response.total
  } finally {
    loading.value = false
  }
}

/** 加载模板选项 */
function loadTemplateOptions() {
  listTemplate({ pageNum: 1, pageSize: 1000 }).then(response => {
    templateOptions.value = response.rows
  })
}

/** 模板名称 */
function templateName(templateId?: number) {
  const template = templateOptions.value.find(item => item.templateId === templateId)
  return template?.templateName ?? templateId
}

/** 转换类型标签 */
function transformLabel(transformType?: string) {
  return transformTypes[transformType ?? '0']?.label ?? transformType
}

/** 转换类型标签颜色 */
function transformTagType(transformType?: string) {
  return transformTypes[transformType ?? '0']?.tag ?? 'info'
}

/** 取消 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    mappingId: undefined,
    templateId: undefined,
    cellRef: undefined,
    sheetIndex: 0,
    rowIndex: undefined,
    colIndex: undefined,
    targetTable: undefined,
    targetColumn: undefined,
    dataType: undefined,
    pkOrder: 0,
    defaultValue: undefined,
    transformType: '0',
    transformScript: undefined,
    orderNum: 0
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
function handleSelectionChange(selection: CollectFieldMapping[]) {
  ids.value = selection.map(item => item.mappingId!)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

/** 新增 */
function handleAdd() {
  reset()
  loadTemplateOptions()
  open.value = true
  title.value = '新增字段映射'
}

/** 修改 */
function handleUpdate(row?: CollectFieldMapping) {
  reset()
  loadTemplateOptions()
  const id = row?.mappingId || ids.value[0]
  getMapping(id).then(response => {
    form.value = response.data!
    open.value = true
    title.value = '修改字段映射'
  })
}

/** 提交 */
function submitForm() {
  proxy.$refs['formRef'].validate((valid: boolean) => {
    if (valid) {
      if (form.value.mappingId !== undefined) {
        updateMapping(form.value).then(() => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        addMapping(form.value).then(() => {
          proxy.$modal.msgSuccess('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除 */
function handleDelete(row?: CollectFieldMapping) {
  const delIds = row?.mappingId || ids.value
  proxy.$modal.confirm('是否确认删除字段映射编号为"' + delIds + '"的数据项？').then(() => {
    return delMapping(delIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

getList()
loadTemplateOptions()
</script>
