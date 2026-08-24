---
name: element-plus
description: Element Plus Vue 3 UI 组件库 — 70+ 组件的桌面端组件库，与若依 Vue3+TypeScript 前端项目深度集成
metadata:
  type: reference
---

# Element Plus UI 开发 Skill

> 🟢 **集成状态：已完全集成** (v2.13.1) | 关联：[Vue3](vue3.md) · [RuoYi-Vue](ruoyi-vue.md)

## 概述

Element Plus 是饿了么前端团队为 Vue 3 打造的桌面端 UI 组件库，全面拥抱 Composition API、深度集成 TypeScript。本项目 (`allinone-typescript`) 已使用 Element Plus 2.13.1 + `@element-plus/icons-vue` 2.3.2。

**官方资源：**
- 文档: https://element-plus.org/zh-CN/
- GitHub: https://github.com/element-plus/element-plus
- 图标: https://element-plus.org/zh-CN/component/icon.html

---

## 项目中的使用方式（全局注册）

本项目采用 **全局注册** 方式引入 Element Plus（配置在 `src/main.ts` 中）：

```ts
// src/main.ts — 全局注册（项目实际使用方式）
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

const app = createApp(App)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
```

**这意味着：** 所有组件已全局注册，在 Vue 模板中可直接使用 `<el-button>`、`<el-input>` 等组件，**无需手动 import 组件本身**。

但以下内容仍需手动 import：
```typescript
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'       // JS 调用的组件
import type { FormInstance, FormRules, UploadProps } from 'element-plus'     // TypeScript 类型
import { Edit, Delete, Search } from '@element-plus/icons-vue'               // 图标组件
```

---

## 组件分类大全

### 一、基础组件

| 组件名 | 标签 | 关键属性 | 场景 |
|--------|------|---------|------|
| **Button** | `<el-button>` | `type`, `size`, `plain`, `round`, `circle`, `loading`, `disabled`, `icon` | 所有按钮操作 |
| **Icon** | `<el-icon>` | `size`, `color` | 图标展示（不会自动按需引入，需手动 import） |
| **Typography** | `<el-text>` `<el-link>` | `type`, `size`, `truncated`, `tag` | 文本排版 |
| **Layout** | `<el-row>` `<el-col>` | `gutter`, `span`, `offset`, `xs/sm/md/lg/xl` | 页面布局 |

**图标用法：**
```vue
<template>
  <el-icon :size="24" color="#409eff">
    <Edit />
  </el-icon>
</template>
<script setup lang="ts">
import { Edit } from '@element-plus/icons-vue'
</script>
```

---

### 二、布局组件

| 组件 | 说明 | 关键属性 |
|------|------|---------|
| **Container** | 页面骨架容器 | `<el-container>` `<el-header>` `<el-aside>` `<el-main>` `<el-footer>` |
| **Grid** | 24 列栅格 | `<el-row>` `<el-col>` 的 `span/offset/push/pull` |
| **Divider** | 分割线 | `<el-divider>` `direction`, `content-position` |
| **Space** | 间距 | `<el-space>` `size`, `direction`, `wrap` |

---

### 三、导航组件

| 组件 | 说明 | 关键属性/子组件 |
|------|------|----------------|
| **Menu** | 菜单导航 | `<el-menu>` `mode`, `collapse`, `router`, `default-active` |
| **Tabs** | 标签页 | `<el-tabs>` `type`, `tab-position`, `editable` |
| **Breadcrumb** | 面包屑 | `<el-breadcrumb>` `<el-breadcrumb-item>` |
| **Dropdown** | 下拉菜单 | `<el-dropdown>` `<el-dropdown-menu>` `<el-dropdown-item>` |
| **Steps** | 步骤条 | `<el-steps>` `<el-step>` `active`, `process-status` |
| **Pagination** | 分页 | `<el-pagination>` `total`, `page-size`, `current-page`, `layout` |
| **PageHeader** | 页头 | `<el-page-header>` `title`, `content` |
| **Backtop** | 回到顶部 | `<el-backtop>` `target`, `visibility-height` |

---

### 四、数据录入（表单）组件

