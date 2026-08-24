---
name: ruoyi-vue
description: RuoYi-Vue 若依框架前后端分离快速开发平台 — 基于 Spring Boot 3 + Vue 3 + Element Plus + TypeScript
metadata:
  type: reference
---

# RuoYi-Vue (若依框架) 开发 Skill

> 🟢 **集成状态：核心框架** (v3.9.2) | 关联：[Vue3](vue3.md) · [Element Plus](element-plus.md) · [Luckysheet](luckysheet.md) · [JimuReport](jimureport.md) · [Warm-Flow](warm-flow.md)

## 项目概述

这是基于 **RuoYi-Vue 3.9.2** 的 AllinOne 报表系统项目。后端使用 Spring Boot 3 + JDK 17，前端使用 Vue 3 + TypeScript + Vite + Element Plus + Pinia。

**官方资源：**
- 文档: https://doc.ruoyi.vip/ruoyi-vue/
- 官网: https://ruoyi.vip/
- 演示: http://vue.ruoyi.vip

---

## 技术栈

### 后端 (Spring Boot 3.x)
| 技术 | 版本/说明 |
|------|-----------|
| Spring Boot | 3.5.14 |
| JDK | 17+ |
| MyBatis | Spring Boot 3 Starter |
| 数据库连接池 | Druid 1.2.28 |
| 分页 | PageHelper |
| Redis | 缓存支持 |
| JWT | 登录认证 |
| SpringDoc | API 文档生成 |
| Oshi | 服务器监控 |

### 前端 (Vue 3 + TypeScript)
| 技术 | 版本 |
|------|------|
| Vue | 3.5.26 |
| Vite | 6.4.1 |
| TypeScript | 5.6.3 |
| Element Plus | 2.13.1 |
| Pinia | 3.0.4 |
| Vue Router | 4.6.4 |
| ECharts | 5.6.0 |
| Axios | 1.13.2 |

---

## 项目模块结构

```
allinone-admin      # 后台管理模块（Controller 层，主启动类）
allinone-common     # 公共工具模块（BaseEntity、AjaxResult、注解、工具类）
allinone-framework  # 核心框架配置（Security、JWT、权限、全局异常处理）
allinone-generator  # 代码生成器模块
allinone-quartz     # 定时任务模块（Quartz）
allinone-system     # 系统业务模块（Service、Mapper、XML）
allinone-typescript # 前端 Vue3 + TypeScript 项目
allinone-collect    # 🆕 数据填报模块（模板管理、数据填报、工作报表）
allinone-report     # 🆕 报表管理模块（报表配置、分类管理、JimuReport 集成）
allinone-luckysheet # 🆕 本地 Luckysheet 构建工程（UMD 产物供前端引用）
```

### 各模块职责

| 模块 | 包路径 | 职责 |
|------|--------|------|
| allinone-collect | `com.allinone.collect` | 填报模板 CRUD、表单数据采集、Luckysheet 存储、数据回写、工作报表协同 |
| allinone-report | `com.allinone.report` | JimuReport 报表配置、报表分类、报表查看 |
| allinone-luckysheet | — | Luckysheet 源码构建、UMD 打包、`file:` 依赖供前端引用 |

---

## 后端开发规范

### 1. 目录分层（Controller -> Service -> Mapper -> XML）

```java
// Controller: 路径 /collect/xxx，统一使用 @RestController + @RequestMapping
@RestController
@RequestMapping("/collect/xxx")
public class XxxController extends BaseController {
    @Autowired
    private IXxxService xxxService;
}

// Service: 接口 + 实现分离
public interface IXxxService { }
@Service
public class XxxServiceImpl implements IXxxService { }

// Mapper: MyBatis 接口
@Mapper
public interface XxxMapper { }
```

### 2. 通用注解

```java
@Log(title = "功能名称", businessType = BusinessType.INSERT)  // 日志记录
@PreAuthorize("@ss.hasPermi('system:xxx:add')")               // 权限控制
@DataScope(deptAlias = "d", userAlias = "u")                  // 数据权限
@RepeatSubmit                                                    // 防重复提交
```

### 3. 分页查询

```java
@PreAuthorize("@ss.hasPermi('system:xxx:list')")
@GetMapping("/list")
public TableDataInfo list(Xxx xxx) {
    startPage();  // 自动分页
    List<Xxx> list = xxxService.selectXxxList(xxx);
    return getDataTable(list);
}
```

### 4. 返回结果

```java
return success(data);           // AjaxResult.success()
return error("错误信息");        // AjaxResult.error()
return toAjax(rows);           // 增删改返回影响行数
return getDataTable(list);     // 分页返回
```

### 5. 代码生成器

代码生成器是 RuoYi 的核心效率工具：
1. 在数据库中创建业务表
2. 在系统菜单 → 代码生成 → 导入表
3. 编辑字段（显示类型、查询方式、表单类型、字典类型等）
4. 生成代码 → 下载 ZIP → 按目录结构解压到项目对应模块
5. 重新运行即可看到生成的 CRUD 功能

