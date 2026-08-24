---
name: vue3
description: Vue 3 官方指南 — Composition API、<script setup>、TypeScript、响应式系统、路由 Pinia、组合式函数
metadata:
  type: reference
---

# Vue 3 开发 Skill

> 🟢 **集成状态：核心框架** (v3.5.26) | 关联：[Element Plus](element-plus.md) · [RuoYi-Vue](ruoyi-vue.md) · [Luckysheet](luckysheet.md)

## 概述

Vue 3 是当前主流的渐进式 JavaScript 框架。本项目 (`allinone-typescript`) 使用 **Vue 3.5.26 + TypeScript + Vite 6 + Pinia + Vue Router 4** 构建前端。

**官方资源：**
- 文档: https://cn.vuejs.org/guide/introduction.html
- API: https://cn.vuejs.org/api/
- GitHub: https://github.com/vuejs/core

---

## 项目中的 Vue 3 版本与依赖

本项目实际使用的 Vue 生态版本：

| 依赖 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5.26 | 核心框架 |
| TypeScript | 5.6.3 | 类型系统 |
| Vite | 6.4.1 | 构建工具 |
| Vue Router | 4.6.4 | 路由管理 |
| Pinia | 3.0.4 | 状态管理 |
| Element Plus | 2.13.1 | UI 组件库 |
| @vueuse/core | 14.1.0 | 组合式工具集 |

---

## 一、<script setup> — 推荐写法

本项目所有 Vue 组件统一使用 `<script setup lang="ts">` 语法：

```vue
<script setup lang="ts">
// 所有代码写在这里，无需 export default
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

// 响应式数据
const count = ref(0)
// 函数
const increment = () => count.value++
// 生命周期
onMounted(() => {
  console.log('组件已挂载')
})
</script>

<template>
  <button @click="increment">{{ count }}</button>
</template>
```

### 核心编译宏（无需 import，直接使用）

| 宏 | 作用 |
|----|------|
| `defineProps` | 声明 props |
| `defineEmits` | 声明 emits |
| `defineModel` | 双向绑定 v-model |
| `defineExpose` | 暴露方法给父组件 |
| `defineOptions` | 设置组件选项 |

---

## 二、响应式核心

### ref vs reactive — 推荐全用 ref

```typescript
import { ref, reactive, computed, watch, watchEffect } from 'vue'

// ✅ ref（推荐——统一访问方式）
const count = ref(0)                    // 基本类型
const user = ref({ name: '张三', age: 18 }) // 对象
// 模板中自动解包：{{ count }}
// JS 中需 .value：count.value++

// reactive（仅对象）
const state = reactive({ list: [] })
// 直接访问：state.list.push(...)
// ⚠️ 解构会丢失响应性，需用 toRefs

// 计算属性
const double = computed(() => count.value * 2)

// 侦听器
watch(count, (newVal, oldVal) => {
  console.log(`变化: ${oldVal} -> ${newVal}`)
})

// 自动收集依赖的侦听器
watchEffect(() => {
  console.log(`count 现在是 ${count.value}`)
})
```

### 响应式工具函数

| 函数 | 说明 |
|------|------|
| `shallowRef()` | 浅层响应式，性能更好（大数据列表场景） |
| `toRef(obj, key)` | 将响应式对象的某个属性转为 ref |
| `toRefs(obj)` | 将响应式对象的所有属性转为 ref（解构时用） |
| `readonly(obj)` | 只读代理，不可修改 |
| `isRef(val)` | 判断是否为 ref |
| `unref(val)` | val 是 ref 则返回 .value，否则返回自身 |

---

## 三、组件通信

### 1. props — 父传子

```vue
<!-- 父组件 -->
<ChildComponent title="标题" :count="count" />

<!-- 子组件：<script setup lang="ts"> -->
const props = defineProps<{
  title: string
  count?: number           // 可选
  status: 'active' | 'inactive'  // 联合类型
}>()
// 使用时：props.title, props.count
```

### 2. emit — 子传父

```vue
<!-- 子组件 -->
const emit = defineEmits<{
  update: [value: string]    // [参数类型元组]
  delete: [id: number]
}>()
// 调用：emit('update', '新值')

<!-- 父组件 -->
<ChildComponent @update="handleUpdate" @delete="handleDelete" />
```

### 3. v-model 双向绑定（支持多个）

