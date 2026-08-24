# AllinOne 企业级报表管理系统 — API 接口与页面设计

---

## 1. 接口规范

| 规范 | 说明 |
|------|------|
| **基路径** | 所有 API 以 `/collect/` 或 `/report/` 开头 |
| **返回格式** | 统一返回 `AjaxResult`（单条数据）或 `TableDataInfo`（分页列表） |
| **分页参数** | `pageNum`、`pageSize`，通过 `startPage()` 自动处理 |
| **权限控制** | 通过 `@PreAuthorize` 注解，配合 `v-hasPermi` 前端指令 |
| **日志记录** | 关键操作使用 `@Log` 注解自动记录到 `sys_oper_log` |
| **数据权限** | 填报数据列表使用 `@DataScope` 注解实现部门/用户级数据隔离 |

---

## 2. API 接口列表

### 2.1 填报模板接口（/collect/template）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/collect/template/list` | 分页查询模板列表 | collect:template:list |
| GET | `/collect/template/{id}` | 获取模板详情（含 Luckysheet JSON） | collect:template:query |
| POST | `/collect/template` | 新增模板 | collect:template:add |
| PUT | `/collect/template` | 修改模板 | collect:template:edit |
| DELETE | `/collect/template/{id}` | 删除模板（支持逗号分隔批量） | collect:template:remove |
| POST | `/collect/template/{id}/publish` | 发布/下架模板 | collect:template:edit |

**请求/响应示例：**
```json
// GET /collect/template/list?pageNum=1&pageSize=10&status=1
{
  "code": 200,
  "msg": "操作成功",
  "total": 15,
  "rows": [
    {
      "templateId": 100,
      "templateName": "月度预算表",
      "templateCode": "monthly_budget",
      "categoryId": 1,
      "categoryName": "财务管理",
      "status": "1",
      "version": 3,
      "createBy": "admin",
      "createTime": "2026-07-01 10:00:00"
    }
  ]
}

// GET /collect/template/100
// 除列表字段外，额外返回 template_json（完整的 Luckysheet 配置 JSON）
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "templateId": 100,
    "templateName": "月度预算表",
    "templateCode": "monthly_budget",
    "templateJson": [ /* Luckysheet JSON */ ],
    "status": "1",
    "version": 3
  }
}
```

---

### 2.2 填报数据接口（/collect/data）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/collect/data/list` | 分页查询填报数据列表 | collect:data:list |
| GET | `/collect/data/{id}` | 获取填报数据详情 | collect:data:query |
| POST | `/collect/data` | 新增/保存草稿 | collect:data:add |
| PUT | `/collect/data` | 修改填报数据 | collect:data:edit |
| DELETE | `/collect/data/{id}` | 删除填报数据（支持批量） | collect:data:remove |
| POST | `/collect/data/{id}/submit` | 提交数据（草稿→已提交） | collect:data:edit |

**请求/响应示例：**
```json
// POST /collect/data
{
  "templateId": 100,
  "formData": [ /* Luckysheet 单元格数据 */ ]
}
// 响应：
{ "code": 200, "msg": "操作成功", "data": { "dataId": 1001 } }

// POST /collect/data/1001/submit
// 无请求体
// 响应：
{ "code": 200, "msg": "提交成功" }
```

---

### 2.3 分类接口（/collect/category）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/collect/category/list` | 获取分类树形列表 | collect:category:list |
| POST | `/collect/category` | 新增分类 | collect:category:add |
| PUT | `/collect/category` | 修改分类 | collect:category:edit |
| DELETE | `/collect/category/{id}` | 删除分类（含下级子节点） | collect:category:remove |

---

### 2.4 报表配置接口（/report/config）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/report/config/list` | 分页查询报表配置列表 
| collect:mapping:list | 查询字段映射列表 | 字段映射 |
| collect:mapping:query | 查询字段映射详情 | 字段映射 |
| collect:mapping:add | 新增字段映射 | 字段映射 |
| collect:mapping:edit | 修改字段映射 | 字段映射 |
| collect:mapping:remove | 删除字段映射 | 字段映射 |
| report:config:list |
| GET | `/report/config/{id}` | 获取报表配置详情 | report:config:query |
| POST | `/report/config` | 新增报表配置 | report:config:add |
| PUT | `/report/config` | 修改报表配置 | report:config:edit |
| DELETE | `/report/config/{id}` | 删除报表配置 | report:config:remove |