**生成器配置要点：**
- 表结构需包含约定的基础字段（如 `create_by`, `create_time`, `update_by`, `update_time`, `remark`）
- 若依自动生成的代码覆盖：Controller、Service、Mapper、XML、Vue 页面、SQL 脚本
- 支持树表、主子表（一对多）等复杂结构

### 6. 数据权限

角色管理支持 5 种数据权限范围：
1. 全部数据权限
2. 自定数据权限
3. 本部门数据权限
4. 本部门及以下数据权限
5. 仅本人数据权限

使用 `@DataScope` 注解 + Mapper XML 中配置 `${params.dataScope}` 实现。

### 7. 定时任务

基于 Quartz 框架实现。通过 `@Component` 注册任务 Bean，由 `ScheduleUtils` 管理 Quartz Job 的创建、启动、暂停和删除：
- 支持 cron 表达式在线配置
- 支持任务执行失败重试
- 支持调用失败策略（立即执行/执行一次/放弃执行）
- 任务执行历史记录存储在 `qrtz_` 前缀的 Quartz 表中
- 在系统管理 → 定时任务菜单中可在线管理任务

### 8. ⚠️ 异常处理规范

**原则：不静默吞噬异常，至少记录日志**

```java
// ✅ 正确：记录 warn 日志，不影响主流程继续
try {
    dataWriteBackService.writeBack(data);
} catch (Exception e) {
    log.warn("数据回写失败 dataId={}", dataId, e);
}

// ✅ 正确：记录日志后向上抛（让全局异常处理器统一返回前端）
try {
    // ...
} catch (Exception e) {
    log.error("业务处理异常", e);
    throw new ServiceException("操作失败：" + e.getMessage());
}

// ❌ 错误：空 catch 块，线上问题无法排查
try { ... } catch (Exception e) { }

// ❌ 错误：只 e.printStackTrace()，生产日志中找不到
try { ... } catch (Exception e) { e.printStackTrace(); }
```

**Service 层抛出的业务异常使用 `ServiceException`**（位于 `com.allinone.common.exception`），由 `GlobalExceptionHandler` 统一处理返回前端。

### 9. ⚠️ 事务管理规范

```java
// ✅ 事务注解正确用法：指定 rollbackFor
@Transactional(rollbackFor = Exception.class)

// ❌ 错误：不指定 rollbackFor，checked exception 不会回滚
@Transactional

// ✅ 需要注意事务的方法：
// - 同时操作多张表的写操作
// - 先删子表再删主表的操作
// - 涉及文件/外部系统的复合操作

// ❌ 不需要事务的方法：
// - 纯查询（select/get/list）
// - 单表单条记录的简单 insert/update
```

**特别注意：** `@Transactional` 仅在通过 Spring 代理调用时生效，类内部直接调用（this.xxx()）不会触发事务。

### 10. ⚠️ 日志记录规范

```java
// 统一使用 SLF4J
private static final Logger log = LoggerFactory.getLogger(XxxServiceImpl.class);

// 日志级别选择：
log.error("需要立即处理的问题", e);    // 影响业务功能的错误
log.warn("不影响主流程的异常", e);     // 可恢复的异常、降级处理
log.info("关键业务节点");              // 提交流程、状态变更等
log.debug("调试信息");                 // 变量值、SQL 参数等（生产环境关闭）

// 日志必须包含上下文：用户、数据ID、操作类型
log.warn("数据回写失败 userId={} dataId={}", userId, dataId, e);
```

**何时用 `@Log` 注解 vs `Logger`：**
- `@Log`：Controller 层记录操作日志（存数据库，用于操作审计）
- `Logger`：Service 层记录运行日志（存文件，用于问题排查）

---

## 前端开发规范

### 1. 页面结构

```vue
<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryRef" ...>
      <el-form-item>
        <el-input v-model="queryParams.xxx" />
        <el-button type="primary" @click="handleQuery">搜索</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-button type="primary" @click="handleAdd">新增</el-button>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="xxxList">
      <el-table-column prop="xxx" label="字段" />
    </el-table>

    <!-- 分页 -->
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" />

    <!-- 新增/修改弹窗 -->
    <el-dialog :title="title" v-model="open" width="500px">
      <el-form ref="xxxRef" :model="form" :rules="rules">
        ...
      </el-form>
      <template #footer>
        <el-button @click="cancel">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
```

### 2. 核心 API 调用

```typescript
// 使用封装的 request（基于 axios）
import { listXxx, getXxx, delXxx, addXxx, updateXxx } from "@/api/system/xxx";

// 分页查询
export function listXxx(query: XxxQuery): Promise<TableResult<XxxVO>> {
  return request({ url: '/system/xxx/list', method: 'get', params: query });
}

// 导出
export function exportXxx(query: XxxQuery): Promise<AxiosResponse<Blob>> {
  return request({ url: '/system/xxx/export', method: 'get', params: query, responseType: 'blob' });
}
```

