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