| 组件 | 标签 | 关键属性 | 说明 |
|------|------|---------|------|
| **Form** | `<el-form>` | `model`, `rules`, `label-width`, `size` | 表单容器，管理验证 |
| **Input** | `<el-input>` | `v-model`, `type`, `placeholder`, `clearable`, `show-password`, `maxlength` | 文本输入 |
| **InputNumber** | `<el-input-number>` | `v-model`, `min`, `max`, `step`, `precision` | 数字输入 |
| **Select** | `<el-select>` | `v-model`, `multiple`, `filterable`, `remote`, `loading` | 下拉选择 |
| **Cascader** | `<el-cascader>` | `v-model`, `options`, `props`, `filterable` | 级联选择 |
| **Switch** | `<el-switch>` | `v-model`, `active-value`, `inactive-value` | 开关 |
| **Slider** | `<el-slider>` | `v-model`, `min`, `max`, `step`, `range` | 滑块 |
| **Radio / RadioGroup** | `<el-radio>` `<el-radio-group>` | `v-model`, `disabled` | 单选框 |
| **Checkbox / CheckboxGroup** | `<el-checkbox>` `<el-checkbox-group>` | `v-model`, `indeterminate` | 多选框 |
| **DatePicker** | `<el-date-picker>` | `v-model`, `type`, `range-separator`, `disabled-date` | 日期选择 |
| **TimePicker** | `<el-time-picker>` | `v-model`, `format`, `value-format` | 时间选择 |
| **TimeSelect** | `<el-time-select>` | `v-model`, `start`, `end`, `step` | 固定时间选择 |
| **ColorPicker** | `<el-color-picker>` | `v-model`, `predefine` | 颜色选择 |
| **Rate** | `<el-rate>` | `v-model`, `max`, `show-text` | 评分 |
| **Upload** | `<el-upload>` | `action`, `multiple`, `accept`, `file-list`, `before-upload`, `on-success`, `on-remove` | 文件上传 |
| **Transfer** | `<el-transfer>` | `v-model`, `data`, `titles`, `filterable` | 穿梭框 |
| **Autocomplete** | `<el-autocomplete>` | `v-model`, `fetch-suggestions`, `debounce` | 自动补全 |

---

### 五、数据展示组件

| 组件 | 标签 | 关键属性 | 说明 |
|------|------|---------|------|
| **Table** | `<el-table>` | `data`, `stripe`, `border`, `height`, `default-sort` | 数据表格（最常用） |
| **TableColumn** | `<el-table-column>` | `prop`, `label`, `width`, `formatter`, `sortable`, `fixed` | 表格列定义 |
| **Tree** | `<el-tree>` | `data`, `props`, `node-key`, `default-expanded-keys` | 树形控件 |
| **Virtualized Table** | `<el-table-v2>` | `columns`, `data`, `width`, `height` | 虚拟表格（大数据） |
| **Virtualized Tree** | `<el-tree-v2>` | `data`, `props`, `height` | 虚拟树（大数据） |
| **Descriptions** | `<el-descriptions>` | `column`, `border`, `title` | 详情展示 |
| **Avatar** | `<el-avatar>` | `size`, `shape`, `src`, `icon` | 头像 |
| **Badge** | `<el-badge>` | `value`, `max`, `is-dot`, `type` | 标记/徽章 |
| **Calendar** | `<el-calendar>` | `v-model` | 日历 |
| **Card** | `<el-card>` | `shadow`, `header` | 卡片容器 |
| **Carousel** | `<el-carousel>` | `autoplay`, `interval`, `type`, `indicator-position` | 走马灯/轮播图 |
| **Collapse** | `<el-collapse>` | `v-model`, `accordion` | 折叠面板 |
| **Image** | `<el-image>` | `src`, `fit`, `preview-src-list`, `lazy` | 图片（支持预览） |
| **Tag** | `<el-tag>` | `type`, `closable`, `size`, `hit`, `effect` | 标签 |
| **Timeline** | `<el-timeline>` | — | 时间线 |
| **Progress** | `<el-progress>` | `percentage`, `status`, `stroke-width`, `type` | 进度条 |
| **Result** | `<el-result>` | `title`, `sub-title`, `status`, `icon` | 结果页 |
| **Skeleton** | `<el-skeleton>` | `loading`, `animated`, `rows` | 骨架屏 |
| **Statistic** | `<el-statistic>` | `value`, `title`, `precision`, `prefix`, `suffix` | 统计数值 |

---

### 六、反馈组件

