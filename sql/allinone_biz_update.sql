-- ============================================================
-- AllinOne 业务增量更新
-- 版本: v2.1.0
-- 日期: 2026-08-30
-- 依赖: 必须按顺序执行 ry_20260417.sql → allinone_biz.sql → 本文件
-- 说明: 全部语句幂等，可重复执行。
--       WorkReport（work_report*，报表管理系统）已于 2026-08-30 下线，
--       历史建表/变更脚本与代码见 tag archive/workreport-20260830。
-- ============================================================

-- -----------------------------------------------------------
-- Phase 1: 为 collect_data_cell / collect_field_mapping 补齐多 Sheet 字段
-- -----------------------------------------------------------
SET @add_cell_sheet_index_sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'collect_data_cell' AND COLUMN_NAME = 'sheet_index') = 0,
  'ALTER TABLE `collect_data_cell` ADD COLUMN `sheet_index` int(4) NOT NULL DEFAULT 0 COMMENT ''Sheet序号（0-based）'' AFTER `template_id`',
  'SELECT 1'
);
PREPARE add_cell_sheet_index_stmt FROM @add_cell_sheet_index_sql;
EXECUTE add_cell_sheet_index_stmt;
DEALLOCATE PREPARE add_cell_sheet_index_stmt;

SET @add_sheet_index_sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'collect_field_mapping' AND COLUMN_NAME = 'sheet_index') = 0,
  'ALTER TABLE `collect_field_mapping` ADD COLUMN `sheet_index` int(8) NOT NULL DEFAULT 0 COMMENT ''Sheet序号（0-based）'' AFTER `cell_ref`',
  'SELECT 1'
);
PREPARE add_sheet_index_stmt FROM @add_sheet_index_sql;
EXECUTE add_sheet_index_stmt;
DEALLOCATE PREPARE add_sheet_index_stmt;

-- -----------------------------------------------------------
-- Phase 2: 在字段已存在后重建多 Sheet 唯一键。
-- 兼容索引存在/不存在两种历史库，可重复执行。
-- -----------------------------------------------------------
SET @rebuild_cell_uk_sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'collect_data_cell' AND INDEX_NAME = 'uk_cdc_data_rc') > 0,
  'ALTER TABLE `collect_data_cell` DROP INDEX `uk_cdc_data_rc`, ADD UNIQUE KEY `uk_cdc_data_rc` (`data_id`, `sheet_index`, `row_index`, `col_index`)',
  'ALTER TABLE `collect_data_cell` ADD UNIQUE KEY `uk_cdc_data_rc` (`data_id`, `sheet_index`, `row_index`, `col_index`)'
);
PREPARE rebuild_cell_uk_stmt FROM @rebuild_cell_uk_sql;
EXECUTE rebuild_cell_uk_stmt;
DEALLOCATE PREPARE rebuild_cell_uk_stmt;

SET @rebuild_mapping_uk_sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'collect_field_mapping' AND INDEX_NAME = 'uk_cfm_rc') > 0,
  'ALTER TABLE `collect_field_mapping` DROP INDEX `uk_cfm_rc`, ADD UNIQUE KEY `uk_cfm_rc` (`template_id`, `sheet_index`, `row_index`, `col_index`)',
  'ALTER TABLE `collect_field_mapping` ADD UNIQUE KEY `uk_cfm_rc` (`template_id`, `sheet_index`, `row_index`, `col_index`)'
);
PREPARE rebuild_mapping_uk_stmt FROM @rebuild_mapping_uk_sql;
EXECUTE rebuild_mapping_uk_stmt;
DEALLOCATE PREPARE rebuild_mapping_uk_stmt;

-- -----------------------------------------------------------
-- Phase 3: 填报数据异步导出任务表（大导出后台生成，前端轮询下载）
-- CREATE TABLE IF NOT EXISTS 天然幂等。
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `collect_export_task` (
  `task_id` bigint(20) NOT NULL COMMENT '任务ID',
  `task_name` varchar(100) DEFAULT NULL COMMENT '任务名称',
  `query_json` longtext DEFAULT NULL COMMENT '导出筛选条件(JSON)',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending排队|running导出中|success成功|failed失败',
  `file_name` varchar(255) DEFAULT NULL COMMENT '生成的导出文件名（位于下载目录）',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`task_id`),
  KEY `idx_cet_create_by` (`create_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='填报数据异步导出任务';

-- -----------------------------------------------------------
-- Phase 4: WorkReport 下线清理（2026-08-30，设计落地计划清单 4.3 选项B）
-- 1) 删除 sys_menu 中的 WorkReport 菜单与按钮（含历史角色授权，幂等）。
--    全新安装不受影响：allinone_menu.sql 已同步移除这些行。
-- 2) 业务数据表默认保留。确认不再需要历史数据后，
--    可手动执行下方注释中的 DROP 语句（不可恢复，执行前请先备份）。
-- -----------------------------------------------------------
DELETE FROM sys_role_menu WHERE menu_id IN (2005, 2013, 2033, 2034, 2035, 2036, 2037);
DELETE FROM sys_menu WHERE menu_id IN (2005, 2013, 2033, 2034, 2035, 2036, 2037);

-- 确认弃用 WorkReport 历史数据后再执行（先备份！）：
-- DROP TABLE IF EXISTS `work_report_sheet_permission`;
-- DROP TABLE IF EXISTS `work_report_cell`;
-- DROP TABLE IF EXISTS `work_report_sheet`;
-- DROP TABLE IF EXISTS `work_report`;

-- -----------------------------------------------------------
-- Phase 5: collect_data 增加模板版本快照列（记录填报/提交时所用的模板版本）
-- -----------------------------------------------------------
SET @add_template_version_sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'collect_data' AND COLUMN_NAME = 'template_version') = 0,
  'ALTER TABLE `collect_data` ADD COLUMN `template_version` int(8) DEFAULT NULL COMMENT ''填报/提交时模板版本快照'' AFTER `version`',
  'SELECT 1'
);
PREPARE add_template_version_stmt FROM @add_template_version_sql;
EXECUTE add_template_version_stmt;
DEALLOCATE PREPARE add_template_version_stmt;
