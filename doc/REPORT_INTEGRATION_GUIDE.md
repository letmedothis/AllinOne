# 报表管理系统 (WorkReport) 集成指南

> 从 `reporting-system` 项目迁移至 `AllinOne/allinone-collect` 模块

---

## 1. 集成概述

将 reporting-system 的**报表管理系统定制功能 / Sheet 级数据权限隔离 / 大规模数据优化 / 显式权限分配（双层权限模型）**四部分集成到 AllinOne 的 `allinone-collect` 数据填报模块。

### 与现有体系的并存关系

```
AllinOne 报表生态:
  ├── JimuReport 通道 (allinone-report)
  │     └── report_config, report_category 表
  ├── 数据填报通道 (allinone-collect)
  │     ├── collect_data, collect_template 表        ← 单次填报表单
  │     └── work_report, work_report_sheet 表        ← 【新增】多Sheet协作报表
  └── Luckysheet 前端引擎 (allinone-typescript)
        ├── views/collect/data/  数据填报页面
        └── views/collect/report/ 报表管理页面         ← 【新增】
```

---

## 2. 数据库变更

### 执行方式
详见 [SQL_EXECUTION_ORDER.md](./SQL_EXECUTION_ORDER.md)，增量部署仅需：
```bash
mysql -u root -p allinone < sql/allinone_biz_update.sql
```

### 新增表 (4张)

| 表名 | 行数预估 | 核心索引 |
|------|---------|---------|
| `work_report` | ~千级 | PRIMARY KEY(`id`) |
| `work_report_sheet` | ~万级 | KEY `idx_report_id`(`report_id`) |
| `work_report_cell` | ~百万级 | UNIQUE `uk_cell`(`sheet_id`,`row_index`,`col_index`), KEY `idx_sheet_row` |
| `work_report_sheet_permission` | ~万级 | KEY `idx_sheet`(`sheet_id`), KEY `idx_target`(`perm_type`,`perm_id`) |

---

## 3. 后端文件清单

### allinone-collect 模块 (25 文件)

```
allinone-collect/src/main/java/com/allinone/collect/
├── domain/
│   ├── WorkReport.java                  # 报表主表实体
│   ├── WorkReportSheet.java             # Sheet 实体
│   ├── WorkReportCell.java              # 单元格实体
│   └── WorkReportSheetPermission.java   # 权限实体
├── mapper/
│   ├── WorkReportMapper.java
│   ├── WorkReportSheetMapper.java
│   ├── WorkReportCellMapper.java
│   └── WorkReportSheetPermissionMapper.java
├── service/
│   ├── IWorkReportService.java
│   ├── IWorkReportSheetService.java
│   ├── IWorkReportCellService.java
│   ├── IWorkReportSheetPermissionService.java
│   └── impl/
│       ├── WorkReportServiceImpl.java
│       ├── WorkReportSheetServiceImpl.java      # 双层权限查询核心
│       ├── WorkReportCellServiceImpl.java
│       └── WorkReportSheetPermissionServiceImpl.java
└── controller/
    └── WorkReportController.java                # Jackson版, 替代fastjson2

allinone-collect/src/main/resources/mapper/collect/
├── WorkReportMapper.xml
├── WorkReportSheetMapper.xml                    # selectAccessibleSheets 双层权限SQL
├── WorkReportCellMapper.xml                     # 范围查询 + ON DUPLICATE KEY UPDATE
└── WorkReportSheetPermissionMapper.xml
```

### API 端点汇总

| 方法 | 路径 | 权限标识 | 说明 |
|------|------|---------|------|
| GET | `/collect/report/list` | collect:report:list | 报表列表 |
| GET | `/collect/report/{id}` | collect:report:query | 报表详情 |
| POST | `/collect/report` | collect:report:add | 新增报表 |
| PUT | `/collect/report` | collect:report:edit | 修改报表 |
| DELETE | `/collect/report/{ids}` | collect:report:remove | 删除报表 |
| GET | `/collect/report/sheet/{reportId}` | collect:report:query | 获取Sheet元数据 |
| PUT | `/collect/report/sheet/{reportId}` | collect:report:edit | 保存Sheet数据 |
| GET | `/collect/report/cells` | collect:report:query | 范围内加载单元格 |
| PUT | `/collect/report/cells` | collect:report:edit | 批量保存单元格 |
| GET | `/collect/report/permissions/{sheetDbId}` | collect:report:query | 查看权限 |
| POST | `/collect/report/permissions/{sheetDbId}` | collect:report:edit | 授权 |
| DELETE | `/collect/report/permissions/{sheetDbId}` | collect:report:edit | 撤销权限 |