| 组件 | 调用方式 | 关键参数 | 说明 |
|------|---------|---------|------|
| **Dialog** | `<el-dialog>` | `v-model`, `title`, `width`, `fullscreen`, `close-on-click-modal` | 模态对话框 |
| **Drawer** | `<el-drawer>` | `v-model`, `title`, `direction`, `size`, `with-header` | 抽屉面板 |
| **Message** | `ElMessage(options)` | `message`, `type`, `duration`, `show-close` | **全局消息提示**（JS 调用） |
| **Notification** | `ElNotification(options)` | `title`, `message`, `type`, `position`, `duration` | **通知提醒**（JS 调用） |
| **MessageBox** | `ElMessageBox.confirm()` | `message`, `title`, `type`, `confirm-button-text` | **弹框确认**（JS 调用） |
| **Popconfirm** | `<el-popconfirm>` | `title`, `confirm-button-text`, `cancel-button-text` | 气泡确认 |
| **Popover** | `<el-popover>` | `content`, `trigger`, `placement`, `width` | 气泡卡片 |
| **Tooltip** | `<el-tooltip>` | `content`, `placement`, `effect`, `trigger` | 文字提示 |
| **Loading** | `ElLoading.service()` / `v-loading` | `target`, `text`, `fullscreen` | 加载指示器 |

---

### 七、其他

| 组件 | 说明 | 关键属性 |
|------|------|---------|
| **Affix** | 固钉 | `<el-affix>` `offset`, `target` |
| **Anchor** | 锚点 | `<el-anchor>` `<el-anchor-link>` |
| **ConfigProvider** | 全局配置 | `<el-config-provider>` `locale`, `size`, `namespace`, `button`, `message` |
| **Watermark** | 水印 | `<el-watermark>` `content`, `font`, `gap`, `offset` |

---

## 表单验证（重要）

Element Plus 内置表单验证（基于 async-validator）：

```vue
<template>
  <el-form ref="ruleFormRef" :model="ruleForm" :rules="rules" label-width="120px">
    <el-form-item label="用户名" prop="username">
      <el-input v-model="ruleForm.username" />
    </el-form-item>
    <el-form-item label="密码" prop="password">
      <el-input v-model="ruleForm.password" type="password" />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="submitForm(ruleFormRef)">提交</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'

const ruleFormRef = ref<FormInstance>()
const ruleForm = reactive({
  username: '',
  password: '',
})

const rules = reactive<FormRules>({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' },
  ],
})

// ✅ Element Plus 2.x 推荐：validate() 返回 Promise
const submitForm = async (formEl: FormInstance | undefined) => {
  if (!formEl) return
  try {
    await formEl.validate()
    // 验证通过，执行提交逻辑
  } catch (fields) {
    console.log('验证失败', fields)
  }
}
</script>
```

### 内置验证规则
- `required` — 必填
- `min` / `max` — 长度/数值范围
- `pattern` — 正则匹配
- `validator` — 自定义验证函数
- `trigger` — 触发方式（`'blur'` / `'change'`）

---

## 主题定制

### 方式一：CSS 变量覆盖（推荐，运行时可切换）

```css
/* 在 src/styles/element-variables.scss 中 */
:root {
  --el-color-primary: #409eff;
  --el-color-primary-light-3: #79bbff;
  --el-color-primary-light-5: #a0cfff;
  --el-color-primary-light-7: #c6e2ff;
  --el-color-primary-light-8: #d9ecff;
  --el-color-primary-dark-2: #337ecc;
  --el-color-success: #67c23a;
  --el-color-warning: #e6a23c;
  --el-color-danger: #f56c6c;
  --el-color-info: #909399;
  --el-border-radius-base: 4px;
  --el-font-size-base: 14px;
}
```

### 方式二：ConfigProvider 局部主题

```vue
<template>
  <!-- 给某段子树单独设置主题 -->
  <el-config-provider namespace="custom-theme">
    <el-button type="primary">自定义主题按钮</el-button>
  </el-config-provider>
</template>
```

### 方式三：SCSS 变量覆盖（需配置构建）

项目已配置 `sass-embedded`，可通过 SCSS 变量覆盖主题（注意语法与 Element UI 不同，需使用 CSS 变量方式）：

```scss
// styles/element-variables.scss
// Element Plus 推荐使用 CSS 变量覆盖（参见方式一），
// 如需 SCSS 覆盖，需配合 @use 引入主题包：
@forward 'element-plus/theme-chalk/src/common/var.scss' with (
  $colors: (
    'primary': (
      'base': #626aef,
    ),
  ),
  $border-radius: (
    'base': 6px,
  ),
);
```

---

## 国际化

Element Plus 内置多语言支持：

