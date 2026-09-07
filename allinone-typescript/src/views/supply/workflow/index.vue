<template>
  <div class="app-container workflow-designer">
    <el-alert title="先保存草稿，再发布为新版本。点击人工任务后，可在右侧配置审批人；新发布只影响之后提交的订单。" type="info" show-icon class="mb16" />
    <el-row :gutter="16">
      <el-col :xl="17" :lg="16" :xs="24">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <div class="title-row">
                <el-input v-model="design.name" maxlength="100" class="name-input" @change="markDirty" />
                <el-tag :type="design.status === 'PUBLISHED' && !dirty ? 'success' : 'warning'">{{ design.status === 'PUBLISHED' && !dirty ? `已发布 V${design.publishedVersion || ''}` : '草稿' }}</el-tag>
              </div>
              <div>
                <el-button @click="resetModel" :disabled="busy">恢复草稿</el-button>
                <el-button :loading="saving" @click="saveDraft" v-hasPermi="['supply:workflow:deploy']">保存草稿</el-button>
                <el-button type="primary" :loading="publishing" @click="publish" v-hasPermi="['supply:workflow:deploy']">发布新版本</el-button>
              </div>
            </div>
          </template>
          <div ref="canvasRef" class="bpmn-canvas" />
        </el-card>
      </el-col>
      <el-col :xl="7" :lg="8" :xs="24" class="right-panel">
        <el-card shadow="never" class="property-card">
          <template #header><span>节点属性</span></template>
          <template v-if="selectedElement">
            <el-form label-position="top">
              <el-form-item label="节点名称"><el-input v-model="nodeForm.name" maxlength="100" @change="applyNodeName" /></el-form-item>
              <el-form-item label="审批人类型">
                <el-select v-model="nodeForm.type" class="full-width" @change="changeAssigneeType">
                  <el-option label="采购主管配置" value="SUPERVISOR_CONFIG" />
                  <el-option label="指定用户" value="USER" />
                  <el-option label="角色候选人" value="ROLE" />
                  <el-option label="指定部门负责人" value="DEPT_LEADER" />
                  <el-option label="发起人部门负责人" value="INITIATOR_MANAGER" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="needsValue" :label="valueLabel">
                <el-select v-model="nodeForm.value" filterable class="full-width" placeholder="请选择" @change="applyAssignment">
                  <el-option v-for="item in valueOptions" :key="`${item.type}:${item.value}`" :label="item.label" :value="item.value">
                    <span>{{ item.label }}</span><span class="option-desc">{{ item.description }}</span>
                  </el-option>
                </el-select>
              </el-form-item>
              <el-alert v-else :title="assignmentHint" type="success" :closable="false" show-icon />
            </el-form>
          </template>
          <el-empty v-else description="请选择画布中的人工任务" :image-size="72" />
        </el-card>
        <el-card shadow="never" class="version-card">
          <template #header><div class="card-header"><span>已发布版本</span><el-button link type="primary" @click="loadDefinitions">刷新</el-button></div></template>
          <el-table v-loading="loading" :data="definitions" size="small" max-height="300">
            <el-table-column label="版本" width="65"><template #default="scope">V{{ scope.row.version }}</template></el-table-column>
            <el-table-column label="状态" width="72"><template #default="scope"><el-tag :type="scope.row.suspended ? 'danger' : 'success'" size="small">{{ scope.row.suspended ? '停用' : '启用' }}</el-tag></template></el-table-column>
            <el-table-column label="操作" min-width="142"><template #default="scope">
              <el-button link type="primary" @click="loadPublished(scope.row)">载入</el-button>
              <el-button link :type="scope.row.suspended ? 'success' : 'danger'" @click="toggleState(scope.row)" v-hasPermi="['supply:workflow:deploy']">{{ scope.row.suspended ? '启用' : '停用' }}</el-button>
            </template></el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts" name="SupplyWorkflowDesigner">
import BpmnModeler from 'bpmn-js/lib/Modeler'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'
import { changeWorkflowDefinitionState, getWorkflowDefinitionXml, getWorkflowDesign, listWorkflowCandidates, listWorkflowDefinitions, publishWorkflowDesign, saveWorkflowDraft } from '@/api/supply'
import type { WorkflowAssigneeType, WorkflowCandidateOption, WorkflowDesign, WorkflowProcessDefinition } from '@/types/api/supply'

const PROCESS_KEY = 'purchase-order-approval'
const { proxy } = getCurrentInstance()!
const canvasRef = ref<HTMLElement>()
const modeler = shallowRef<any>()
const definitions = ref<WorkflowProcessDefinition[]>([])
const candidates = ref<WorkflowCandidateOption[]>([])
const design = reactive<WorkflowDesign>({ processKey: PROCESS_KEY, name: '采购订单审批', bpmnXml: '', revision: 0, status: 'DRAFT' })
const loading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const dirty = ref(false)
const selectedElement = shallowRef<any>()
const nodeForm = reactive<{ name: string; type: WorkflowAssigneeType | ''; value: string }>({ name: '', type: '', value: '' })
const busy = computed(() => saving.value || publishing.value)
const needsValue = computed(() => ['USER', 'ROLE', 'DEPT_LEADER'].includes(nodeForm.type))
const valueLabel = computed(() => nodeForm.type === 'USER' ? '审批用户' : nodeForm.type === 'ROLE' ? '审批角色' : nodeForm.type === 'DEPT_LEADER' ? '负责人所属部门' : '审批对象')
const valueOptions = computed(() => candidates.value.filter((item: WorkflowCandidateOption) => item.type === nodeForm.type))
const assignmentHint = computed(() => nodeForm.type === 'INITIATOR_MANAGER' ? '运行时自动取发起人所属部门负责人' : '运行时沿用供应链采购主管配置')

