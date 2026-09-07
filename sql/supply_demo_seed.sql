-- 供应链采购审批 Demo 初始化数据
-- 使用前请设置：SET @supply_supervisor_user_id = <主管用户ID>;
-- 发票双审还需设置：SET @supply_purchase_user_id = <采购员ID>;
--                     SET @supply_finance_user_id = <财务用户ID>;
-- 不会创建或修改 sys_user，审批人必须是已存在且启用的业务用户。

INSERT INTO company_profile (id, name, tax_id, timezone, currency, revision, create_time, update_time)
SELECT 1, '演示采购公司', 'DEMOBUYER000000001', 'Asia/Shanghai', 'CNY', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM company_profile WHERE id = 1);

INSERT INTO warehouses (id, name, enabled, create_by, create_time)
SELECT 1, '演示主仓库', '1', 'demo', NOW()
WHERE NOT EXISTS (SELECT 1 FROM warehouses WHERE id = 1);

INSERT INTO workflow_configs (id, flow_type, version, supervisor_candidates, finance_candidates, updated_at)
SELECT 1, 'SUPPLIER_ONBOARDING', 1, CAST(@supply_supervisor_user_id AS CHAR), NULL, NOW()
WHERE @supply_supervisor_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM workflow_configs WHERE flow_type = 'SUPPLIER_ONBOARDING');

INSERT INTO workflow_configs (id, flow_type, version, supervisor_candidates, finance_candidates, updated_at)
SELECT 2, 'PURCHASE_ORDER_APPROVAL', 1, CAST(@supply_supervisor_user_id AS CHAR), NULL, NOW()
WHERE @supply_supervisor_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM workflow_configs WHERE flow_type = 'PURCHASE_ORDER_APPROVAL');

INSERT INTO workflow_configs (id, flow_type, version, supervisor_candidates, finance_candidates, updated_at)
SELECT 3, 'INVOICE_APPROVAL', 1, CAST(@supply_purchase_user_id AS CHAR), CAST(@supply_finance_user_id AS CHAR), NOW()
WHERE @supply_purchase_user_id IS NOT NULL AND @supply_finance_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM workflow_configs WHERE flow_type = 'INVOICE_APPROVAL');
