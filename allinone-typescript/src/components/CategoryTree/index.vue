<template>
  <div class="category-tree">
    <!-- Empty / 无数据 -->
    <el-empty v-if="!data || data.length === 0" :description="emptyText" />

    <!-- 搜索过滤 -->
    <div v-if="showFilter" class="tree-filter">
      <el-input
        v-model="filterText"
        placeholder="输入关键字过滤"
        size="small"
        clearable
        prefix-icon="Search"
      />
    </div>

    <!-- 树形组件 -->
    <el-tree
      v-if="data && data.length > 0"
      ref="treeRef"
      :data="treeData"
      :props="treeProps"
      :node-key="nodeKey"
      :default-expand-all="defaultExpandAll"
      :highlight-current="highlightCurrent"
      :filter-node-method="filterNode"
      :show-checkbox="showCheckbox"
      :check-strictly="checkStrictly"
      :current-node-key="currentKey"
      :expand-on-click-node="expandOnClickNode"
      :draggable="draggable"
      @node-click="handleNodeClick"
      @check="handleCheck"
      @node-drag-end="handleDragEnd"
    >
      <template #default="{ node, data: nodeData }">
        <span class="custom-tree-node" :class="{ 'is-disabled': nodeData.disabled }">
          <!-- 图标 -->
          <el-icon v-if="nodeData.icon" class="tree-node-icon">
            <component :is="nodeData.icon" />
          </el-icon>
          <el-icon v-else class="tree-node-icon">
            <FolderOpened v-if="node.isExpanded && nodeData.children?.length" />
            <Folder v-else-if="nodeData.children?.length" />
            <Document v-else />
          </el-icon>

          <!-- 名称 -->
          <span class="tree-node-label">{{ nodeData[labelKey] || node.label }}</span>

          <!-- 附加信息 -->
          <span v-if="showMeta && nodeData.meta" class="tree-node-meta">
            ({{ nodeData.meta }})
          </span>
        </span>
      </template>
    </el-tree>

    <!-- 空状态（过滤后无结果） -->
    <el-empty v-if="filterText && filteredEmpty && data?.length" :description="'无匹配结果'" />
  </div>
</template>

<script setup lang="ts" name="CategoryTree">
import { FolderOpened, Folder, Document } from '@element-plus/icons-vue'
import type ElTree from 'element-plus/es/components/tree/src/tree.vue'

const props = defineProps({
  /** 树形数据 */
  data: {
    type: Array as PropType<any[]>,
    default: () => []
  },
  /** 树节点配置 */
  treeProps: {
    type: Object,
    default: () => ({
      children: 'children',
      label: 'name'
    })
  },
  /** 节点key字段名 */
  nodeKey: {
    type: String,
    default: 'id'
  },
  /** 标签字段名 */
  labelKey: {
    type: String,
    default: 'name'
  },
  /** 是否默认展开全部 */
  defaultExpandAll: {
    type: Boolean,
    default: false
  },
  /** 是否高亮当前选中 */
  highlightCurrent: {
    type: Boolean,
    default: true
  },
  /** 是否显示搜索过滤 */
  showFilter: {
    type: Boolean,
    default: false
  },
  /** 是否显示多选框 */
  showCheckbox: {
    type: Boolean,
    default: false
  },
  /** 多选框是否父子不关联 */
  checkStrictly: {
    type: Boolean,
    default: true
  },
  /** 点击节点是否展开 */
  expandOnClickNode: {
    type: Boolean,
    default: true
  },
  /** 是否可拖拽 */
  draggable: {
    type: Boolean,
    default: false
  },
  /** 是否显示附加信息 */
  showMeta: {
    type: Boolean,
    default: false
  },
  /** 空数据提示 */
  emptyText: {
    type: String,
    default: '暂无分类'
  },
  /** 当前选中key */
  currentKey: {
    type: [String, Number] as PropType<string | number | null>,
    default: null
  }
})

const emit = defineEmits<{
  'node-click': [data: any, node: any]
  check: [data: any[], checked: any]
  'drag-end': [data: any, newParent: any, oldParent: any]
  'update:currentKey': [key: string | number | null]
}>()

const treeRef = ref<InstanceType<typeof ElTree> | null>(null)
const filterText = ref<string>('')
const filteredEmpty = ref<boolean>(false)

/** 过滤条件变化时，调用树过滤 */
watch(filterText, (val: string) => {
  treeRef.value?.filter(val)
})

/** 过滤节点方法 */
function filterNode(value: string, node: any): boolean {
  if (!value) return true
  const label = node[props.labelKey] || node.label || ''
  const match = label.toLowerCase().includes(value.toLowerCase())
  return match
}

/** 节点点击 */
function handleNodeClick(nodeData: any, node: any) {
  emit('node-click', nodeData, node)
}

/** 多选变化 */
function handleCheck(nodeData: any, checkResult: any) {
  emit('check', checkResult.checkedNodes, checkResult.checkedKeys)
}

/** 拖拽结束 */
function handleDragEnd(draggingNode: any, dropNode: any, dropType: string, ev: any) {
  if (!draggingNode) return
  emit('drag-end', draggingNode.data, dropNode?.parent?.data, draggingNode.parent?.data)
}

/** 设置当前选中节点 */
function setCurrentKey(key: string | number | null) {
  treeRef.value?.setCurrentKey(key)
  emit('update:currentKey', key)
}

/** 获取选中节点 */
function getCurrentNode() {
  return treeRef.value?.getCurrentNode()
}

/** 获取选中节点key */
function getCurrentKey() {
  return treeRef.value?.getCurrentKey()
}

/** 获取勾选节点IDs */
function getCheckedKeys(leafOnly = false) {
  return treeRef.value?.getCheckedKeys(leafOnly)
}

/** 设置勾选节点 */
function setCheckedKeys(keys: (string | number)[]) {
  treeRef.value?.setCheckedKeys(keys)
}

defineExpose({
  setCurrentKey,
  getCurrentNode,
  getCurrentKey,
  getCheckedKeys,
  setCheckedKeys,
  filter: (value: string) => { filterText.value = value },
  treeRef
})
</script>

<style scoped>
.category-tree {
  width: 100%;
}

.tree-filter {
  margin-bottom: 8px;
}

.custom-tree-node {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
}

.custom-tree-node.is-disabled {
  color: #c0c4cc;
  cursor: not-allowed;
}

.tree-node-icon {
  flex-shrink: 0;
  color: #909399;
}

.tree-node-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-node-meta {
  color: #909399;
  font-size: 12px;
  margin-left: 4px;
}

/* 深色模式背景适配 */
:deep(.el-tree-node__content) {
  height: 36px;
}
</style>