```vue
<!-- 子组件：defineModel 宏（Vue 3.4+） -->
const title = defineModel<string>({ required: true })
const visible = defineModel<boolean>('visible', { default: false })
// 使用：title.value = '新值'

<!-- 父组件 -->
<ChildComponent v-model="title" v-model:visible="showDialog" />
```

### 4. provide / inject — 跨层级

```typescript
// 祖先组件
import { provide, ref } from 'vue'
const theme = ref('dark')
provide('theme', theme)
provide('updateTheme', (newTheme: string) => { theme.value = newTheme })

// 后代组件
import { inject } from 'vue'
const theme = inject('theme', ref('light'))  // 第二个参数是默认值
const updateTheme = inject<(t: string) => void>('updateTheme')
```

### 5. defineExpose — 暴露给父组件

```vue
<script setup lang="ts">
const reset = () => { /* ... */ }
defineExpose({ reset })
</script>

<!-- 父组件通过 ref 调用 -->
<ChildComponent ref="childRef" />
<script setup>
const childRef = ref()
childRef.value?.reset()
</script>
```

---

## 四、组合式函数 (Composables)

Vue 3 的核心设计模式：**把逻辑拆成函数，按需组合。**

### 标准写法（项目中的 composables 放在 `src/composables/` 目录）

```typescript
// src/composables/useCounter.ts
import { ref, computed, type Ref } from 'vue'

export function useCounter(initialValue = 0) {
  const count = ref(initialValue)
  const double = computed(() => count.value * 2)

  const increment = () => count.value++
  const decrement = () => count.value--
  const reset = () => { count.value = initialValue }

  return { count, double, increment, decrement, reset }
}
```

### 常用的组合式函数模式

```typescript
// useFetch — 异步数据请求
export function useFetch<T>(url: string) {
  const data = ref<T | null>(null)
  const error = ref<Error | null>(null)
  const loading = ref(true)

  onMounted(async () => {
    try {
      const res = await fetch(url)
      data.value = await res.json()
    } catch (e) {
      error.value = e as Error
    } finally {
      loading.value = false
    }
  })

  return { data, error, loading }
}
```

```typescript
// usePage — 若依风格的页面分页逻辑
export function usePage<T>(fetchFn: (params: any) => Promise<TableResult<T>>) {
  const loading = ref(false)
  const list = ref<T[]>([])
  const total = ref(0)
  const queryParams = reactive({
    pageNum: 1,
    pageSize: 10,
  })

  const getList = async () => {
    loading.value = true
    try {
      const res = await fetchFn(queryParams)
      list.value = res.rows
      total.value = res.total
    } finally {
      loading.value = false
    }
  }

  return { loading, list, total, queryParams, getList }
}
```

### Composables 最佳实践

- ✅ 以 `use` 开头命名（如 `useMouse`, `useAuth`）
- ✅ 可接收参数，可返回响应式数据
- ✅ 在 `onMounted` / `onUnmounted` 中管理副作用
- ✅ 组合多个 composables 构建复杂逻辑
- ✅ 返回的对象优先用 ref（而非 reactive），以便解构
- ❌ 不要在 composable 外部使用生命周期钩子

---

## 五、生命周期

| Vue 2 (选项式) | Vue 3 (选项式) | Vue 3 (组合式 API) |
|---------------|----------------|-------------------|
| beforeCreate | beforeCreate | ❌ 直接用 setup |
| created | created | ❌ 直接用 setup |
| beforeMount | beforeMount | `onBeforeMount` |
| mounted | mounted | `onMounted` |
| beforeUpdate | beforeUpdate | `onBeforeUpdate` |
| updated | updated | `onUpdated` |
| beforeDestroy | beforeUnmount | `onBeforeUnmount` |
| destroyed | unmounted | `onUnmounted` |
| — | activated | `onActivated` (keep-alive) |
| — | deactivated | `onDeactivated` (keep-alive) |

```typescript
import {
  onMounted, onBeforeMount, onUpdated,
  onBeforeUpdate, onUnmounted, onBeforeUnmount,
  onActivated, onDeactivated, onErrorCaptured,
} from 'vue'
```

---

## 六、内置指令

