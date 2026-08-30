-- ============================================================
-- AllinOne 业务菜单与权限种子数据
-- 依赖：必须先执行 ry_20260417.sql（sys_menu 表结构）
-- 幂等：使用 INSERT IGNORE，可重复执行
-- 菜单ID段：2000-2099（ry_20260417.sql 已占用 1-1060，sys_menu auto_increment 从 2000 开始）
-- ============================================================

-- ------------------------------------------------------------
-- 顶层目录
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_menu VALUES
(2000, '业务管理', 0, 1, 'collect', NULL, '', 'BusinessManage', 1, 0, 'M', '0', '0', '', 'example', 'admin', sysdate(), '', NULL, '填报/报表业务目录'),
(2001, '报表中心', 0, 2, 'report', NULL, '', 'ReportCenter', 1, 0, 'M', '0', '0', '', 'component', 'admin', sysdate(), '', NULL, '报表配置/查看目录');

-- ------------------------------------------------------------
-- 业务管理子菜单
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_menu VALUES
(2002, '填报分类', 2000, 1, 'category', 'collect/category/index', '', 'CollectCategory', 1, 0, 'C', '0', '0', 'collect:category:list', 'list', 'admin', sysdate(), '', NULL, '填报分类菜单'),
(2003, '填报模板', 2000, 2, 'template', 'collect/template/index', '', 'CollectTemplate', 1, 0, 'C', '0', '0', 'collect:template:list', 'form', 'admin', sysdate(), '', NULL, '填报模板菜单'),
(2004, '填报数据', 2000, 3, 'data', 'collect/data/index', '', 'CollectData', 1, 0, 'C', '0', '0', 'collect:data:list', 'clipboard', 'admin', sysdate(), '', NULL, '填报数据菜单'),
(2050, '字段映射', 2000, 5, 'mapping', 'collect/mapping/index', '', 'CollectFieldMapping', 1, 0, 'C', '0', '0', '', 'excel', 'admin', sysdate(), '', NULL, '字段映射菜单（Tier 3 数据回写配置）');

-- ------------------------------------------------------------
-- 报表中心子菜单
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_menu VALUES
(2006, '报表配置', 2001, 1, 'config', 'report/config/index', '', 'ReportConfig', 1, 0, 'C', '0', '0', 'report:config:list', 'documentation', 'admin', sysdate(), '', NULL, '报表配置菜单'),
(2007, '报表分类', 2001, 2, 'category', 'report/category/index', '', 'ReportCategory', 1, 0, 'C', '0', '0', 'report:category:list', 'tree', 'admin', sysdate(), '', NULL, '报表分类菜单');

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
