-- ============================================================
-- AllinOne 业务增量更新 — 报表管理系统 (work_report*)
-- 版本: v2.0.0
-- 日期: 2026-07-23
-- 依赖: 必须按顺序执行 ry_20260417.sql → allinone_biz.sql → 本文件
-- ============================================================

-- -----------------------------------------------------------
-- Phase 1: 报表主表 (报表管理容器)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `work_report` (
  `id` varchar(64) NOT NULL COMMENT '主键',
  `report_name` varchar(100) DEFAULT NULL COMMENT '报表名',
  `report_jianjie` varchar(500) DEFAULT NULL COMMENT '报表简介',
  `report_beizhu` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_datetime` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',
  `del_status` bigint(20) DEFAULT 0 COMMENT '逻辑删除状态 0未删除 1已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表管理';

-- -----------------------------------------------------------
-- Phase 2: Sheet 管理表 (多Sheet支持)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `work_report_sheet` (
  `id` varchar(64) NOT NULL COMMENT '主键',
  `report_id` varchar(64) NOT NULL COMMENT '报表ID',
  `sheet_index` bigint(20) DEFAULT NULL COMMENT 'Sheet序号',
  `sheet_name` varchar(100) DEFAULT NULL COMMENT 'Sheet名称',
  `sheet_data` longtext DEFAULT NULL COMMENT 'Luckysheet Sheet数据(JSON)',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_status` bigint(20) DEFAULT 0 COMMENT '逻辑删除状态 0未删除 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_report_id` (`report_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表Sheet管理';

-- -----------------------------------------------------------
-- Phase 3: 单元格数据表 (大规模数据优化)
--   - 范围查询: row_index/col_index BETWEEN
--   - 批量upsert: ON DUPLICATE KEY UPDATE
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `work_report_cell` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sheet_id` varchar(64) NOT NULL COMMENT 'Sheet ID',
  `row_index` int(11) NOT NULL COMMENT '行号',
  `col_index` int(11) NOT NULL COMMENT '列号',
  `cell_value` longtext DEFAULT NULL COMMENT '单元格值',
  `cell_formula` varchar(2000) DEFAULT NULL COMMENT '公式',
  `cell_type` varchar(20) DEFAULT 'string' COMMENT '类型 string|number|date|formula|bool',
  `cell_style` longtext DEFAULT NULL COMMENT '样式JSON',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cell` (`sheet_id`,`row_index`,`col_index`),
  KEY `idx_sheet_row` (`sheet_id`,`row_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表单元格数据';

-- -----------------------------------------------------------
-- Phase 4: 显式权限分配表 (双层权限模型)
--   权限类型: role(角色) | dept(部门) | user(用户)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `work_report_sheet_permission` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sheet_id` varchar(64) NOT NULL COMMENT 'Sheet ID',
  `perm_type` varchar(20) NOT NULL COMMENT '类型: role|dept|user',
  `perm_id` bigint(20) NOT NULL COMMENT '角色ID/部门ID/用户ID',
  `granted_by` bigint(20) DEFAULT NULL COMMENT '分配人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sheet_perm` (`sheet_id`,`perm_type`,`perm_id`),
  KEY `idx_sheet` (`sheet_id`),
  KEY `idx_target` (`perm_type`,`perm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Sheet 显式权限分配';

-- -----------------------------------------------------------
-- Phase 5: 先为旧版业务表补齐多 Sheet 字段
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
-- Phase 6: 在字段已存在后重建多 Sheet 唯一键。
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
-- Phase 7: 权限分配表增加唯一约束（防止重复授权）
-- 仅对已经执行过旧版本文件的数据库执行一次。
-- -----------------------------------------------------------
SET @add_perm_uk_sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'work_report_sheet_permission' AND INDEX_NAME = 'uk_sheet_perm') = 0,
  'ALTER TABLE `work_report_sheet_permission` ADD UNIQUE KEY `uk_sheet_perm` (`sheet_id`, `perm_type`, `perm_id`)',
  'SELECT 1'
);
PREPARE add_perm_uk_stmt FROM @add_perm_uk_sql;
EXECUTE add_perm_uk_stmt;
DEALLOCATE PREPARE add_perm_uk_stmt;

-- -----------------------------------------------------------
-- Phase 8: 清理 work_report 表遗留死字段 sheet_data（重构后从未读写）
-- 仅对已经执行过旧版本文件的数据库执行一次。
-- 注意：work_report_sheet.sheet_data 仍在正常使用，不能删除。
-- -----------------------------------------------------------
SET @drop_wr_sheet_data_sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'work_report' AND COLUMN_NAME = 'sheet_data') > 0,
  'ALTER TABLE `work_report` DROP COLUMN `sheet_data`',
  'SELECT 1'
);
PREPARE drop_wr_sheet_data_stmt FROM @drop_wr_sheet_data_sql;
EXECUTE drop_wr_sheet_data_stmt;
DEALLOCATE PREPARE drop_wr_sheet_data_stmt;

-- -----------------------------------------------------------
-- Phase 9: work_report_sheet 增加乐观锁版本号（多用户并发编辑防覆盖）
-- 单元格保存时按客户端持有版本 CAS 递增；幂等，已存在 version 列时跳过。
-- -----------------------------------------------------------
SET @add_sheet_version_sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'work_report_sheet' AND COLUMN_NAME = 'version') = 0,
  'ALTER TABLE `work_report_sheet` ADD COLUMN `version` bigint(20) NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号'' AFTER `del_status`',
  'SELECT 1'
);
PREPARE add_sheet_version_stmt FROM @add_sheet_version_sql;
EXECUTE add_sheet_version_stmt;
DEALLOCATE PREPARE add_sheet_version_stmt;

-- -----------------------------------------------------------
-- Phase 10: 新增填报数据异步导出任务表（大导出后台生成，前端轮询下载）
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
