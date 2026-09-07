-- ============================================================
-- AllinOne 业务菜单与权限种子数据
-- 依赖：必须先执行 ry_20260417.sql（sys_menu 表结构）
-- 幂等：使用 INSERT IGNORE，可重复执行
-- 菜单ID段：2100-2199（ry_20260417.sql 已占用 1-1060，避开现有业务菜单）
-- ============================================================

-- ------------------------------------------------------------
-- 顶层目录
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_menu VALUES
(2000, '数据填报', 0, 1, 'collect', NULL, '', 'BusinessManage', 1, 0, 'M', '0', '0', '', 'example', 'admin', sysdate(), '', NULL, '数据填报目录'),
(2001, '报表管理', 0, 2, 'report', NULL, '', 'ReportCenter', 1, 0, 'M', '0', '0', '', 'component', 'admin', sysdate(), '', NULL, '报表配置/查看/大屏目录'),
(2100, '供应链管理', 0, 3, 'supply', NULL, '', 'SupplyChain', 1, 0, 'M', '0', '0', '', 'shopping', 'admin', sysdate(), '', NULL, '供应链采购审批与票据台账目录');

-- ------------------------------------------------------------
-- 供应链基础页面
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_menu VALUES
(2106, '供应链工作台', 2100, 0, 'dashboard', 'supply/dashboard/index', '', 'SupplyDashboard', 1, 0, 'C', '0', '0', 'supply:dashboard:view', 'dashboard', 'admin', sysdate(), '', NULL, '供应链工作台'),
(2101, '供应商管理', 2100, 1, 'supplier', 'supply/supplier/index', '', 'SupplySupplier', 1, 0, 'C', '0', '0', 'supply:supplier:list', 'peoples', 'admin', sysdate(), '', NULL, '供应商准入管理菜单'),
(2102, '采购订单', 2100, 2, 'order', 'supply/order/index', '', 'SupplyOrder', 1, 0, 'C', '0', '0', 'supply:order:list', 'shopping', 'admin', sysdate(), '', NULL, '采购订单管理菜单'),
(2103, '审批中心', 2100, 3, 'approval', 'supply/approval/index', '', 'SupplyApproval', 1, 0, 'C', '0', '0', 'supply:approval:list', 'audit', 'admin', sysdate(), '', NULL, '供应链审批中心'),
(2104, '入库管理', 2100, 4, 'receipt', 'supply/receipt/index', '', 'SupplyReceipt', 1, 0, 'C', '0', '0', 'supply:receipt:add', 'box', 'admin', sysdate(), '', NULL, '分批入库管理'),
(2105, '发票管理', 2100, 5, 'invoice', 'supply/invoice/index', '', 'SupplyInvoice', 1, 0, 'C', '0', '0', 'supply:invoice:query', 'tickets', 'admin', sysdate(), '', NULL, '发票登记与解析'),
(2107, '综合台账', 2100, 6, 'ledger', 'supply/ledger/index', '', 'SupplyLedger', 1, 0, 'C', '0', '0', 'supply:ledger:list', 'list', 'admin', sysdate(), '', NULL, '采购入库发票综合台账'),
(2108, '流程配置', 2100, 7, 'workflow-config', 'supply/config/workflow', '', 'SupplyWorkflowConfig', 1, 0, 'C', '0', '0', 'supply:config:workflow', 'edit', 'admin', sysdate(), '', NULL, '供应链审批候选人配置'),
(2109, '流程待办', 2100, 8, 'workflow-tasks', 'supply/workflow/tasks', '', 'SupplyWorkflowTasks', 1, 0, 'C', '0', '0', 'supply:workflow:task', 'list', 'admin', sysdate(), '', NULL, 'Flowable 采购订单流程待办'),
(2111, '流程设计', 2100, 9, 'workflow-designer', 'supply/workflow/index', '', 'SupplyWorkflowDesigner', 1, 0, 'C', '0', '0', 'supply:workflow:list', 'guide', 'admin', sysdate(), '', NULL, 'Flowable BPMN 流程设计与发布'),
(2110, '供应商详情', 2100, 10, 'supplier/detail', 'supply/supplier/detail', '', 'SupplySupplierDetail', 1, 0, 'C', '1', '0', 'supply:supplier:query', '#', 'admin', sysdate(), '', NULL, '供应商详情页（隐藏）'),
(2112, '流程详情', 2100, 11, 'workflow/detail', 'supply/workflow/detail', '', 'SupplyWorkflowDetail', 1, 0, 'C', '1', '0', 'supply:workflow:task', '#', 'admin', sysdate(), '', NULL, 'Flowable 流程实例详情');

