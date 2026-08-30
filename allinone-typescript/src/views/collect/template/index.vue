<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="模板名称" prop="templateName">
        <el-input
          v-model="queryParams.templateName"
          placeholder="请输入模板名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="模板状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="模板状态" clearable style="width: 200px">
          <el-option label="草稿" value="0" />
          <el-option label="已发布" value="1" />
          <el-option label="已下架" value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-tree-select
          v-model="queryParams.categoryId"
          :data="categoryTree"
          :props="{ value: 'categoryId', label: 'categoryName', children: 'children' }"
          placeholder="选择分类"
          clearable
          style="width: 200px"
          check-strictly
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
          v-hasPermi="['collect:template:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['collect:template:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['collect:template:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['collect:template:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="templateList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编号" align="center" prop="templateId" min-width="150" />
      <el-table-column label="模板名称" align="center" prop="templateName" min-width="160" :show-overflow-tooltip="true" />
      <el-table-column label="模板编码" align="center" prop="templateCode" width="150" />
      <el-table-column label="分类" align="center" prop="categoryName" width="120" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <dict-tag :options="collect_template_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="版本号" align="center" prop="version" width="80" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="320" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleEditConfig(scope.row)" v-hasPermi="['collect:template:edit']">
            设计
          </el-button>
          <el-button link type="primary" icon="CopyDocument" @click="handleCopy(scope.row)" v-hasPermi="['collect:template:add']">
            复制
          </el-button>
          <el-button link type="primary" icon="View" @click="handlePreview(scope.row)" v-hasPermi="['collect:template:query']">
            预览
          </el-button>
          <el-button
            link
            :type="scope.row.status === '1' ? 'warning' : 'success'"
            :icon="scope.row.status === '1' ? 'Bottom' : 'Top'"
            @click="handlePublish(scope.row)"
            v-hasPermi="['collect:template:edit']"
          >
            {{ scope.row.status === '1' ? '下架' : '发布' }}
          </el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['collect:template:edit']">
            修改
          </el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['collect:template:remove']">
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
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="form.templateName" placeholder="请输入模板名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="模板编码" prop="templateCode">
          <el-input v-model="form.templateCode" placeholder="请输入模板编码（唯一标识）" maxlength="64" />
        </el-form-item>
        <el-form-item label="所属分类" prop="categoryId">
          <el-tree-select
            v-model="form.categoryId"
            :data="categoryTree"
            :props="{ value: 'categoryId', label: 'categoryName', children: 'children' }"
            placeholder="选择分类"
            check-strictly
            clearable
          />
        </el-form-item>
        <el-form-item label="描述" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入描述" />
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

<script setup lang="ts" name="CollectTemplate">
import type { CollectTemplate, CollectTemplateQueryParams } from '@/types/api/collect/template'
import { listTemplate, getTemplate, delTemplate, addTemplate, updateTemplate, publishTemplate, copyTemplate } from '@/api/collect/template'
import { listCategory } from '@/api/collect/category'
import { useRouter } from 'vue-router'

const { proxy } = getCurrentInstance()!
const router = useRouter()

const { collect_template_status } = useDict('collect_template_status')

const templateList = ref<CollectTemplate[]>([])
const categoryTree = ref<any[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>('')

const data = reactive({
  form: {} as CollectTemplate,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    templateName: undefined,
    status: undefined,
    categoryId: undefined
  } as CollectTemplateQueryParams,
  rules: {
    templateName: [{ required: true, message: '模板名称不能为空', trigger: 'blur' }],
    templateCode: [
      { required: true, message: '模板编码不能为空', trigger: 'blur' },
      { pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/, message: '模板编码必须以字母开头，仅允许字母数字下划线', trigger: 'blur' }
    ]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询模板列表 */
async function getList() {
  loading.value = true
  try {
    const response = await listTemplate(queryParams.value)
    templateList.value = response.rows
    total.value = response.total
  } finally {
    loading.value = false
  }
}

/** 加载分类树 */
function loadCategoryTree() {
  listCategory().then(response => {
    categoryTree.value = proxy.handleTree(response.data, 'categoryId')
  })
}

/** 取消 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    templateId: undefined,
    templateName: undefined,
    templateCode: undefined,
    categoryId: undefined,
    remark: undefined,
    status: '0'
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
function handleSelectionChange(selection: CollectTemplate[]) {
  ids.value = selection.map(item => item.templateId!)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

/** 新增 */
function handleAdd() {
  reset()
  loadCategoryTree()
  open.value = true
  title.value = '新增填报模板'
}

/** 修改 */
function handleUpdate(row: CollectTemplate) {
  reset()
  loadCategoryTree()
  const id = row.templateId || ids.value[0]
  getTemplate(id).then(response => {
    form.value = response.data!
    open.value = true
    title.value = '修改填报模板'
  })
}

/** 提交 */
function submitForm() {
  proxy.$refs['formRef'].validate((valid: boolean) => {
    if (valid) {
      if (form.value.templateId !== undefined) {
        updateTemplate(form.value).then(() => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        addTemplate(form.value).then(() => {
          proxy.$modal.msgSuccess('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除 */
function handleDelete(row: CollectTemplate) {
  const delIds = row.templateId || ids.value
  proxy.$modal.confirm('是否确认删除模板编号为"' + delIds + '"的数据项？').then(() => {
    return delTemplate(delIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

/** 设计模板（进入编辑页） */
function handleEditConfig(row: CollectTemplate) {
  router.push({ path: '/collect/template/edit', query: { id: row.templateId } })
}

/** 预览模板 */
function handlePreview(row: CollectTemplate) {
  router.push({ path: '/collect/template/edit', query: { id: row.templateId, readonly: '1' } })
}

/** 复制模板：克隆为未发布新模板，成功后直接进入编辑页 */
function handleCopy(row: CollectTemplate) {
  proxy.$modal.confirm('是否复制模板"' + row.templateName + '"？将生成未发布的副本。').then(() => {
    return copyTemplate(row.templateId!)
  }).then((response: any) => {
    proxy.$modal.msgSuccess('复制成功')
    getList()
    const copy = response?.data
    if (copy?.templateId) {
      router.push({ path: '/collect/template/edit', query: { id: copy.templateId } })
    }
  }).catch(() => {})
}

/** 发布/下架 */
function handlePublish(row: CollectTemplate) {
  const action = row.status === '1' ? '下架' : '发布'
  proxy.$modal.confirm('是否确认' + action + '模板"' + row.templateName + '"？').then(() => {
    return publishTemplate(row.templateId!, row.status === '1' ? '2' : '1')
  }).then(() => {
    proxy.$modal.msgSuccess(action + '成功')
    getList()
  }).catch(() => {})
}

/** 导出 */
function handleExport() {
  proxy.download('collect/template/export', {
    ...queryParams.value
  }, `template_${new Date().getTime()}.xlsx`)
}

getList()
</script>