---

## 3. 权限标识对照表

| 权限标识 | 说明 | 所属模块 |
|---------|------|---------|
| `collect:template:list` | 查询填报模板列表 | 填报模板 |
| `collect:template:query` | 查询填报模板详情 | 填报模板 |
| `collect:template:add` | 新增填报模板 | 填报模板 |
| `collect:template:edit` | 修改/发布/设计填报模板 | 填报模板 |
| `collect:template:remove` | 删除填报模板 | 填报模板 |
| `collect:template:export` | 导出填报模板 | 填报模板 |
| `collect:data:list` | 查询填报数据列表 | 填报数据 |
| `collect:data:query` | 查询填报数据详情 | 填报数据 |
| `collect:data:add` | 新增/草稿保存填报数据 | 填报数据 |
| `collect:data:edit` | 修改/提交填报数据 | 填报数据 |
| `collect:data:remove` | 删除填报数据 | 填报数据 |
| `collect:data:export` | 导出填报数据 | 填报数据 |
| `collect:category:list` | 查询分类树 | 分类管理 |
| `collect:category:add` | 新增分类 | 分类管理 |
| `collect:category:edit` | 修改分类 | 分类管理 |
| `collect:category:remove` | 删除分类 | 分类管理 |
| `report:config:list` | 查询报表配置列表 | 报表配置 |
| `report:config:query` | 查询报表配置详情 | 报表配置 |
| `report:config:add` | 新增报表配置 | 报表配置 |
| `report:config:edit` | 修改报表配置 | 报表配置 |
| `report:config:remove` | 删除报表配置 | 报表配置 |

---

## 4. 前端页面路径

### 4.1 数据填报模块页面

```
src/views/collect/
├── template/
│   ├── index.vue              — 模板列表页
│   │    功能：搜索+表格+发布/下架+CRUD 操作
│   └── edit.vue               — 模板编辑页
│        功能：嵌入 Luckysheet 设计器，设计表头/样式/公式
│
├── data/
│   ├── index.vue              — 填报数据列表页
│   │    功能：按模板/状态筛选、我的填报/全部、删除、导出
│   ├── edit.vue               — 填报数据编辑页
│   │    功能：选择模板→嵌入 Luckysheet→填写→保存草稿/提交
│   └── detail.vue             — 填报数据查看页
│        功能：只读模式展示 Luckysheet 快照
│
└── category/
    └── index.vue              — 分类管理页
         功能：树形表格+CRUD
```

### 4.2 报表模块页面

```
src/views/report/
├── config/
│   ├── index.vue              — 报表配置列表页
│   │    功能：按类型/分类筛选、CRUD
│   └── edit.vue               — 报表配置编辑页
│        功能：表单（名称、编码、类型、关联JimuReport ID）
│
├── view/
│   └── index.vue              — 报表查看页
│        功能：iframe 嵌入 JimuReport 渲染内容
│
└── dashboard/
    ├── list.vue               — 大屏列表页
    │    功能：卡片式布局展示大屏
    └── index.vue              — 大屏查看页
         功能：全屏 iframe 嵌入 + 悬浮工具栏
```

---

## 5. 核心组件设计

### 5.1 CollectSheet.vue

Luckysheet 电子表格封装组件，从本地源码目录 `allinone-luckysheet/` 引入（项目根目录），采用 `file:` 协议或相对路径引用。

**构建方式：**
```bash
cd allinone-luckysheet
yarn install && yarn build        # 构建后生成 dist/ 目录
```

**组件引用路径：**
```typescript
// package.json 中配置 "luckysheet": "file:../allinone-luckysheet"
import luckysheet from 'luckysheet'
import 'luckysheet/dist/css/luckysheet.css'
import 'luckysheet/dist/plugins/css/pluginsCss.css'
```

| Props | 类型 | 默认值 | 说明 |
|-------|------|--------|------|
| `sheetData` | Array/Object/null | null | 表格数据，新建时传null，编辑时传已有数据 |
| `readonly` | Boolean | false | 是否只读模式 |
| `height` | Number | 600 | 容器高度(px) |