**本项目自定义 API 路径约定：**
| 业务 | API 目录 | 后端路径前缀 |
|------|---------|-------------|
| 数据填报 | `src/api/collect/` | `/collect/` |
| 报表管理 | `src/api/report/` | `/report/` |

### 3. 常用组件使用

| 组件 | 用途 |
|------|------|
| `<pagination>` | 表格分页（已全局注册） |
| `<el-dialog>` | 弹窗表单 |
| `<el-form>` + `rules` | 表单验证 |
| `<el-table>` + `selection-change` | 多选操作 |
| `<dict-tag>` | 字典值显示标签 |
| `<svg-icon>` | 图标组件 |
| `<file-upload>` | 若依封装的多文件上传组件 |
| `<image-upload>` | 若依封装的图片上传组件 |
| `<right-toolbar>` | 展示切换/刷新组件 |
| `<parent-view>` | 内嵌页面 |
| `<CollectSheet>` | 🆕 本项目封装的 Luckysheet 填报组件 |
| `<ReportFrame>` | 🆕 本项目封装的 JimuReport iframe 组件 |

### 4. 字典用法

```vue
<!-- 字典标签显示 -->
<dict-tag :options="sys_xxx_status" :value="row.status" />

<!-- 字典下拉选择 -->
<el-select v-model="form.status">
  <el-option v-for="dict in sys_xxx_status" :key="dict.value" :label="dict.label" :value="dict.value" />
</el-select>
```

### 5. 路由与菜单

在 `src/router/index.ts` 中配置：
- 路由 `path` 对应菜单配置中的路由地址
- `component` 对应 `views/` 下的页面路径
- `meta.title` 对应菜单名称
- 若依自动根据菜单配置动态生成路由

### 6. ⚠️ 错误处理规范（前端）

```typescript
// ✅ 正确：catch 中显示用户友好的错误提示
try {
  await service.save(data)
  proxy.$modal.msgSuccess('保存成功')
} catch (e) {
  proxy.$modal.msgError('保存失败，请稍后重试')
  console.error('保存失败:', e)  // console.error 保留用于开发排查
}

// ❌ 错误：捕获异常但不给用户任何反馈
try {
  await service.save(data)
} catch (e) { }

// ❌ 错误：使用 console.log 打印生产日志
console.log('操作成功')
```

---

## 数据表设计规范

### 基础字段约定

```sql
create_by    varchar(64)   comment '创建者',
create_time  datetime      comment '创建时间',
update_by    varchar(64)   comment '更新者',
update_time  datetime      comment '更新时间',
remark       varchar(500)  comment '备注',
```

若依代码生成器默认以上述字段结尾的表为"业务表"，会正确解析生成。

### 表前缀说明

| 前缀 | 说明 | 模块 |
|------|------|------|
| `sys_` | 系统管理相关表 | allinone-system |
| `gen_` | 代码生成相关表 | allinone-generator |
| `qrtz_` | 定时任务相关表 | allinone-quartz |
| `collect_` | 🆕 数据填报相关表 | allinone-collect |
| `report_` | 🆕 报表配置相关表 | allinone-report |
| `biz_` | 🆕 数据回写目标业务表 | 自定义 |

自定义业务表建议使用业务相关前缀。

---

## 常见开发场景

### 场景一：新增业务模块（代码生成器路径）
1. 建表 → 导入代码生成器 → 编辑字段配置 → 生成代码
2. 将生成的 main 目录代码解压到对应的 allinone-xxx 模块
3. 将生成的 Vue 页面放到 allinone-typescript/src/views/ 下
4. 执行生成的 SQL 脚本添加菜单和权限
5. 重启后端，刷新前端即可

### 场景二：添加自定义接口
1. Controller 添加 `@PreAuthorize` 权限控制
2. Service 添加业务逻辑
3. Mapper + XML 添加 SQL 映射
4. 前端 api/ 目录添加调用方法
5. Vue 页面调用

### 场景三：集成第三方库
1. 后端：在 `allinone-common/pom.xml` 或模块自己的 `pom.xml` 添加依赖
2. 前端：使用 `yarn add <package>` 安装
3. 按需在前端 `src/` 中封装使用

### 场景四：前端新建页面
1. 在 `src/views/` 下创建页面目录和 `.vue` 文件
2. 在 `src/api/` 下创建对应的 API 调用
3. 通过系统菜单 → 菜单管理添加菜单配置（无需编码注册路由）
4. 分配权限给角色

### 场景五：新建数据填报模板
1. 建表 → 使用代码生成器生成基本 CRUD
2. 在模板编辑页集成 `<CollectSheet>` 组件
3. 在 Service 添加 `submitData()` 提交逻辑（JSON 解析 + 数据回写）
4. 参考 `CollectDataServiceImpl` 的 Tier 2/Tier 3 架构实现