INSERT IGNORE INTO sys_menu VALUES
(2120, '供应商查询', 2101, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:supplier:query', '#', 'admin', sysdate(), '', NULL, ''),
(2121, '供应商新增', 2101, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:supplier:add', '#', 'admin', sysdate(), '', NULL, ''),
(2122, '供应商修改', 2101, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:supplier:edit', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_menu VALUES
(2130, '订单查询', 2102, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:order:query', '#', 'admin', sysdate(), '', NULL, ''),
(2131, '订单新增', 2102, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:order:add', '#', 'admin', sysdate(), '', NULL, ''),
(2132, '订单修改', 2102, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:order:edit', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_menu VALUES
(2140, '审批查询', 2103, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:approval:list', '#', 'admin', sysdate(), '', NULL, ''),
(2141, '审批通过', 2103, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:approval:approve', '#', 'admin', sysdate(), '', NULL, ''),
(2142, '审批退回', 2103, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:approval:reject', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_menu VALUES
(2150, '台账导出', 2107, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:ledger:export', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_menu VALUES
(2160, '入库查询', 2104, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:receipt:query', '#', 'admin', sysdate(), '', NULL, ''),
(2161, '入库确认', 2104, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:receipt:add', '#', 'admin', sysdate(), '', NULL, ''),
(2170, '发票查询', 2105, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:invoice:query', '#', 'admin', sysdate(), '', NULL, ''),
(2171, '发票新增', 2105, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:invoice:add', '#', 'admin', sysdate(), '', NULL, ''),
(2172, '发票修改', 2105, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:invoice:edit', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_menu VALUES
(2180, '流程定义查看', 2111, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:workflow:list', '#', 'admin', sysdate(), '', NULL, ''),
(2181, '流程定义发布', 2111, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:workflow:deploy', '#', 'admin', sysdate(), '', NULL, ''),
(2182, '流程待办处理', 2109, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:workflow:task', '#', 'admin', sysdate(), '', NULL, '');

-- ------------------------------------------------------------
-- 业务管理子菜单
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_menu VALUES
(2002, '分类管理', 2000, 1, 'category', 'collect/category/index', '', 'CollectCategory', 1, 0, 'C', '0', '0', 'collect:category:list', 'list', 'admin', sysdate(), '', NULL, '分类管理菜单'),
(2003, '填报模板', 2000, 2, 'template', 'collect/template/index', '', 'CollectTemplate', 1, 0, 'C', '0', '0', 'collect:template:list', 'form', 'admin', sysdate(), '', NULL, '填报模板菜单'),
(2004, '我的填报', 2000, 3, 'data', 'collect/data/index', '', 'CollectData', 1, 0, 'C', '0', '0', 'collect:data:list', 'clipboard', 'admin', sysdate(), '', NULL, '我的填报菜单'),
(2050, '字段映射', 2000, 5, 'mapping', 'collect/mapping/index', '', 'CollectFieldMapping', 1, 0, 'C', '0', '0', '', 'excel', 'admin', sysdate(), '', NULL, '字段映射菜单（Tier 3 数据回写配置）');

-- ------------------------------------------------------------
-- 报表中心子菜单
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_menu VALUES
(2006, '报表配置', 2001, 1, 'config', 'report/config/index', '', 'ReportConfig', 1, 0, 'C', '0', '0', 'report:config:list', 'documentation', 'admin', sysdate(), '', NULL, '报表配置菜单'),
(2007, '报表分类', 2001, 2, 'category', 'report/category/index', '', 'ReportCategory', 1, 0, 'C', '0', '0', 'report:category:list', 'tree', 'admin', sysdate(), '', NULL, '报表分类菜单'),
(2017, '大屏管理', 2001, 3, 'dashboardList', 'report/dashboard/list', '', 'ReportDashboardList', 1, 0, 'C', '0', '0', 'report:config:list', 'monitor', 'admin', sysdate(), '', NULL, '大屏卡片列表菜单');

-- ------------------------------------------------------------
-- 隐藏路由（供页面跳转使用，不在侧边栏显示）
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_menu VALUES
(2010, '填报数据编辑', 2000, 10, 'data/edit', 'collect/data/edit', '', 'CollectDataEdit', 1, 0, 'C', '1', '0', 'collect:data:query,collect:data:edit', '#', 'admin', sysdate(), '', NULL, '填报数据编辑页(隐藏)'),
(2011, '填报数据详情', 2000, 11, 'data/detail', 'collect/data/detail', '', 'CollectDataDetail', 1, 0, 'C', '1', '0', 'collect:data:query', '#', 'admin', sysdate(), '', NULL, '填报数据详情页(隐藏)'),
(2012, '填报模板编辑', 2000, 12, 'template/edit', 'collect/template/edit', '', 'CollectTemplateEdit', 1, 0, 'C', '1', '0', 'collect:template:edit', '#', 'admin', sysdate(), '', NULL, '填报模板编辑页(隐藏)'),
(2014, '报表查看', 2001, 10, 'view', 'report/view/index', '', 'ReportView', 1, 0, 'C', '1', '0', 'report:config:query', '#', 'admin', sysdate(), '', NULL, '报表查看页(隐藏)'),
(2015, '大屏查看', 2001, 11, 'dashboard', 'report/dashboard/index', '', 'ReportDashboard', 1, 0, 'C', '1', '0', 'report:config:query', '#', 'admin', sysdate(), '', NULL, '大屏查看页(隐藏)'),
(2016, '报表配置独立编辑', 2001, 12, 'config/edit', 'report/config/edit', '', 'ReportConfigEdit', 1, 0, 'C', '1', '0', 'report:config:edit', '#', 'admin', sysdate(), '', NULL, '报表配置独立编辑页(隐藏)');

-- ------------------------------------------------------------
-- 填报分类按钮权限
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_menu VALUES
(2020, '分类新增', 2002, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'collect:category:add', '#', 'admin', sysdate(), '', NULL, ''),
(2021, '分类修改', 2002, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'collect:category:edit', '#', 'admin', sysdate(), '', NULL, ''),
(2022, '分类删除', 2002, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'collect:category:remove', '#', 'admin', sysdate(), '', NULL, '');

-- ------------------------------------------------------------
-- 填报模板按钮权限
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_menu VALUES
(2023, '模板查询', 2003, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'collect:template:query', '#', 'admin', sysdate(), '', NULL, ''),
(2024, '模板新增', 2003, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'collect:template:add', '#', 'admin', sysdate(), '', NULL, ''),
(2025, '模板修改', 2003, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'collect:template:edit', '#', 'admin', sysdate(), '', NULL, ''),
(2026, '模板删除', 2003, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'collect:template:remove', '#', 'admin', sysdate(), '', NULL, ''),
(2027, '模板导出', 2003, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'collect:template:export', '#', 'admin', sysdate(), '', NULL, '');

-- ------------------------------------------------------------
-- 填报数据按钮权限
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_menu VALUES
(2028, '数据查询', 2004, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'collect:data:query', '#', 'admin', sysdate(), '', NULL, ''),
(2029, '数据新增', 2004, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'collect:data:add', '#', 'admin', sysdate(), '', NULL, ''),
(2030, '数据修改', 2004, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'collect:data:edit', '#', 'admin', sysdate(), '', NULL, ''),
(2031, '数据删除', 2004, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'collect:data:remove', '#', 'admin', sysdate(), '', NULL, ''),
(2032, '数据导出', 2004, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'collect:data:export', '#', 'admin', sysdate(), '', NULL, '');

-- ------------------------------------------------------------
-- 字段映射按钮权限
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_menu VALUES
(2045, '映射列表', 2050, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'collect:mapping:list', '#', 'admin', sysdate(), '', NULL, ''),
(2046, '映射查询', 2050, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'collect:mapping:query', '#', 'admin', sysdate(), '', NULL, ''),
(2047, '映射新增', 2050, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'collect:mapping:add', '#', 'admin', sysdate(), '', NULL, ''),
(2048, '映射修改', 2050, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'collect:mapping:edit', '#', 'admin', sysdate(), '', NULL, ''),
(2049, '映射删除', 2050, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'collect:mapping:remove', '#', 'admin', sysdate(), '', NULL, '');

-- ------------------------------------------------------------
-- 报表配置按钮权限
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_menu VALUES
(2038, '配置查询', 2006, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'report:config:query', '#', 'admin', sysdate(), '', NULL, ''),
(2039, '配置新增', 2006, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'report:config:add', '#', 'admin', sysdate(), '', NULL, ''),
(2040, '配置修改', 2006, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'report:config:edit', '#', 'admin', sysdate(), '', NULL, ''),
(2041, '配置删除', 2006, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'report:config:remove', '#', 'admin', sysdate(), '', NULL, '');

-- ------------------------------------------------------------
-- 报表分类按钮权限
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_menu VALUES
(2042, '分类新增', 2007, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'report:category:add', '#', 'admin', sysdate(), '', NULL, ''),
(2043, '分类修改', 2007, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'report:category:edit', '#', 'admin', sysdate(), '', NULL, ''),
(2044, '分类删除', 2007, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'report:category:remove', '#', 'admin', sysdate(), '', NULL, '');

-- ------------------------------------------------------------
-- 供应链-付款管理(M4)
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_menu VALUES
(2130, '付款管理', 2100, 12, 'payment', 'supply/payment/index', '', 'SupplyPayment', 1, 0, 'C', '0', '0', 'supply:payment:query', 'money', 'admin', sysdate(), '', NULL, '采购付款与应付核销'),
(2131, '付款查询', 2130, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:payment:query', '#', 'admin', sysdate(), '', NULL, ''),
(2132, '付款新增', 2130, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:payment:add', '#', 'admin', sysdate(), '', NULL, ''),
(2133, '付款修改', 2130, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:payment:edit', '#', 'admin', sysdate(), '', NULL, ''),
(2134, '付款审批', 2130, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:payment:approve', '#', 'admin', sysdate(), '', NULL, '');
