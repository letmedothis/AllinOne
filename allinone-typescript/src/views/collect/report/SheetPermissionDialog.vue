<template>
  <el-dialog
    :title="'权限管理 - ' + sheetName"
    v-model="visible"
    width="640px"
    append-to-body
    :before-close="handleClose">

    <el-alert
      v-if="!isCreator"
      title="只有创建者和管理员可以修改权限"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom:12px;" />

    <el-table :data="permissions" v-loading="loading" max-height="280" style="width:100%" stripe>
      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="row.permType === 'role' ? 'primary' : row.permType === 'dept' ? 'success' : 'warning'" size="small">
            {{ row.permType === 'role' ? '角色' : row.permType === 'dept' ? '部门' : '用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="名称" prop="permName" min-width="180" />
      <el-table-column label="操作" width="80" align="center">
        <template #default="{ row }">
          <el-button v-if="isCreator" link type="danger" size="small" :loading="deleting" @click="handleRevoke(row)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-divider content-position="left">添加权限</el-divider>
      <el-form :inline="true" :model="addForm" size="small" style="text-align:center;">
        <el-form-item label="类型">
          <el-select v-model="addForm.permType" style="width:120px" :disabled="!isCreator" @change="onTypeChange">
            <el-option label="角色" value="role" />
            <el-option label="部门" value="dept" />
            <el-option label="用户" value="user" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标">
          <el-select
            v-if="addForm.permType === 'role'"
            v-model="addForm.permId"
            filterable
            placeholder="选择角色"
            style="width:240px"
            :disabled="!isCreator">
            <el-option v-for="r in roleList" :key="r.roleId" :label="r.roleName" :value="r.roleId" />
          </el-select>
          <el-select
            v-else-if="addForm.permType === 'dept'"
            v-model="addForm.permId"
            filterable
            placeholder="选择部门"
            style="width:240px"
            :disabled="!isCreator">
            <el-option v-for="d in deptFlat" :key="d.deptId" :label="d.deptName" :value="d.deptId" />
          </el-select>
          <el-select
            v-else-if="addForm.permType === 'user'"
            v-model="addForm.permId"
            filterable
            remote
            :remote-method="searchUser"
            :loading="userSearching"
            placeholder="搜索用户"
            style="width:240px"
            :disabled="!isCreator">
            <el-option v-for="u in userList" :key="u.userId" :label="u.nickName" :value="u.userId" />
          </el-select>
          <span v-else style="color:#999;">请先选择类型</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="adding" :disabled="!addForm.permId || !isCreator" @click="handleGrant">添加</el-button>
        </el-form-item>
      </el-form>
      <div style="text-align:right;margin-top:12px;">
        <el-button @click="handleClose">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { getPermissions, grantPermission, revokePermission } from '@/api/collect/report'
import { listRole } from '@/api/system/role'
import { listDept } from '@/api/system/dept'
import { listUser } from '@/api/system/user'
import { ElMessage } from 'element-plus'

interface PermItem { id: number; sheetId: string; permType: string; permId: number; permName: string }

const visible = ref(false)
const loading = ref(false)
const adding = ref(false)
const deleting = ref(false)
const userSearching = ref(false)

const sheetDbId = ref('')
const sheetName = ref('')
const isCreator = ref(false)
const permissions = ref<PermItem[]>([])
const roleList = ref<any[]>([])
const deptFlat = ref<any[]>([])
const userList = ref<any[]>([])

const addForm = reactive({ permType: 'role' as string, permId: null as number | null })

// name cache
const roleMap = new Map<number, string>()
const deptMap = new Map<number, string>()
const userMap = new Map<number, string>()

async function open(sDbId: string, sName: string, creator: boolean) {
  sheetDbId.value = sDbId
  sheetName.value = sName
  isCreator.value = creator
  visible.value = true
  addForm.permType = 'role'
  addForm.permId = null
  await Promise.all([loadRoles(), loadDepts(), loadPermissions()])
}

async function loadRoles() {
  try {
    const res = await listRole({ pageNum: 1, pageSize: 999 })
    roleList.value = res.rows || []
    roleList.value.forEach((r: { roleId: number; roleName: string }) => roleMap.set(r.roleId, r.roleName))
  } catch { /* ignore */ }
}

async function loadDepts() {
  try {
    const res = await listDept()
    const items: any[] = res.data || []
    const flat: any[] = []
    function walk(arr: any[]) { arr.forEach((n: any) => { flat.push(n); if (n.children) walk(n.children) }) }
    walk(items)
    deptFlat.value = flat
    flat.forEach((d: { deptId: number; deptName: string }) => deptMap.set(d.deptId, d.deptName))
  } catch { /* ignore */ }
}

async function searchUser(query: string) {
  if (!query) return
  userSearching.value = true
  try {
    const res = await listUser({ pageNum: 1, pageSize: 50, userName: query })
    userList.value = res.rows || []
    userList.value.forEach((u: { userId: number; nickName: string }) => userMap.set(u.userId, u.nickName))
  } catch { /* ignore */ }
  userSearching.value = false
}

async function loadPermissions() {
  loading.value = true
  try {
    const res = await getPermissions(sheetDbId.value)
    const raw: any[] = res.data || []
    permissions.value = raw.map(p => ({ ...p, permName: resolveName(p.permType, p.permId) }))
  } catch { /* ignore */ }
  loading.value = false
}

function resolveName(type: string, id: number): string {
  if (type === 'role') return roleMap.get(id) || '角色#' + id
  if (type === 'dept') return deptMap.get(id) || '部门#' + id
  if (type === 'user') return userMap.get(id) || '用户#' + id
  return String(id)
}

function onTypeChange() { addForm.permId = null; userList.value = [] }

async function handleGrant() {
  if (!addForm.permId) return
  adding.value = true
  try {
    await grantPermission(sheetDbId.value, addForm.permType, addForm.permId)
    if (addForm.permType === 'role') { const r = roleList.value.find((x: { roleId: number }) => x.roleId === addForm.permId); if (r) roleMap.set(r.roleId, r.roleName) }
    if (addForm.permType === 'dept') { const d = deptFlat.value.find((x: { deptId: number }) => x.deptId === addForm.permId); if (d) deptMap.set(d.deptId, d.deptName) }
    if (addForm.permType === 'user') { const u = userList.value.find((x: { userId: number }) => x.userId === addForm.permId); if (u) userMap.set(u.userId, u.nickName) }
    addForm.permId = null
    await loadPermissions()
  } catch (e: any) { ElMessage.error(e?.msg || '添加失败') }
  adding.value = false
}

async function handleRevoke(row: PermItem) {
  deleting.value = true
  try {
    await revokePermission(row.sheetId, row.permType, row.permId)
    await loadPermissions()
  } catch (e: any) { ElMessage.error(e?.msg || '移除失败') }
  deleting.value = false
}

function handleClose() { visible.value = false }

defineExpose({ open })
</script>
