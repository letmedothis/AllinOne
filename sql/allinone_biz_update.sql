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
  `sheet_data` longtext NULL COMMENT 'Luckysheet表格数据(JSON)',
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
  KEY `idx_sheet` (`sheet_id`),
  KEY `idx_target` (`perm_type`,`perm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Sheet 显式权限分配';

-- -----------------------------------------------------------
-- Phase 5: 修正多 Sheet 单元格唯一键
-- 仅对已经执行过旧版 allinone_biz.sql 的数据库执行一次。
-- -----------------------------------------------------------
ALTER TABLE `collect_data_cell`
  DROP INDEX `uk_cdc_data_rc`,
  ADD UNIQUE KEY `uk_cdc_data_rc` (`data_id`, `sheet_index`, `row_index`, `col_index`);

-- -----------------------------------------------------------
-- Phase 6: 字段映射支持多 Sheet
-- 仅对尚未增加 sheet_index 的数据库执行一次。
-- -----------------------------------------------------------
SET @add_sheet_index_sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'collect_field_mapping' AND COLUMN_NAME = 'sheet_index') = 0,
  'ALTER TABLE `collect_field_mapping` ADD COLUMN `sheet_index` int(8) NOT NULL DEFAULT 0 COMMENT ''Sheet序号（0-based）'' AFTER `cell_ref`',
  'SELECT 1'
);
PREPARE add_sheet_index_stmt FROM @add_sheet_index_sql;
EXECUTE add_sheet_index_stmt;
DEALLOCATE PREPARE add_sheet_index_stmt;

ALTER TABLE `collect_field_mapping`
  DROP INDEX `uk_cfm_rc`,
  ADD UNIQUE KEY `uk_cfm_rc` (`template_id`, `sheet_index`, `row_index`, `col_index`);
