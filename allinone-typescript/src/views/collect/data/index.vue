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
      <el-form-item label="填报状态" prop="bizStatus">
        <el-select v-model="queryParams.bizStatus" placeholder="填报状态" clearable style="width: 150px">
          <el-option label="草稿" value="draft" />
          <el-option label="已提交" value="submitted" />
        </el-select>
      </el-form-item>
      <el-form-item label="创建人" prop="createBy">
        <el-input
          v-model="queryParams.createBy"
          placeholder="请输入创建人"
          clearable
          style="width: 150px"
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
          icon="Edit"
          @click="handleAdd"
          v-hasPermi="['collect:data:add']"
        >新增填报</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['collect:data:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['collect:data:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['collect:data:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编号" align="center" prop="dataId" min-width="150" />
      <el-table-column label="业务编码" align="center" prop="dataCode" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="模板名称" align="center" prop="templateName" width="140" :show-overflow-tooltip="true" />
      <el-table-column label="模板编码" align="center" prop="templateCode" width="120" />
      <el-table-column label="填报状态" align="center" prop="bizStatus" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.bizStatus === 'submitted' ? 'success' : 'warning'" disable-transitions>
            {{ scope.row.bizStatus === 'submitted' ? '已提交' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建人" align="center" prop="createBy" width="120" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" align="center" prop="submitTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.submitTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="250" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['collect:data:query']">
            查看
          </el-button>
          <el-button
            v-if="scope.row.bizStatus !== 'submitted'"
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['collect:data:edit']"
          >编辑</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['collect:data:remove']">
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
  </div>
</template>

<script setup lang="ts" name="CollectData">
import type { CollectData, CollectDataQueryParams } from '@/types/api/collect/data'
import { listData, delData } from '@/api/collect/data'
import { useRouter } from 'vue-router'

const { proxy } = getCurrentInstance()!
const router = useRouter()

const dataList = ref<CollectData[]>([])
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    templateName: undefined,
    bizStatus: undefined,
    createBy: undefined
  } as CollectDataQueryParams
})

const { queryParams } = toRefs(data)

/** 查询填报数据列表 */
async function getList() {
  loading.value = true
  try {
    const response = await listData(queryParams.value)
    dataList.value = response.rows
    total.value = response.total
  } finally {
    loading.value = false
  }
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
function handleSelectionChange(selection: CollectData[]) {
  ids.value = selection.map(item => item.dataId!)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

/** 新增 */
function handleAdd() {
  router.push({ path: '/collect/data/edit' })
}

/** 修改 */
function handleUpdate(row: CollectData) {
  const id = row.dataId || ids.value[0]
  router.push({ path: '/collect/data/edit', query: { id } })
}

/** 查看详情（只读） */
function handleDetail(row: CollectData) {
  router.push({ path: '/collect/data/detail', query: { id: row.dataId } })
}

/** 删除 */
function handleDelete(row: CollectData) {
  const delIds = row.dataId || ids.value
  proxy.$modal.confirm(`是否确认删除填报数据编号为"${delIds}"的数据项？`).then(() => {
    return delData(delIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

/** 导出 */
function handleExport() {
  proxy.download('collect/data/export', {
    ...queryParams.value
  }, `collect_data_${new Date().getTime()}.xlsx`)
}

getList()
</script>