| 指令 | 语法 | 说明 |
|------|------|------|
| `v-text` | `v-text="msg"` | 更新文本内容 |
| `v-html` | `v-html="html"` | 更新 HTML（注意 XSS 风险） |
| `v-show` | `v-show="isVisible"` | 切换 display |
| `v-if` | `v-if="cond"` | 条件渲染（销毁/重建） |
| `v-else` | `v-else` | 与 v-if 配合 |
| `v-else-if` | `v-else-if="cond"` | 多重条件 |
| `v-for` | `v-for="(item, i) in list" :key="item.id"` | 列表渲染（**必须带 :key**） |
| `v-on` / `@` | `@click="handler"` | 事件绑定 |
| `v-bind` / `:` | `:src="url"` | 动态绑定属性 |
| `v-model` | `v-model="value"` | 双向绑定 |
| `v-slot` / `#` | `#default="{ item }"` | 插槽 |
| `v-pre` | `v-pre` | 跳过编译 |
| `v-once` | `v-once` | 只渲染一次 |
| `v-memo` | `v-memo="[dep]"` | 条件缓存（Vue 3.2+） |
| `v-cloak` | `v-cloak` | 编译前隐藏 |

### 自定义指令

```typescript
// 局部指令（命名需要 v 前缀）
const vFocus = {
  mounted: (el: HTMLElement) => el.focus(),
}

// 全局指令（在 main.ts 中注册）
app.directive('focus', {
  mounted: (el: HTMLElement) => el.focus(),
})
```

---

## 七、插槽 (Slots)

```vue
<!-- 子组件定义插槽 -->
<template>
  <div class="card">
    <header>
      <slot name="header" :title="title" />   <!-- 具名 + 作用域插槽 -->
    </header>
    <main>
      <slot :data="items" />                   <!-- 默认插槽 + 作用域 -->
    </main>
    <footer>
      <slot name="footer" />
    </footer>
  </div>
</template>

<!-- 父组件使用插槽 -->
<CardComponent>
  <template #header="{ title }">
    <h2>{{ title }}</h2>
  </template>
  <template #default="{ data }">
    <li v-for="item in data" :key="item.id">{{ item.name }}</li>
  </template>
  <template #footer>
    <p>页脚内容</p>
  </template>
</CardComponent>
```

### 插槽类型标注（Vue 3.3+）

```typescript
defineSlots<{
  default(props: { msg: string }): any
  header(props: { title: string }): any
}>()
```

---

## 八、Teleport 与 Suspense

### Teleport — 传送门

将内容渲染到 DOM 的指定位置（常用于弹窗、提示）：

```vue
<template>
  <Teleport to="body">
    <div class="modal-overlay">
      <div class="modal">
        <slot />
      </div>
    </div>
  </Teleport>
</template>
```

`to` 属性可以是 CSS 选择器字符串（`"body"`、`"#app"`）或 DOM 元素。

### Suspense — 异步依赖处理

```vue
<template>
  <Suspense>
    <template #default>
      <!-- 异步组件或有 async setup 的组件 -->
      <AsyncDashboard />
    </template>
    <template #fallback>
      <el-skeleton :rows="5" animated />
    </template>
  </Suspense>
</template>
```

> ✅ `Suspense` 在 Vue 3.5+ 中已转为**稳定特性**，可放心在生产环境中使用。

---

## 九、TypeScript 集成

本项目使用的是 `allinone-typescript`（TypeScript 版本），类型支持尤为重要。

### 组件 Props 类型

```typescript
// ✅ 纯类型声明（推荐）
defineProps<{
  title: string
  count?: number
  items: Item[]
  onChange?: (id: number) => void
}>()

// 带默认值的 props
interface Props {
  modelValue: string
  placeholder?: string
}
const props = withDefaults(defineProps<Props>(), {
  placeholder: '请输入',
})
```

### Emits 类型

```typescript
// ✅ 类型声明
const emit = defineEmits<{
  change: [value: string]
  'update:modelValue': [value: string]
  delete: [id: number]
}>()

// 或者使用回调类型标注
const emit = defineEmits<{
  (e: 'change', value: string): void
  (e: 'update:modelValue', value: string): void
}>()
```

### ref 类型标注

```typescript
const count = ref(0)              // Ref<number>
const name = ref('')              // Ref<string>
const list = ref<Item[]>([])      // Ref<Item[]>
const compRef = ref<InstanceType<typeof MyComponent> | null>(null)
```

---

## 十、Vue Router 4

### 在项目中的用法

```typescript
import { useRouter, useRoute } from 'vue-router'
import type { RouteLocationNormalizedLoaded } from 'vue-router'

const router = useRouter()
const route = useRoute()

// 导航
router.push('/dashboard')
router.push({ path: '/user', query: { id: '123' } })
router.replace({ name: 'Login' })
router.back()

// 路由信息
console.log(route.path)       // 当前路径
console.log(route.query)      // 查询参数
console.log(route.params)     // 动态参数
console.log(route.name)       // 路由名称
console.log(route.meta)       // 元数据
```