| Emits | 说明 |
|-------|------|
| `ready` | Luckysheet 初始化完成 |
| `change(row, col, oldVal, newVal)` | 单元格数据变更 |
| `save` | 手动触发保存 |

| Expose | 说明 |
|--------|------|
| `getSheetData()` | 获取所有工作表数据 |
| `loadData(data)` | 加载指定数据到表格 |

### 5.2 ReportFrame.vue

报表 iframe 加载组件

| Props | 类型 | 默认值 | 说明 |
|-------|------|--------|------|
| `src` | String | '' | iframe 的 src URL |
| `height` | String | '100%' | 高度 |
| `width` | String | '100%' | 宽度 |

| Emits | 说明 |
|-------|------|
| `load` | iframe 加载完成 |

### 5.3 CategoryTree.vue

分类树组件

| Props | 类型 | 默认值 | 说明 |
|-------|------|--------|------|
| `data` | Array | [] | 树数据 |
| `showFilter` | Boolean | true | 显示搜索框 |
| `showCheckbox` | Boolean | false | 显示多选框 |
| `nodeKey` | String | 'categoryId' | 节点 key 字段 |

---

## 6. Pinia Store 设计

### 6.1 useCollectStore（填报模块）

```
store/modules/collect.ts

State:
  templateList: TemplateVO[]     — 模板列表
  templateTotal: number          — 模板总数
  dataList: CollectDataVO[]      — 填报数据列表
  dataTotal: number              — 填报数据总数
  categoryTree: CategoryVO[]     — 分类树
  currentTemplate: TemplateVO    — 当前编辑的模板
  currentData: CollectDataVO     — 当前编辑的填报数据

Actions:
  loadTemplates(params)          — 加载模板列表
  loadDataList(params)           — 加载填报数据列表
  loadCategoryTree()             — 加载分类树
  clearTemplates()               — 清空模板列表
  clearDataList()                — 清空填报数据列表
```

### 6.2 useReportStore（报表模块）

```
store/modules/report.ts

State:
  configList: ReportConfigVO[]   — 报表配置列表
  configTotal: number            — 报表配置总数

Actions:
  loadConfigList(params)         — 加载报表配置列表
  clearConfigList()              — 清空报表配置列表
```

---

## 7. 关键页面代码示例

### 填报编辑页（edit.vue）

```vue
<template>
  <div class="app-container">
    <!-- 工具栏 -->
    <el-form :model="form" inline>
      <el-form-item v-if="!dataId" label="选择模板">
        <el-select v-model="form.templateId" placeholder="请选择填报模板" @change="loadTemplate">
          <el-option v-for="t in templateOptions" :key="t.templateId" :label="t.templateName" :value="t.templateId" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <dict-tag :options="collect_data_status" :value="form.bizStatus" />
      </el-form-item>
    </el-form>

    <!-- Luckysheet 表格区域 -->
    <CollectSheet
      ref="sheetRef"
      :key="sheetKey"
      :sheet-data="form.formData"
      :readonly="form.bizStatus === '1'"
      height="500"
      @ready="onSheetReady"
    />

    <!-- 底部操作栏 -->
    <div style="margin-top: 16px; text-align: center;">
      <el-button @click="handleSaveDraft" :loading="saving" :disabled="form.bizStatus === '1'">
        保存草稿
      </el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting"
        :disabled="form.bizStatus === '1'" v-hasPermi="['collect:data:edit']">
        {{ form.bizStatus === '1' ? '已提交' : '提交数据' }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getData, addData, updateData, submitData } from '@/api/collect/data'
import { listTemplate } from '@/api/collect/template'
import CollectSheet from '@/components/CollectSheet/index.vue'

const route = useRoute()
const dataId = route.params.id as string
const sheetRef = ref()
const sheetKey = ref(0)
const saving = ref(false)
const submitting = ref(false)
const templateOptions = ref<any[]>([])

const form = ref({
  dataId: undefined as number | undefined,
  templateId: undefined as number | undefined,
  formData: null as any,
  bizStatus: '0',
})

// 加载已有数据
onMounted(async () => {
  try {
    if (dataId) {
      const detail = await getData(dataId)
      form.value = detail
      templateName.value = detail.templateName
      sheetKey.value++
    }
    // 加载可选模板列表
    const res = await listTemplate({ status: '1', pageSize: 100 })
    templateOptions.value = res.rows
  } catch (e) {
    ElMessage.error('加载数据失败')
  }
})

// 切换模板时重新加载Luckysheet
async function loadTemplate(templateId: number) {
  try {
    const { getTemplate } = await import('@/api/collect/template')
    const template = await getTemplate(templateId)
    form.value.formData = template.templateJson
    templateName.value = template.templateName
    sheetKey.value++ // 触发重新渲染
  } catch (e) {
    ElMessage.error('加载模板失败')
  }
}

// 保存草稿
async function handleSaveDraft() {
  saving.value = true
  try {
    const sheetData = sheetRef.value?.getSheetData()
    const payload = { ...form.value, formData: sheetData }
    if (form.value.dataId) {
      await updateData(payload)
    } else {
      const res = await addData(payload)
      form.value.dataId = res.dataId
    }
    ElMessage.success('草稿保存成功')
  } catch (e: any) {
    ElMessage.error(e?.msg || '草稿保存失败')
  } finally {
    saving.value = false
  }
}

// 提交数据
async function handleSubmit() {
  submitting.value = true
  try {
    // 先保存最新数据
    const sheetData = sheetRef.value?.getSheetData()
    const payload = { ...form.value, formData: sheetData }
    if (form.value.dataId) {
      await updateData(payload)
      await submitData(form.value.dataId)
    } else {
      const res = await addData(payload)
      await submitData(res.dataId)
    }
    ElMessage.success('数据提交成功')
    form.value.bizStatus = '1'
  } catch (e: any) {
    ElMessage.error(e?.msg || '提交失败')
  } finally {
    submitting.value = false
  }
}

function onSheetReady() {
  console.log('Luckysheet 初始化完成')
}
</script>
```

