<template>
  <div class="app-container">
    <!-- 页面标题 -->
    <page-header title="大屏列表">
      <template #extra>
        <el-button type="primary" icon="Plus" @click="handleAdd" v-hasPermi="['report:config:add']">新增大屏</el-button>
      </template>
    </page-header>

    <!-- Loading -->
    <el-skeleton v-if="loading" :rows="5" animated />

    <!-- 大屏卡片列表 -->
    <template v-else>
      <el-row :gutter="20" v-if="dashboardList.length > 0">
        <el-col
          v-for="item in dashboardList"
          :key="item.reportId"
          :xl="6"
          :lg="8"
          :md="12"
          :sm="24"
          class="mb20"
        >
          <el-card
            shadow="hover"
            class="dashboard-card"
            :body-style="{ padding: '0px' }"
          >
            <!-- 缩略图 -->
            <div class="dashboard-thumb" @click="handleView(item)">
              <div class="thumb-placeholder">
                <el-icon :size="48"><Monitor /></el-icon>
                <p>{{ item.reportName }}</p>
              </div>
            </div>

            <!-- 卡片底部 -->
            <div class="dashboard-footer">
              <div class="dashboard-info">
                <el-tooltip :content="item.remark || item.reportName" placement="top">
                  <span class="dashboard-name">{{ item.reportName }}</span>
                </el-tooltip>
                <div class="dashboard-meta">
                  <el-tag
                    :type="item.status === '0' ? 'success' : 'info'"
                    size="small"
                  >
                    {{ item.status === '0' ? '启用' : '停用' }}
                  </el-tag>
                  <span class="update-time">{{ parseTime(item.updateTime) || parseTime(item.createTime) }}</span>
                </div>
              </div>
              <div class="dashboard-actions">
                <el-button text type="primary" icon="View" @click="handleView(item)" />
                <el-button text type="primary" icon="Edit" @click="handleEdit(item)" v-hasPermi="['report:config:edit']" />
                <el-button text type="danger" icon="Delete" @click="handleDelete(item)" v-hasPermi="['report:config:remove']" />
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 空状态 -->
      <el-empty v-else description="暂无大屏配置" />

      <!-- 分页 -->
      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </template>
  </div>
</template>

<script setup lang="ts" name="ReportDashboard">
import { useRouter } from 'vue-router'
import { listConfig, delConfig } from '@/api/report/config'
import type { ReportConfig, ReportConfigQueryParams } from '@/types/api/report/config'
import { Monitor } from '@element-plus/icons-vue'

const { proxy } = getCurrentInstance()!
const router = useRouter()

const dashboardList = ref<ReportConfig[]>([])
const loading = ref<boolean>(true)
const total = ref<number>(0)

const queryParams = reactive<ReportConfigQueryParams>({
  pageNum: 1,
  pageSize: 12,
  reportType: '1'  // 只查询大屏类型
})

/** 查询大屏列表 */
function getList() {
  loading.value = true
  listConfig(queryParams).then(response => {
    dashboardList.value = response.rows
    total.value = response.total
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

/** 查看大屏 */
function handleView(item: ReportConfig) {
  router.push({ path: '/report/dashboard', query: { id: item.reportId } })
}

/** 编辑大屏配置 */
function handleEdit(item: ReportConfig) {
  router.push({ path: '/report/config/edit', query: { id: item.reportId } })
}

/** 新增大屏 */
function handleAdd() {
  router.push({ path: '/report/config/edit' })
}

/** 删除大屏 */
function handleDelete(item: ReportConfig) {
  proxy.$modal.confirm('是否确认删除大屏"' + item.reportName + '"？').then(() => {
    return delConfig(item.reportId!)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

getList()
</script>

<style scoped>
.dashboard-card {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  border-radius: 8px;
  overflow: hidden;
}

.dashboard-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.dashboard-thumb {
  height: 180px;
  overflow: hidden;
  background: #f5f7fa;
}

.thumb-image {
  width: 100%;
  height: 100%;
}

.thumb-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
  gap: 8px;
}

.thumb-placeholder p {
  margin: 0;
  font-size: 14px;
  color: #606266;
}

.dashboard-footer {
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dashboard-info {
  flex: 1;
  min-width: 0;
}

.dashboard-name {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dashboard-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.update-time {
  font-size: 12px;
  color: #909399;
}

.dashboard-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.mb20 {
  margin-bottom: 20px;
}
</style>