```ts
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

app.use(ElementPlus, { locale: zhCn })
```

若依项目中已在 `src/main.ts` 中配置国际化，一般无需额外处理。

---

## 在若依项目中的最佳实践

### 1. Table 的标准用法

```vue
<el-table v-loading="loading" :data="userList" @selection-change="handleSelectionChange">
  <el-table-column type="selection" width="55" />
  <el-table-column label="用户编号" align="center" prop="userId" />
  <el-table-column label="用户名称" align="center" prop="userName" />
  <el-table-column label="状态" align="center">
    <template #default="scope">
      <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
    </template>
  </el-table-column>
  <el-table-column label="创建时间" align="center" prop="createTime" width="180">
    <template #default="scope">
      <span>{{ parseTime(scope.row.createTime) }}</span>
    </template>
  </el-table-column>
  <el-table-column label="操作" align="center" width="200">
    <template #default="scope">
      <el-button type="primary" link size="small" @click="handleUpdate(scope.row)">修改</el-button>
      <el-button type="danger" link size="small" @click="handleDelete(scope.row)">删除</el-button>
    </template>
  </el-table-column>
</el-table>
```

### 2. 消息提示（JS 式调用）

```typescript
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'

// 成功提示
ElMessage.success('操作成功')
// 错误提示
ElMessage.error('操作失败')
// 警告提示
ElMessage.warning('请确认信息')

// 确认弹框
ElMessageBox.confirm('确认删除该记录？', '系统提示', {
  confirmButtonText: '确定',
  cancelButtonText: '取消',
  type: 'warning',
}).then(() => {
  // 确认后执行
}).catch(() => {
  // 取消
})

// 通知
ElNotification({
  title: '成功',
  message: '数据导入完成',
  type: 'success',
  duration: 3000,
})
```

### 3. 分页组件

若依已经对分页做了封装，使用 `<pagination>` 组件：
```vue
<pagination
  v-show="total > 0"
  :total="total"
  v-model:page="queryParams.pageNum"
  v-model:limit="queryParams.pageSize"
  @pagination="getList"
/>
```

### 4. 弹窗表单标准结构

```vue
<el-dialog :title="title" v-model="open" width="600px" append-to-body>
  <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
    <el-row>
      <el-col :span="12">
        <el-form-item label="字段一" prop="field1">
          <el-input v-model="form.field1" placeholder="请输入" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="字段二" prop="field2">
          <el-select v-model="form.field2" placeholder="请选择">
            <el-option v-for="item in options" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
  <template #footer>
    <el-button @click="cancel">取 消</el-button>
    <el-button type="primary" @click="submitForm">确 定</el-button>
  </template>
</el-dialog>
```

### 5. 空状态与加载

```vue
<!-- 表格加载（通过 #empty 插槽显示空状态） -->
<el-table v-loading="loading" :data="list">
  <template #empty>
    <el-empty description="暂无数据" />
  </template>
</el-table>

<!-- 按钮加载 -->
<el-button :loading="submitting" type="primary" @click="submit">提交中</el-button>
```

---

## ⚠️ 常见反模式与注意事项

### 1. ElMessageBox.confirm 的 Catch 陷阱

`ElMessageBox.confirm()` 取消时走 **catch**，确认时走 **then**。忽略 catch 会导致未捕获的 Promise rejection：

```typescript
// ✅ 正确
ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  .then(() => { /* 确认：执行删除 */ })
  .catch(() => { /* 取消：不做任何事 */ })

// ❌ 错误：不处理 catch，会在控制台报 Uncaught (in promise)
ElMessageBox.confirm('确认删除？')
  .then(() => { /* 删除 */ })
```

### 2. Table 大数据性能

当表格数据超过 ~500 行时，考虑：
- 使用 `<el-table-v2>` 虚拟化表格
- 或确保分页 `pageSize` 不超过 `100`

### 3. v-loading 指令的边界情况

同一页面多个 `v-loading` 元素时，使用 `v-loading` 的字符串参数区分目标：
```vue
<el-table v-loading="tableLoading" :data="list" element-loading-text="加载中..." />
```

### 4. Form 组件 `ref` 获取

在 `<script setup>` 中获取 `el-form` ref 时，ref 变量名必须与模板中 `ref` 属性值**完全一致**：
```vue
<el-form ref="ruleFormRef" ...>
<script setup>
const ruleFormRef = ref<FormInstance>()  // 变量名必须 = ref 值
</script>
```