### 导航守卫

```typescript
import { onBeforeRouteLeave, onBeforeRouteUpdate } from 'vue-router'

// 离开前确认（注意：Vue Router 4 已移除 next 回调）
onBeforeRouteLeave((to, from) => {
  const answer = window.confirm('有未保存的内容，确定离开？')
  if (!answer) return false  // return false 取消导航
})
```

---

## 十一、Pinia 状态管理

### Pinia 支持两种写法

| 写法 | 优点 | 本项目 |
|------|------|--------|
| **Options Store** (`state`/`actions`) | 结构清晰，类似 Vuex 迁移成本低 | ✅ **项目实际使用**（8个 Store 全部采用此风格） |
| **Setup Store** (`setup` 函数) | 更灵活，组合式写法 | 推荐用于新 Store |

### 本项目中的 Pinia Store（Options API 写法 — 实际采用）

```typescript
// src/store/modules/user.ts — 项目实际写法
import { defineStore } from 'pinia'
import { login, logout, getInfo } from '@/api/login'

const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    name: '',
    roles: [] as string[],
  }),
  actions: {
    async login(userInfo: LoginData) {
      const res = await login(userInfo.username, userInfo.password, userInfo.code, userInfo.uuid)
      setToken(res.token)
      this.token = res.token
    },
    async logOut() {
      await logout()
      this.token = ''
      this.roles = []
      removeToken()
    }
  }
})
```

> **注意：** 本项目保持 Options Store 风格以维持一致性。新建 Store 时**选择与现有文件一致的风格**。

### 两种写法对比参考

```typescript
// === Options Store（项目当前风格）===
export const useUserStore = defineStore('user', {
  state: () => ({ token: '' }),
  actions: {
    async login(data: LoginData) {
      const res = await login(data)
      this.token = res.token   // Options 中 this 访问 state
    }
  }
})

// === Setup Store（Vue3 Skill 推荐，新 store 可参考）===
export const useUserStore = defineStore('user', () => {
  const token = ref('')
  const login = async (data: LoginData) => {
    const res = await loginApi(data)
    token.value = res.token    // Setup 中 .value 访问
  }
  return { token, login }
})
```

### Pinia 核心概念

| 概念 | 说明 |
|------|------|
| `defineStore(id, options)` | 定义 store，id 唯一 |
| `storeToRefs(store)` | 解构 store 时保持响应性 |
| `store.$patch({ })` | 批量更新 |
| `store.$reset()` | 重置 state（仅 Options Store 支持） |
| `store.$subscribe(callback)` | 监听 state 变化 |

---

## 十二、与若依项目的 Vue 特殊用法

### 若依路由 — 前端解析后端菜单树

若依的路由是**由后端返回的菜单树动态生成**的，非手动编写。前端 `src/router/index.ts` 负责解析：

```typescript
// 若依内置的 router 模块 (src/router/index.ts)
// 路由根据后端返回的菜单配置动态添加
// 你的页面只需在 src/views/ 下创建，并通过菜单管理配置路由
// 无需手动写 router.addRoute()
```

### 若依封装的全局组件

这些组件在项目模板中可直接使用（已全局注册）：

| 组件 | 用途 |
|------|------|
| `<pagination>` | 分页组件 |
| `<dict-tag>` | 字典标签 |
| `<svg-icon>` | SVG 图标 |
| `<right-toolbar>` | 表格工具栏 |
| `<file-upload>` | 文件上传 |
| `<image-upload>` | 图片上传 |
| `<image-preview>` | 图片预览 |
| `<parent-view>` | 内嵌页面 |

### 若依提供的全局函数

```typescript
// 工具函数（在模板中可直接使用）
parseTime(time, pattern)       // 时间格式化
resetForm(refName)             // 表单重置（传入 el-form 的 ref 名称）
addDateRange(params, dateRange)     // 日期范围参数追加
handleTree(data, id, parentId)      // 构建树结构
selectDictLabel(dict, value)        // 字典值转标签
selectDictLabels(dict, value, sep)  // 多值字典转标签（逗号分隔）
tansParams(params)                  // 参数序列化
parseStrEmpty(str)                  // null/undefined 转空字符串
getNormalPath(path)                 // 规范化路径
sprintf(str, ...args)               // 字符串格式化 %s
```
