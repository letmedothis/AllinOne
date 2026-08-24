---
name: luckysheet
description: Luckysheet 纯前端在线电子表格组件 — 类 Excel 浏览器端表格，支持公式、图表、协同编辑、导入导出
metadata:
  type: reference
---

# Luckysheet 在线电子表格开发 Skill

> 🟢 **集成状态：已完全集成** (v2.1.13, 本地构建) | 关联：[JimuReport](jimureport.md) · [Warm-Flow](warm-flow.md) · [Vue3](vue3.md)

## 概述

Luckysheet 是一款**纯前端**、类似 Excel 的在线电子表格组件，功能强大、配置简单、完全开源。通过 npm 包 `luckysheet` 安装使用，可与 Vue 3 项目无缝集成。

> ⚠️ **注意：** Luckysheet 2.x 官方仓库已**停止维护（archived）**，最后一次发布为 v2.1.13。新项目建议评估 [Univer](https://github.com/dream-num/univer)（原团队的新一代方案）或 SheetJS 作为替代。本 skill 仍适用于维护既有 Luckysheet 功能的项目。

**官方资源：**
- 文档: https://dream-num.github.io/LuckysheetDocs/zh/guide/
- GitHub: https://github.com/mengshukeji/Luckysheet
- 后续替代方案: https://github.com/dream-num/univer
- 当前稳定版本: 2.1.13

---

## 安装与集成

### 本项目的特殊架构：本地构建 + file: 依赖

本项目采用**两步式架构**，与其他 npm 包直接安装不同：

```
allinone-luckysheet/              ← 源码工程（基于 Luckysheet v2.1.13）
  ├── src/                        ← 修改后的源码
  ├── dist/                       ← 📦 预构建的 UMD 产物（已提交 Git）
  └── package.json                ← 构建脚本

allinone-typescript/              ← 前端主工程
  └── package.json                ← "luckysheet": "file:../allinone-luckysheet"
```

**工作原理：**
1. `allinone-luckysheet/dist/` 包含预构建的 UMD 产物（`luckysheet.umd.js` + CSS/插件）
2. `allinone-typescript` 通过 `file:` 协议直接引用本地目录，npm install 自动软链接
3. 前端组件中使用 `import luckysheet from 'luckysheet'` 和 `import 'luckysheet/dist/css/...'`

**与标准 npm 安装的关键区别：**
- ❌ 不是 `npm install luckysheet`（官方 v2.1.13 已停止维护）
- ✅ `"luckysheet": "file:../allinone-luckysheet"` — 本地构建产物
- ✅ 修改 `src/` 后需重新 `npm run build` 才能生效

### 在本项目 (Vue 3 + TypeScript + Vite) 中安装

```bash
# Luckysheet 作为本地 file: 依赖引入（已在 package.json 中配置）
cd allinone-typescript
npm install
```

> **构建说明：** 本项目 Luckysheet 的 UMD 产物已预置于 `dist/`，`allinone-typescript` 通过 `file:` 依赖直接引用。
> 如果修改了 `src/` 下的源码，需要重新打包才能生效：
> ```bash
> cd allinone-luckysheet
> npm install
> npm run build
> ```
> 构建完成后重启前端 dev server 即可。

### 完整引入 (在组件中使用)

```vue
<template>
  <div id="luckysheet" ref="sheetContainer" style="width:100%;height:100vh;"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import 'luckysheet/dist/plugins/css/pluginsCss.css'
import 'luckysheet/dist/plugins/plugins.css'
import 'luckysheet/dist/css/luckysheet.css'
import 'luckysheet/dist/assets/iconfont/iconfont.css'
import luckysheet from 'luckysheet'

onMounted(() => {
  luckysheet.create({
    container: 'luckysheet',
    showinfobar: true,
    showtoolbar: true,
    showstatisticBar: true,
    allowEdit: true,
    lang: 'zh',
    title: '电子表格',
    // 更多配置...
  })
})

onUnmounted(() => {
  luckysheet.destroy()
})
</script>
```

---

## 全部配置选项

```typescript
interface LuckysheetOptions {
  container: string;           // 容器 DOM id
  title?: string;              // 表格标题
  lang?: string;               // 语言: 'zh' | 'en' | 'es' | ...
  allowEdit?: boolean;         // 是否允许编辑 (默认 true)
  forceCalculation?: boolean;  // 是否强制重新计算公式
  data?: SheetData[];          // 工作表数据数组
  // 工具栏配置
  showtoolbar?: boolean;       // 是否显示工具栏
  toolbarConfig?: ToolbarItem[]; // 自定义工具栏
  showinfobar?: boolean;       // 是否显示公式栏
  showstatisticBar?: boolean;  // 是否显示底部统计栏
  sheetBottomConfig?: boolean; // 是否显示底部工作表标签
  // 功能配置
  enableAddRow?: boolean;      // 允许增加行
  enableAddCol?: boolean;      // 允许增加列
  enablePage?: boolean;        // 允许多 sheet
  pageSize?: number;           // 每页显示行数
  // 样式
  rowHeight?: number;          // 默认行高
  columnWidth?: number;        // 默认列宽
  column?: { ch?: number; en?: number }[]; // 自定义列宽
  // 钩子事件
  hook?: LuckysheetHook;       // 事件钩子
}
```

---

## 工作表数据格式

```typescript
interface SheetData {
  name: string;                // 工作表名称
  color?: string;              // 标签颜色
  status?: number;             // 0=隐藏, 1=显示
  order?: number;              // 排序索引
  index?: number;              // 唯一索引
  row?: number;                // 行数
  column?: number;             // 列数
  defaultRowHeight?: number;   // 默认行高 (px)
  defaultColumnWidth?: number; // 默认列宽 (px)
  celldata?: CellData[];       // 单元格数据 (数组格式)
  config?: SheetConfig;        // 合并单元格、边框等配置
}

interface CellData {
  r: number;     // 行号 (0-based)
  c: number;     // 列号 (0-based)
  v: CellValue;  // 单元格值
}

interface CellValue {
  v?: string | number;        // 原始值
  m?: string;                 // 显示值
  f?: string;                 // 公式 (如 '=sum(A1:A5)')
  bg?: string;                // 背景色
  fc?: string;                // 字体颜色
  bl?: number;                // 是否加粗 (0/1)
  fs?: number;                // 字号
  it?: number;                // 是否斜体 (0/1)
  cl?: number;                // 设置删除线 (0/1)
  ht?: number;                // 水平对齐 (0=居中,1=左,2=右)
  vt?: number;                // 垂直对齐 (0=居中,1=上,2=下)
  tb?: number;                // 换行 (0=截断,1=溢出,2=自动换行)
  ct?: { fa?: string; t?: string }; // 单元格类型（fa=格式代码, t=类型）
}
```

---

## API 参考（完整版）

### 工作簿操作

| API | 说明 |
|-----|------|
| `luckysheet.create(options)` | **初始化创建表格** |
| `luckysheet.refresh()` | 刷新 Canvas |
| `luckysheet.destroy()` | **销毁表格释放资源**（组件卸载时必调） |
| `luckysheet.getScreenshot([setting])` | 导出选区截图（Base64） |

### 工作表操作

| API | 说明 |
|-----|------|
| `luckysheet.getAllSheets()` | 获取所有工作表数据 |
| `luckysheet.getLuckysheetfile()` | 获取工作表调试信息 |
| `luckysheet.getSheetData()` | 获取当前活动工作表数据 |
| `luckysheet.setSheetAdd([setting])` | 新增工作表 |
| `luckysheet.setSheetDelete([setting])` | 删除当前工作表 |
| `luckysheet.setSheetHide([setting])` | 隐藏当前工作表 |
| `luckysheet.setSheetShow(index)` | 显示指定工作表 |
| `luckysheet.setSheetName(name, [setting])` | 重命名工作表 |
| `luckysheet.setSheetColor(color, [setting])` | 设置标签颜色 |
| `luckysheet.setSheetOrder(orderList)` | 调整工作表顺序 |
| `luckysheet.setSheetActivate(order)` | 激活指定工作表 |
| `luckysheet.setSheetCopy([setting])` | 复制工作表 |

### 单元格操作

| API | 说明 |
|-----|------|
| `luckysheet.getCellValue(row, col, [setting])` | 获取单元格值 |
| `luckysheet.setCellValue(row, col, value, [setting])` | 设置单元格值（支持公式） |
| `luckysheet.clearCell(row, col, [setting])` | 清除单元格内容 |
| `luckysheet.deleteCell(move, row, col, [setting])` | 删除单元格（`move: 'left'/'up'`） |
| `luckysheet.setCellFormat(row, col, attr, value, [setting])` | 设置单元格属性 |
| `luckysheet.exitEditMode([setting])` | 退出编辑模式 |

### 行列操作

| API | 说明 |
|-----|------|
| `luckysheet.setHorizontalFrozen(isRange, [setting])` | 冻结行 |
| `luckysheet.setVerticalFrozen(isRange, [setting])` | 冻结列 |
| `luckysheet.setBothFrozen(isRange, [setting])` | 同时冻结行列 |
| `luckysheet.cancelFrozen([setting])` | 取消冻结 |
| `luckysheet.insertRow(row, [number], [setting])` | 插入行 |
| `luckysheet.insertColumn(column, [number], [setting])` | 插入列 |
| `luckysheet.deleteRow(rowStart, rowEnd, [setting])` | 删除行 |
| `luckysheet.deleteColumn(colStart, colEnd, [setting])` | 删除列 |
| `luckysheet.hideRow(rowStart, rowEnd, [setting])` | 隐藏行 |
| `luckysheet.hideColumn(colStart, colEnd, [setting])` | 隐藏列 |
| `luckysheet.showRow(rowStart, rowEnd, [setting])` | 显示行 |
| `luckysheet.showColumn(colStart, colEnd, [setting])` | 显示列 |
| `luckysheet.setRowHeight(row, height, [setting])` | 设置行高 |
| `luckysheet.setColumnWidth(col, width, [setting])` | 设置列宽 |

### 选区操作

| API | 说明 |
|-----|------|
| `luckysheet.getRange()` | 获取当前选区 `{row:[], column:[]}` |
| `luckysheet.getRangeWithFlatten()` | 获取扁平化选区（每单元格独立） |
| `luckysheet.getRangeAxis()` | 获取选区坐标字符串 `["A1:B2"]` |
| `luckysheet.getRangeValue([setting])` | 获取选区内值 |
| `luckysheet.setRangeShow(range, [setting])` | 高亮显示选区 |
| `luckysheet.mergeCell(range, [setting])` | 合并单元格 |
| `luckysheet.unMergeCell(range, [setting])` | 取消合并单元格 |

### 数据操作

| API | 说明 |
|-----|------|
| `luckysheet.find(content, [setting])` | 查找内容（支持正则） |
| `luckysheet.replace(content, replaceContent, [setting])` | 查找并替换 |
| `luckysheet.sort(range, type, order)` | 排序（type: 'all'/'cell'） |
| `luckysheet.filter(range)` | 筛选 |

### 图表操作

| API | 说明 |
|-----|------|
| `luckysheet.getChart(chartId)` | 获取图表配置 |
| `luckysheet.getChartData(chartId)` | 获取图表数据 |
| `luckysheet.setChart(chartOption)` | 创建/设置图表 |

---

## 事件钩子（Hook）

```typescript
interface LuckysheetHook {
  // 单元格
  cellUpdated?: (row: number, col: number, oldVal: any, newVal: any) => void;
  cellRenderBefore?: (cell: any, position: any) => void;
  cellRenderAfter?: (cell: any, position: any) => void;

  // 工作表
  sheetCreated?: (index: number) => void;
  sheetMoved?: (oldOrder: number, newOrder: number) => void;
  sheetDelete?: (index: number) => void;
  sheetActivate?: (index: number) => void;
  sheetDeactivate?: (index: number) => void;

  // 选区
  rangeSelect?: (range: any) => void;

  // 工作簿
  workbookCreated?: () => void;
  workbookDestroyed?: () => void;

  // 编辑
  editModeEnter?: () => void;
  editModeExit?: () => void;

  // 滚动
  scroll?: (position: { scrollLeft: number; scrollTop: number }) => void;

  // 更新后
  updated?: () => void;
}
```

---

## 导入/导出 Excel

### 导入 Excel（需引入 LuckyExcel）

```ts
// 引入 LuckyExcel 工具
import LuckyExcel from 'luckysheet/dist/plugins/js/plugin.js'

// 文件上传后
function handleImport(file: File) {
  LuckyExcel.transformExcelToLucky(file, (exportJson: any) => {
    luckysheet.create({
      container: 'luckysheet',
      data: exportJson.sheets,
      title: exportJson.info.name,
    })
  })
}
```

### 导出 Excel

```ts
import { exportExcel } from 'luckysheet/dist/plugins/js/plugin.js'

function handleExport() {
  const sheets = luckysheet.getAllSheets()
  exportExcel(sheets, '导出文件名.xlsx')
}
```

---

## 协同编辑

Luckysheet 支持协同编辑，但需要自行搭建 WebSocket 服务端：
1. 客户端通过 WebSocket 发送 pako 压缩的操作数据
2. 服务端广播给其他客户端
3. 客户端接收并应用操作

**实现要点：**
- 使用 `cellUpdated` 钩子捕获变化
- 通过 WebSocket 同步操作日志
- 使用 `luckysheet.setCellValue()` 应用远端变更
- 注意操作冲突处理（OT 算法参考）

---

## 常见场景示例

### 场景一：初始化带数据的表格

```ts
luckysheet.create({
  container: 'luckysheet',
  data: [{
    name: 'Sheet1',
    row: 100,
    column: 26,
    celldata: [
      { r: 0, c: 0, v: { v: '姓名', m: '姓名' } },
      { r: 0, c: 1, v: { v: '年龄', m: '年龄' } },
      { r: 1, c: 0, v: { v: '张三', m: '张三' } },
      { r: 1, c: 1, v: { v: 28, m: '28' } },
    ]
  }]
})
```

### 场景二：读取表格全部数据

```ts
const allData = luckysheet.getAllSheets()
const activeSheet = luckysheet.getSheetData()
// 遍历单元格
activeSheet.celldata?.forEach(cell => {
  console.log(`(${cell.r}, ${cell.c}) = ${cell.v?.v}`)
})
```

### 场景三：联动保存（定期自动保存）

```ts
// 每 30 秒自动保存
setInterval(() => {
  const data = luckysheet.getAllSheets()
  // 发送到后端保存
  axios.post('/api/spreadsheet/save', data)
}, 30000)
```

### 场景四：Vue 组件封装

```vue
<template>
  <div :id="containerId" style="width:100%;height:100%;"></div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, watch } from 'vue'
import luckysheet from 'luckysheet'

const props = withDefaults(defineProps<{
  containerId?: string
  data?: any[]
  readonly?: boolean
}>(), {
  containerId: 'luckysheet-container',
  data: () => [{ name: 'Sheet1' }],
  readonly: false,
})

const emit = defineEmits<{
  cellChange: [row: number, col: number, oldVal: any, newVal: any]
  sheetChange: [sheets: any[]]
}>()

onMounted(() => {
  luckysheet.create({
    container: props.containerId,
    data: props.data,
    allowEdit: !props.readonly,
    hook: {
      cellUpdated: (r, c, oldV, newV) => emit('cellChange', r, c, oldV, newV),
    },
  })
})

onUnmounted(() => {
  luckysheet.destroy()
})
</script>
```
