# AllinOne SQL 执行顺序

> 所有SQL文件位于 `sql/` 目录，必须按以下顺序执行，不可跳过或乱序。

---

## 执行顺序

| 序号 | 文件名 | 说明 | 依赖 |
|------|--------|------|------|
| 1 | `ry_20260417.sql` | RuoYi 基础框架表 (sys_*) | 无 |
| 2 | `quartz.sql` | Quartz 定时任务表 | 1 |
| 3 | `allinone_biz.sql` | AllinOne 业务表 (collect_*, report_*) | 1 |
| 4 | `jimureport.mysql5.7.create.sql` | JimuReport 积木报表表 | 1 |
| 5 | `allinone_biz_update.sql` | 报表管理系统增量 (work_report*) | 1, 3 |
| 6 | `allinone_menu.sql` | AllinOne 业务菜单与权限种子数据 (sys_menu) | 1 |

---

## 各文件详解

### 1. ry_20260417.sql — RuoYi 基础表
- `sys_dept` — 部门表
- `sys_user` / `sys_role` / `sys_menu` — 用户/角色/菜单
- `sys_config` / `sys_dict_*` / `sys_notice` — 配置/字典/通知
- `sys_oper_log` / `sys_logininfor` — 操作日志/登录日志
- `sys_job` / `sys_job_log` — 定时任务

**执行条件**: 全新项目首次部署

### 2. quartz.sql — Quartz 调度器
标准 Quartz 11 张表 (QRTZ_*)

### 3. allinone_biz.sql — AllinOne 业务表
| 表名 | 说明 |
|------|------|
| `collect_category` | 填报模板分类 |
| `collect_template` | 填报模板 |
| `collect_data` | 填报数据 (含 Luckysheet JSON) |
| `collect_data_cell` | 填报单元格数据 (Tier 2) |
| `collect_field_mapping` | 字段映射配置 |
| `report_category` | 报表分类 (JimuReport) |
| `report_config` | 报表配置 (JimuReport) |

### 4. jimureport.mysql5.7.create.sql — JimuReport
积木报表引擎所需表 (jimu_*)

### 5. allinone_biz_update.sql — 报表管理系统增量
| 表名 | 说明 | 功能 |
|------|------|------|
| `work_report` | 报表主表 | 报表管理容器 |
| `work_report_sheet` | Sheet 管理表 | 多 Sheet 支持 |
| `work_report_cell` | 单元格数据表 | 大规模数据优化 |
| `work_report_sheet_permission` | 显式权限分配表 | 双层权限模型 |

### 6. allinone_menu.sql — 业务菜单与权限种子
| 内容 | 说明 |
|------|------|
| `业务管理` / `报表中心` 目录及子菜单 | collect/report/work_report 各功能页面菜单 |
| 按钮权限（`collect:*` / `report:*`） | 与后端 `@PreAuthorize` 权限字符串一一对应 |
| 隐藏路由 | 编辑/详情/查看/大屏等跳转页路由 |

> 依赖 `sys_menu` 表（步骤 1）。使用 `INSERT IGNORE` 幂等插入，可重复执行。
> 不预置 `sys_role_menu` 关联：超级管理员自动可见全部菜单，普通角色需在「角色管理 → 分配菜单权限」中勾选。

---

## 快速执行 (MySQL)

> **重要：第 1 步必须用 `SOURCE` 方式在交互式会话中执行。** `ry_20260417.sql` 需要会话变量
> `@bootstrap_password_bcrypt` 来设置 admin 账号的初始密码（BCrypt 哈希）。用 `mysql < file`
> 的非交互式导入无法设置该变量，admin 会写入无效占位密码并保持**停用状态**，导致部署后无人能登录。
> 详见 [sql/README.md](../sql/README.md)。

```sql
-- 登录后按顺序执行（哈希请自行生成，勿提交到仓库）
mysql -u root -p allinone
```

```sql
SET @bootstrap_password_bcrypt = '<LOCAL_BCRYPT_HASH>';
SOURCE sql/ry_20260417.sql;
SOURCE sql/quartz.sql;
SOURCE sql/allinone_biz.sql;
SOURCE sql/jimureport.mysql5.7.create.sql;
SOURCE sql/allinone_biz_update.sql;
SOURCE sql/allinone_menu.sql;
```

第 2~6 步无会话变量要求，也可以用非交互方式执行：

```bash
mysql -u root -p allinone < sql/quartz.sql
mysql -u root -p allinone < sql/allinone_biz.sql
mysql -u root -p allinone < sql/jimureport.mysql5.7.create.sql
mysql -u root -p allinone < sql/allinone_biz_update.sql
mysql -u root -p allinone < sql/allinone_menu.sql
```

---

## 表依赖关系

```
sys_dept ──────┐
sys_user ──────┤
sys_role ──────┼── work_report (user_id, dept_id)
sys_menu ──────┘       │
                        ├── work_report_sheet (report_id, user_id, dept_id)
                        │       │
                        │       ├── work_report_cell (sheet_id)
                        │       │
                        │       └── work_report_sheet_permission (sheet_id)
                        │               │
                        │               ├── perm_type='role' → sys_role
                        │               ├── perm_type='dept' → sys_dept
                        │               └── perm_type='user' → sys_user
                        │
collect_template ───────┘ (现有数据填报体系，独立于 work_report*)
```

---

## 升级场景

### 已有 allinone_biz.sql 环境的增量升级
仅需执行第 5、6 步：
```bash
mysql -u root -p allinone < sql/allinone_biz_update.sql
mysql -u root -p allinone < sql/allinone_menu.sql
```

### 验证表是否创建成功
```sql
SHOW TABLES LIKE 'work_report%';
-- 预期输出:
-- work_report
-- work_report_cell
-- work_report_sheet
-- work_report_sheet_permission
```
