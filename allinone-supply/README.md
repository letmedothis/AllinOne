# 供应链采购审批 Demo

本模块承载供应商、采购订单、入库、发票、审批和台账功能。

## 初始化演示数据

先执行 `sql/allinone_biz.sql`（新库）或 `sql/allinone_biz_update.sql`（已有库），再根据现有系统用户设置审批人：

```sql
SET @supply_supervisor_user_id = 主管用户ID;
SET @supply_purchase_user_id = 采购员用户ID;
SET @supply_finance_user_id = 财务用户ID;
SOURCE sql/supply_demo_seed.sql;
```

`supply_demo_seed.sql` 不创建用户、不修改用户角色，也不会把管理员自动设置为业务审批人。

## 主要接口

- `/supply/suppliers`：供应商准入
- `/supply/orders`：采购订单
- `/supply/receipts`：分批入库
- `/supply/invoices`：发票登记和 XML 解析
- `/supply/approvals`：审批待办
- `/supply/ledger`：综合台账
- `/supply/dashboard`：工作台统计
- `/supply/config/workflows`：流程候选人配置

当前 XML 仅支持需求文档附录中的 `DemoInvoice schemaVersion="1.0"`，不代表真实税务发票验真。

## 采购订单审批引擎

- 采购订单提交后进入 **Flowable 引擎**（`documents.workflow_engine='FLOWABLE'`）；供应商与发票仍走**内置固定流程**（`LEGACY`），由原审批中心处理。
- 提交时在 `documents.workflow_engine` 记录归属；撤回/作废据此同步取消 Flowable 运行实例，避免幽灵待办。
- 历史在途采购订单（`LEGACY`）仍由旧「审批中心」菜单处理；审批通过退回后重新提交才会进入 Flowable。
- Flowable 引擎表 `ACT_*` 由应用首次启动自动创建，生产初始化后应设 `FLOWABLE_DATABASE_SCHEMA_UPDATE=false`（见 `sql/README.md`）。