### API 封装示例

```typescript
// src/api/collect/template.ts
import request from '@/utils/request'
import type { TableResult } from '@/types/api'

export interface TemplateQuery {
  pageNum?: number
  pageSize?: number
  templateName?: string
  categoryId?: number
  status?: string
}

export interface TemplateVO {
  templateId: number
  templateName: string
  templateCode: string
  categoryId?: number
  categoryName?: string
  templateJson?: any[]
  status: string
  version: number
  createBy: string
  createTime: string
}

export function listTemplate(query: TemplateQuery): Promise<TableResult<TemplateVO>> {
  return request({ url: '/collect/template/list', method: 'get', params: query })
}

export function getTemplate(id: number): Promise<TemplateVO> {
  return request({ url: '/collect/template/' + id, method: 'get' })
}

export function addTemplate(data: any): Promise<any> {
  return request({ url: '/collect/template', method: 'post', data })
}

export function updateTemplate(data: any): Promise<any> {
  return request({ url: '/collect/template', method: 'put', data })
}

export function delTemplate(id: string): Promise<any> {
  return request({ url: '/collect/template/' + id, method: 'delete' })
}

export function publishTemplate(id: number, status: string): Promise<any> {
  return request({ url: '/collect/template/' + id + '/publish', method: 'post', data: { status } })
}
```

```typescript
// src/api/collect/data.ts
import request from '@/utils/request'
import type { TableResult } from '@/types/api'

export interface CollectDataQuery {
  pageNum?: number
  pageSize?: number
  templateId?: number
  bizStatus?: string
  submitBy?: string
}

export interface CollectDataVO {
  dataId?: number
  templateId: number
  templateName?: string
  formData?: any[]
  bizStatus: string
  submitBy?: string
  submitTime?: string
  createBy: string
  createTime: string
}

export function listData(query: CollectDataQuery): Promise<TableResult<CollectDataVO>> {
  return request({ url: '/collect/data/list', method: 'get', params: query })
}

export function getData(id: number): Promise<CollectDataVO> {
  return request({ url: '/collect/data/' + id, method: 'get' })
}

export function addData(data: any): Promise<any> {
  return request({ url: '/collect/data', method: 'post', data })
}

export function updateData(data: any): Promise<any> {
  return request({ url: '/collect/data', method: 'put', data })
}

export function delData(id: string): Promise<any> {
  return request({ url: '/collect/data/' + id, method: 'delete' })
}

export function submitData(id: number): Promise<any> {
  return request({ url: '/collect/data/' + id + '/submit', method: 'post' })
}
```