---

## 4. 前端文件清单

### allinone-typescript 模块

```
allinone-typescript/src/
├── api/collect/
│   └── report.ts                         # 报表API服务
└── views/collect/report/
    ├── index.vue                         # 报表列表页
    ├── editor.vue                        # Luckysheet编辑器 (含懒加载+脏检测)
    └── SheetPermissionDialog.vue         # 权限管理对话框
```

---

## 5. 四大功能技术说明

### 5.1 报表管理系统定制功能
- **表**: `work_report` (报表主表)
- **实体**: `WorkReport.java` — 支持 reportName/reportJianjie/reportBeizhu 字段
- **业务逻辑**: 带 `@DataScope` 的 CRUD，按创建时间倒序
- **前端**: `index.vue` 列表页，支持搜索、新增(弹窗)、删除

### 5.2 Sheet 级数据权限隔离
- **表**: `work_report_sheet` (user_id + dept_id)
- **核心SQL**: `selectAccessibleSheets` — 三层 OR 逻辑
  1. 管理员 → 全部可见
  2. `@DataScope` 条件 → 部门数据权限
  3. 自己创建的 Sheet (`user_id = currentUserId`)
  4. 被显式分配了权限
- **实现**: `WorkReportSheetServiceImpl.buildDataScopeCondition()` 手动构建 DataScope 条件片段
- **作用**: 非管理员用户只能看到自己有权访问的 Sheet

### 5.3 大规模数据优化
- **表**: `work_report_cell` — 每个单元格独立存储
- **关键技术**:
  - **范围查询**: `SELECT ... WHERE row_index BETWEEN ? AND ? AND col_index BETWEEN ?` — 分片加载避免全量传输
  - **批量 Upsert**: `ON DUPLICATE KEY UPDATE` — 单条 SQL 插入/更新数千单元格
  - **前端懒加载**: `editor.vue` 中 `loadedRanges` 去重 + `cellSnapshots` 脏检测
- **保存策略**: 仅保存变更单元格 (`collectDirtyCells()`)

### 5.4 显式权限分配（双层权限模型）
- **表**: `work_report_sheet_permission` (sheet_id + perm_type + perm_id)
- **第一层 (隐式)**: `@DataScope` — 基于部门/角色的自动权限
- **第二层 (显式)**: 创建者手动分配 role/dept/user 三级权限
- **控制规则**: 仅创建者和管理员可分配/撤销权限
- **前端**: `SheetPermissionDialog.vue` — 支持角色/部门/用户三级添加和移除
- **API**: grant/revoke/list 三个端点

---

## 6. 部署检查清单

- [ ] 数据库执行 `sql/allinone_biz_update.sql`
- [ ] 在 `sys_menu` 表中新增报表管理菜单，权限标识 `collect:report:*`
- [ ] Vue Router 添加路由 `/collect/report/editor/:id` → `views/collect/report/editor.vue`
- [ ] 确认前端已引入 Luckysheet 运行时 (CDN 或 npm)
- [ ] 执行 `mvn compile` 验证 allinone-collect 模块编译通过
- [ ] 清除浏览器缓存后测试报表 CRUD 流程
- [ ] 测试双层权限：管理员/创建者/被授权用户 三种角色访问 Sheet

---

## 7. 关键设计决策

| 决策 | 理由 |
|------|------|
| 表名沿用 `work_report*` 不改为 `collect_report*` | 与 JimuReport(report_config) 和 CollectData 各自语义独立 |
| Controller 放在 allinone-collect | 遵循 AllinOne 现有惯例 (CollectDataController 同模块) |
| 新增 `work_report_cell` 独立表 | 与 `collect_data_cell` 键结构不同，避免两种访问模式冲突 |
| Jackson 替代 fastjson2 | AllinOne 未引入 fastjson2 依赖 |
| 传统 getter/setter 替代 Lombok | AllinOne 未使用 Lombok |