async function importXml(xml: string, changed = false) {
  await modeler.value.importXML(xml)
  modeler.value.get('canvas').zoom('fit-viewport')
  selectedElement.value = undefined
  dirty.value = changed
}

async function resetModel() {
  const response = await getWorkflowDesign(PROCESS_KEY)
  const source = response.data
  if (!source?.bpmnXml) return
  Object.assign(design, source)
  await importXml(source.bpmnXml, false)
}

async function loadDefinitions() {
  loading.value = true
  try {
    const response = await listWorkflowDefinitions()
    definitions.value = response.data || []
  } finally { loading.value = false }
}

async function buildPayload(): Promise<WorkflowDesign | undefined> {
  const result = await modeler.value.saveXML({ format: true })
  if (!result.xml) return
  return { ...toRaw(design), bpmnXml: result.xml }
}

async function saveDraft() {
  const payload = await buildPayload()
  if (!payload) return
  saving.value = true
  try {
    const response = await saveWorkflowDraft(payload)
    Object.assign(design, response.data)
    dirty.value = false
    proxy.$modal.msgSuccess('流程草稿已保存')
  } finally { saving.value = false }
}

async function publish() {
  const payload = await buildPayload()
  if (!payload) return
  publishing.value = true
  try {
    await publishWorkflowDesign(payload)
    proxy.$modal.msgSuccess('新版本已发布，之后提交的订单将使用它')
    const response = await getWorkflowDesign(PROCESS_KEY)
    Object.assign(design, response.data)
    dirty.value = false
    await loadDefinitions()
  } finally { publishing.value = false }
}

async function loadPublished(row: WorkflowProcessDefinition) {
  if (dirty.value) {
    try { await proxy.$modal.confirm('当前未保存的修改会被覆盖，是否继续？') } catch { return }
  }
  const response = await getWorkflowDefinitionXml(row.id)
  await importXml(response.data || '', true)
  proxy.$modal.msgSuccess(`已载入 V${row.version}，保存后将成为当前草稿`)
}

async function toggleState(row: WorkflowProcessDefinition) {
  const action = row.suspended ? '启用' : '停用'
  try { await proxy.$modal.confirm(`确认${action}流程版本 V${row.version}？已运行的实例不会改变。`) } catch { return }
  await changeWorkflowDefinitionState(row.id, !row.suspended)
  proxy.$modal.msgSuccess(`已${action} V${row.version}`)
  await loadDefinitions()
}

function readNode(element: any) {
  const attrs = element.businessObject?.$attrs || {}
  nodeForm.name = element.businessObject?.name || ''
  nodeForm.type = (attrs['allinone:assigneeType'] || (attrs['flowable:assignee'] === '${approverUserId}' ? 'SUPERVISOR_CONFIG' : '')) as WorkflowAssigneeType | ''
  nodeForm.value = attrs['allinone:assigneeValue'] || ''
}

function applyNodeName() {
  if (!selectedElement.value) return
  modeler.value.get('modeling').updateProperties(selectedElement.value, { name: nodeForm.name })
  markDirty()
}

function changeAssigneeType() {
  nodeForm.value = ''
  applyAssignment()
}

function applyAssignment() {
  if (!selectedElement.value || !nodeForm.type) return
  const nodeId = selectedElement.value.id
  const props: Record<string, string | undefined> = {
    'allinone:assigneeType': nodeForm.type,
    'allinone:assigneeValue': nodeForm.value,
    'flowable:assignee': undefined,
    'flowable:candidateGroups': undefined
  }
  if (nodeForm.type === 'SUPERVISOR_CONFIG') props['flowable:assignee'] = '${approverUserId}'
  if (nodeForm.type === 'USER') props['flowable:assignee'] = nodeForm.value
  if (nodeForm.type === 'ROLE') props['flowable:candidateGroups'] = nodeForm.value
  if (nodeForm.type === 'DEPT_LEADER' || nodeForm.type === 'INITIATOR_MANAGER') props['flowable:assignee'] = `\${wfAssignee_${nodeId}}`
  modeler.value.get('modeling').updateProperties(selectedElement.value, props)
  markDirty()
}

function markDirty() { dirty.value = true }

onMounted(async () => {
  modeler.value = new BpmnModeler({ container: canvasRef.value })
  modeler.value.on('selection.changed', (event: any) => {
    const element = event.newSelection?.[0]
    selectedElement.value = element?.type === 'bpmn:UserTask' ? element : undefined
    if (selectedElement.value) readNode(selectedElement.value)
  })
  modeler.value.on('commandStack.changed', markDirty)
  const [candidateResponse] = await Promise.all([listWorkflowCandidates(), loadDefinitions()])
  candidates.value = candidateResponse.data || []
  await resetModel()
})

onBeforeUnmount(() => modeler.value?.destroy())
</script>

<style scoped>
.card-header,.title-row { display:flex;align-items:center;justify-content:space-between;gap:12px; }
.title-row { justify-content:flex-start;min-width:340px; }.name-input { width:240px; }.full-width { width:100%; }
.bpmn-canvas { height:650px;border:1px solid var(--el-border-color-light); }.right-panel>*+* { margin-top:16px; }
.property-card { min-height:330px; }.option-desc { float:right;color:var(--el-text-color-secondary);font-size:12px;margin-left:16px; }
@media (max-width:1199px) { .right-panel { margin-top:16px; }.card-header { flex-wrap:wrap; } }
</style>
