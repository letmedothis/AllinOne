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
CREATE TABLE IF NOT EXISTS `collect_template_version` (
  `template_id` bigint(20) NOT NULL,
  `version` int(8) NOT NULL,
  `template_name` varchar(200) NOT NULL,
  `template_json` longtext,
  `status` char(1) NOT NULL DEFAULT '0',
  `created_by` varchar(64) DEFAULT '',
  `created_at` datetime DEFAULT NULL,
  PRIMARY KEY (`template_id`,`version`),
  KEY `idx_ctv_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='填报模板不可变版本快照';
INSERT IGNORE INTO collect_template_version(template_id,version,template_name,template_json,status,created_by,created_at)
SELECT template_id,version,template_name,template_json,status,COALESCE(update_by,create_by),COALESCE(update_time,create_time) FROM collect_template;

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

-- -----------------------------------------------------------
-- Phase 6: 供应链采购审批基础表（V1.0）
-- 说明：与 allinone_biz.sql 的全新安装定义保持一致；后续阶段继续追加明细、附件、发票和台账表。
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS company_profile (
  id bigint(20) NOT NULL, name varchar(200) NOT NULL, tax_id varchar(32) NOT NULL,
  timezone varchar(64) NOT NULL DEFAULT 'Asia/Shanghai', currency char(3) NOT NULL DEFAULT 'CNY',
  revision int(11) NOT NULL DEFAULT 1, create_time datetime NOT NULL, update_time datetime NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_sc_company_tax (tax_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链公司配置';

CREATE TABLE IF NOT EXISTS warehouses (
  id bigint(20) NOT NULL, name varchar(100) NOT NULL, enabled char(1) NOT NULL DEFAULT '1',
  create_by varchar(64) DEFAULT '', create_time datetime, update_by varchar(64) DEFAULT '', update_time datetime,
  PRIMARY KEY (id), UNIQUE KEY uk_sc_warehouse_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链仓库';

CREATE TABLE IF NOT EXISTS number_sequences (
  document_type varchar(32) NOT NULL, business_date date NOT NULL, next_value int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (document_type, business_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链单号序列';

CREATE TABLE IF NOT EXISTS documents (
  id bigint(20) NOT NULL, type varchar(32) NOT NULL, number varchar(64) NOT NULL, creator_id bigint(20) NOT NULL,
  creation_key varchar(80) NOT NULL, approval_status varchar(20) NOT NULL DEFAULT 'DRAFT', current_node varchar(20),
  current_version int(11) NOT NULL DEFAULT 0, revision int(11) NOT NULL DEFAULT 0, deleted char(1) NOT NULL DEFAULT '0',
  create_time datetime NOT NULL, update_time datetime NOT NULL, PRIMARY KEY (id),
  UNIQUE KEY uk_sc_document_number (number), UNIQUE KEY uk_sc_document_creation (creator_id, creation_key),
  KEY idx_sc_document_type_status (type, approval_status, update_time), KEY idx_sc_document_creator (creator_id, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链公共单据';

CREATE TABLE IF NOT EXISTS suppliers (
  document_id bigint(20) NOT NULL, name varchar(200) NOT NULL, tax_id varchar(32) NOT NULL, dedup_key varchar(32) NOT NULL,
  contact varchar(100) NOT NULL, phone varchar(50) NOT NULL, address varchar(500) NOT NULL, bank_name varchar(200) NOT NULL,
  account_name varchar(200) NOT NULL, bank_account varchar(100) NOT NULL, attachment_paths varchar(4000), remark varchar(2000), PRIMARY KEY (document_id),
  UNIQUE KEY uk_sc_supplier_dedup (dedup_key), KEY idx_sc_supplier_tax (tax_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链供应商准入';

SET @add_supplier_attachment_sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'suppliers' AND COLUMN_NAME = 'attachment_paths') = 0,
  'ALTER TABLE suppliers ADD COLUMN attachment_paths varchar(4000) DEFAULT NULL AFTER bank_account',
  'SELECT 1'
);
PREPARE add_supplier_attachment_stmt FROM @add_supplier_attachment_sql;
EXECUTE add_supplier_attachment_stmt;
DEALLOCATE PREPARE add_supplier_attachment_stmt;

CREATE TABLE IF NOT EXISTS purchase_orders (
  document_id bigint(20) NOT NULL, supplier_id bigint(20) NOT NULL, buyer_id bigint(20) NOT NULL, expected_date date,
  currency char(3) NOT NULL DEFAULT 'CNY', amount_cents bigint(20) NOT NULL DEFAULT 0, tax_cents bigint(20) NOT NULL DEFAULT 0,
  total_cents bigint(20) NOT NULL DEFAULT 0, remark varchar(2000), PRIMARY KEY (document_id),
  KEY idx_sc_po_supplier (supplier_id), KEY idx_sc_po_buyer (buyer_id), KEY idx_sc_po_expected (expected_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购订单';

CREATE TABLE IF NOT EXISTS purchase_order_lines (
  id bigint(20) NOT NULL, order_id bigint(20) NOT NULL, line_no int(11) NOT NULL, is_current char(1) NOT NULL DEFAULT '1',
  name varchar(200) NOT NULL, specification varchar(500), unit varchar(32) NOT NULL, quantity_q4 decimal(20,4) NOT NULL,
  price_p6 decimal(20,6) NOT NULL, rate_r4 decimal(20,4) NOT NULL DEFAULT 0, amount_cents bigint(20) NOT NULL DEFAULT 0,
  tax_cents bigint(20) NOT NULL DEFAULT 0, total_cents bigint(20) NOT NULL DEFAULT 0, PRIMARY KEY (id),
  KEY idx_sc_po_line_current (order_id, line_no, is_current), KEY idx_sc_po_line_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购订单明细';

SET @drop_sc_po_line_unique_sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'purchase_order_lines' AND INDEX_NAME = 'uk_sc_po_line') > 0,
  'ALTER TABLE purchase_order_lines DROP INDEX uk_sc_po_line', 'SELECT 1'
);
PREPARE drop_sc_po_line_unique_stmt FROM @drop_sc_po_line_unique_sql;
EXECUTE drop_sc_po_line_unique_stmt;
DEALLOCATE PREPARE drop_sc_po_line_unique_stmt;

CREATE TABLE IF NOT EXISTS workflow_configs (
  id bigint(20) NOT NULL, flow_type varchar(32) NOT NULL, version int(11) NOT NULL,
  supervisor_candidates varchar(2000), finance_candidates varchar(2000), updated_by bigint(20), updated_at datetime,
  PRIMARY KEY (id), UNIQUE KEY uk_sc_flow_config (flow_type, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='固定审批流配置';

CREATE TABLE IF NOT EXISTS attachment_files (
  id bigint(20) NOT NULL, original_name varchar(255) NOT NULL, storage_path varchar(1000) NOT NULL,
  media_type varchar(100), size_bytes bigint(20) NOT NULL, sha256 varchar(64) NOT NULL,
  uploaded_by bigint(20) NOT NULL, uploaded_at datetime NOT NULL, PRIMARY KEY (id),
  KEY idx_sc_attachment_hash (sha256), KEY idx_sc_attachment_uploader (uploaded_by, uploaded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链附件文件';

CREATE TABLE IF NOT EXISTS document_attachments (
  id bigint(20) NOT NULL, document_id bigint(20) NOT NULL, file_id bigint(20) NOT NULL,
  version_id bigint(20), attachment_type varchar(32) NOT NULL DEFAULT 'SUPPORTING',
  created_by bigint(20) NOT NULL, created_at datetime NOT NULL, PRIMARY KEY (id),
  UNIQUE KEY uk_sc_document_file (document_id, file_id), KEY idx_sc_document_attachment (document_id, version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链单据附件绑定';

CREATE TABLE IF NOT EXISTS version_attachments (
  version_id bigint(20) NOT NULL, file_id bigint(20) NOT NULL, name_snapshot varchar(255) NOT NULL,
  sha256 varchar(64) NOT NULL, PRIMARY KEY (version_id, file_id), KEY idx_sc_version_attachment_file (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链提交版本附件快照';

CREATE TABLE IF NOT EXISTS document_comments (
  id bigint(20) NOT NULL, document_id bigint(20) NOT NULL, version_id bigint(20), author_id bigint(20) NOT NULL,
  author_snapshot varchar(200) NOT NULL, content varchar(2000) NOT NULL, created_at datetime NOT NULL,
  PRIMARY KEY (id), KEY idx_sc_comment_document (document_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链单据追加评论';

CREATE TABLE IF NOT EXISTS document_versions (
  id bigint(20) NOT NULL, document_id bigint(20) NOT NULL, version_no int(11) NOT NULL, snapshot_json longtext NOT NULL,
  content_hash varchar(128) NOT NULL, submitted_by bigint(20) NOT NULL, submitted_at datetime NOT NULL, PRIMARY KEY (id),
  UNIQUE KEY uk_sc_document_version (document_id, version_no), KEY idx_sc_version_document (document_id, submitted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链提交版本快照';

CREATE TABLE IF NOT EXISTS workflow_instances (
  id bigint(20) NOT NULL, document_id bigint(20) NOT NULL, version_id bigint(20) NOT NULL, config_version int(11) NOT NULL,
  status varchar(20) NOT NULL, started_at datetime NOT NULL, finished_at datetime, PRIMARY KEY (id),
  UNIQUE KEY uk_sc_workflow_version (document_id, version_id), KEY idx_sc_workflow_document (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链审批流程实例';

CREATE TABLE IF NOT EXISTS workflow_tasks (
  id bigint(20) NOT NULL, instance_id bigint(20) NOT NULL, node varchar(20) NOT NULL, sequence_no int(11) NOT NULL,
  assignee_id bigint(20) NOT NULL, status varchar(20) NOT NULL DEFAULT 'PENDING', decision varchar(20), comment varchar(2000),
  acted_by bigint(20), acted_at datetime, revision int(11) NOT NULL DEFAULT 0, PRIMARY KEY (id),
  UNIQUE KEY uk_sc_workflow_task (instance_id, sequence_no), KEY idx_sc_task_assignee (assignee_id, status, id), KEY idx_sc_task_instance (instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链审批任务';

CREATE TABLE IF NOT EXISTS audit_events (
  id bigint(20) NOT NULL, document_id bigint(20) NOT NULL, version_id bigint(20), actor_id bigint(20) NOT NULL,
  actor_snapshot varchar(200) NOT NULL, action varchar(50) NOT NULL, reason varchar(2000), changes_json longtext,
  request_id varchar(64), created_at datetime NOT NULL, PRIMARY KEY (id), KEY idx_sc_audit_document (document_id, created_at), KEY idx_sc_audit_request (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链审计事件';

CREATE TABLE IF NOT EXISTS receipts (
  document_id bigint(20) NOT NULL, order_id bigint(20) NOT NULL, warehouse_id bigint(20) NOT NULL,
  business_date date NOT NULL, confirmed_by bigint(20) NOT NULL, confirmed_at datetime NOT NULL,
  remark varchar(2000), PRIMARY KEY (document_id), KEY idx_sc_receipt_order (order_id, confirmed_at), KEY idx_sc_receipt_date (business_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链入库单';

CREATE TABLE IF NOT EXISTS receipt_lines (
  id bigint(20) NOT NULL, receipt_id bigint(20) NOT NULL, order_line_id bigint(20) NOT NULL,
  quantity_q4 decimal(20,4) NOT NULL, item_snapshot varchar(600) NOT NULL, PRIMARY KEY (id),
  UNIQUE KEY uk_sc_receipt_line (receipt_id, order_line_id), KEY idx_sc_receipt_line_order (order_line_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链入库明细';

CREATE TABLE IF NOT EXISTS invoices (
  document_id bigint(20) NOT NULL, order_id bigint(20) NULL, invoice_number varchar(64) NOT NULL,
  invoice_type varchar(32) NOT NULL DEFAULT 'VAT_SPECIAL', issue_date date NOT NULL, seller_name varchar(200) NOT NULL,
  seller_tax_id varchar(32) NOT NULL, buyer_name varchar(200) NOT NULL, buyer_tax_id varchar(32) NOT NULL,
  amount_cents bigint(20) NOT NULL DEFAULT 0, tax_cents bigint(20) NOT NULL DEFAULT 0, total_cents bigint(20) NOT NULL DEFAULT 0,
  difference_note varchar(2000), manual_confirmed char(1) NOT NULL DEFAULT '0', confirmed_content_hash varchar(64), confirmed_by bigint(20), confirmed_at datetime, PRIMARY KEY (document_id), KEY idx_sc_invoice_order (order_id), KEY idx_sc_invoice_issue (issue_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链发票';
SET @sc_invoice_order_nullable_sql = IF(
  (SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='invoices' AND COLUMN_NAME='order_id')='NO',
  'ALTER TABLE invoices MODIFY COLUMN order_id bigint(20) NULL', 'SELECT 1'
);
PREPARE sc_invoice_order_nullable_stmt FROM @sc_invoice_order_nullable_sql;
EXECUTE sc_invoice_order_nullable_stmt;
DEALLOCATE PREPARE sc_invoice_order_nullable_stmt;

CREATE TABLE IF NOT EXISTS opening_payables (
  invoice_id bigint(20) NOT NULL, supplier_id bigint(20) NOT NULL, opening_date date NOT NULL,
  reason varchar(500) NOT NULL, PRIMARY KEY (invoice_id), KEY idx_sc_opening_supplier (supplier_id, opening_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='期初应付余额';
SET @add_sc_invoice_confirm_sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='invoices' AND COLUMN_NAME='manual_confirmed')=0,
  'ALTER TABLE invoices ADD COLUMN manual_confirmed char(1) NOT NULL DEFAULT ''0'' AFTER difference_note, ADD COLUMN confirmed_content_hash varchar(64) AFTER manual_confirmed', 'SELECT 1'
);
PREPARE add_sc_invoice_confirm_stmt FROM @add_sc_invoice_confirm_sql;
EXECUTE add_sc_invoice_confirm_stmt;
DEALLOCATE PREPARE add_sc_invoice_confirm_stmt;
SET @add_sc_invoice_hash_sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='invoices' AND COLUMN_NAME='confirmed_content_hash')=0,
  'ALTER TABLE invoices ADD COLUMN confirmed_content_hash varchar(64) AFTER manual_confirmed', 'SELECT 1'
);
PREPARE add_sc_invoice_hash_stmt FROM @add_sc_invoice_hash_sql;
EXECUTE add_sc_invoice_hash_stmt;
DEALLOCATE PREPARE add_sc_invoice_hash_stmt;
SET @add_sc_invoice_confirmed_by_sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='invoices' AND COLUMN_NAME='confirmed_by')=0,
  'ALTER TABLE invoices ADD COLUMN confirmed_by bigint(20) AFTER confirmed_content_hash', 'SELECT 1'
);
PREPARE add_sc_invoice_confirmed_by_stmt FROM @add_sc_invoice_confirmed_by_sql;
EXECUTE add_sc_invoice_confirmed_by_stmt;
DEALLOCATE PREPARE add_sc_invoice_confirmed_by_stmt;
SET @add_sc_invoice_confirmed_at_sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='invoices' AND COLUMN_NAME='confirmed_at')=0,
  'ALTER TABLE invoices ADD COLUMN confirmed_at datetime AFTER confirmed_by', 'SELECT 1'
);
PREPARE add_sc_invoice_confirmed_at_stmt FROM @add_sc_invoice_confirmed_at_sql;
EXECUTE add_sc_invoice_confirmed_at_stmt;
DEALLOCATE PREPARE add_sc_invoice_confirmed_at_stmt;
CREATE TABLE IF NOT EXISTS invoice_lines (
  id bigint(20) NOT NULL, invoice_id bigint(20) NOT NULL, line_no int(11) NOT NULL, is_current char(1) NOT NULL DEFAULT '1', order_line_id bigint(20) NOT NULL,
  name varchar(200) NOT NULL, unit varchar(32) NOT NULL, quantity_q4 decimal(20,4) NOT NULL, price_p6 decimal(20,6) NOT NULL,
  rate_r4 decimal(20,4) NOT NULL DEFAULT 0, amount_cents bigint(20) NOT NULL DEFAULT 0, tax_cents bigint(20) NOT NULL DEFAULT 0,
  total_cents bigint(20) NOT NULL DEFAULT 0, PRIMARY KEY (id), UNIQUE KEY uk_sc_invoice_line (invoice_id, line_no, is_current), KEY idx_sc_invoice_line_order (order_line_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链发票明细';
SET @add_sc_invoice_line_current_sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='invoice_lines' AND COLUMN_NAME='is_current')=0,
  'ALTER TABLE invoice_lines ADD COLUMN is_current char(1) NOT NULL DEFAULT ''1'' AFTER line_no', 'SELECT 1'
);
PREPARE add_sc_invoice_line_current_stmt FROM @add_sc_invoice_line_current_sql;
EXECUTE add_sc_invoice_line_current_stmt;
DEALLOCATE PREPARE add_sc_invoice_line_current_stmt;
SET @drop_sc_invoice_line_unique_sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='invoice_lines' AND INDEX_NAME='uk_sc_invoice_line')>0,
  'ALTER TABLE invoice_lines DROP INDEX uk_sc_invoice_line', 'SELECT 1'
);
PREPARE drop_sc_invoice_line_unique_stmt FROM @drop_sc_invoice_line_unique_sql;
EXECUTE drop_sc_invoice_line_unique_stmt;
SET @add_sc_invoice_line_unique_sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='invoice_lines' AND INDEX_NAME='uk_sc_invoice_line_v')=0,
  'ALTER TABLE invoice_lines ADD UNIQUE KEY uk_sc_invoice_line_v (invoice_id,line_no,is_current)', 'SELECT 1'
);
PREPARE add_sc_invoice_line_unique_stmt FROM @add_sc_invoice_line_unique_sql;
EXECUTE add_sc_invoice_line_unique_stmt;
DEALLOCATE PREPARE add_sc_invoice_line_unique_stmt;
CREATE TABLE IF NOT EXISTS invoice_identities (
  id bigint(20) NOT NULL, seller_tax_id varchar(32) NOT NULL, invoice_number varchar(64) NOT NULL, invoice_id bigint(20) NOT NULL,
  ever_submitted char(1) NOT NULL DEFAULT '1', created_at datetime NOT NULL, PRIMARY KEY (id), UNIQUE KEY uk_sc_invoice_identity (seller_tax_id, invoice_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链发票身份唯一性';
CREATE TABLE IF NOT EXISTS invoice_allocations (
  id bigint(20) NOT NULL, invoice_id bigint(20) NOT NULL, version_id bigint(20) NOT NULL, invoice_line_id bigint(20) NOT NULL, receipt_line_id bigint(20) NOT NULL,
  quantity_q4 decimal(20,4) NOT NULL, state varchar(20) NOT NULL DEFAULT 'RESERVED', PRIMARY KEY (id),
  UNIQUE KEY uk_sc_invoice_alloc (invoice_id, version_id, invoice_line_id, receipt_line_id), KEY idx_sc_invoice_alloc_receipt (receipt_line_id, state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链发票入库分配';

SET @add_sc_alloc_version_sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='invoice_allocations' AND COLUMN_NAME='version_id')=0,
  'ALTER TABLE invoice_allocations ADD COLUMN version_id bigint(20) NOT NULL DEFAULT 0 AFTER invoice_id', 'SELECT 1'
);
PREPARE add_sc_alloc_version_stmt FROM @add_sc_alloc_version_sql;
EXECUTE add_sc_alloc_version_stmt;
DEALLOCATE PREPARE add_sc_alloc_version_stmt;
SET @drop_sc_alloc_unique_sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='invoice_allocations' AND INDEX_NAME='uk_sc_invoice_alloc')>0,
  'ALTER TABLE invoice_allocations DROP INDEX uk_sc_invoice_alloc', 'SELECT 1'
);
PREPARE drop_sc_alloc_unique_stmt FROM @drop_sc_alloc_unique_sql;
EXECUTE drop_sc_alloc_unique_stmt;
DEALLOCATE PREPARE drop_sc_alloc_unique_stmt;
SET @add_sc_alloc_unique_sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='invoice_allocations' AND INDEX_NAME='uk_sc_invoice_alloc_v')=0,
  'ALTER TABLE invoice_allocations ADD UNIQUE KEY uk_sc_invoice_alloc_v (invoice_id,version_id,invoice_line_id,receipt_line_id)', 'SELECT 1'
);
PREPARE add_sc_alloc_unique_stmt FROM @add_sc_alloc_unique_sql;
EXECUTE add_sc_alloc_unique_stmt;
DEALLOCATE PREPARE add_sc_alloc_unique_stmt;

-- documents 增加审批引擎归属列（LEGACY=内置固定流程；FLOWABLE=Flowable 引擎）。
SET @add_sc_doc_engine_sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='documents' AND COLUMN_NAME='workflow_engine')=0,
  'ALTER TABLE documents ADD COLUMN workflow_engine varchar(16) NOT NULL DEFAULT ''LEGACY'' AFTER current_node', 'SELECT 1'
);
PREPARE add_sc_doc_engine_stmt FROM @add_sc_doc_engine_sql;
EXECUTE add_sc_doc_engine_stmt;
DEALLOCATE PREPARE add_sc_doc_engine_stmt;

-- Flowable ACT_* 引擎表由应用首次启动时自动初始化（见 FLOWABLE_DATABASE_SCHEMA_UPDATE）。
CREATE TABLE IF NOT EXISTS workflow_designs (
  id bigint(20) NOT NULL, process_key varchar(100) NOT NULL, name varchar(100) NOT NULL, bpmn_xml longtext NOT NULL,
  status varchar(20) NOT NULL DEFAULT 'DRAFT', revision int(11) NOT NULL DEFAULT 0,
  published_definition_id varchar(128), published_version int(11), create_by varchar(64) NOT NULL, create_time datetime NOT NULL,
  update_by varchar(64) NOT NULL, update_time datetime NOT NULL, published_at datetime,
  PRIMARY KEY (id), UNIQUE KEY uk_sc_workflow_design_key (process_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Flowable 流程设计草稿';
CREATE TABLE IF NOT EXISTS workflow_action_logs (
  id bigint(20) NOT NULL, process_instance_id varchar(128) NOT NULL, task_id varchar(128) NOT NULL,
  task_definition_key varchar(100) NOT NULL, action varchar(20) NOT NULL, comment varchar(2000),
  actor_id bigint(20) NOT NULL, actor_name varchar(100) NOT NULL, created_at datetime NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_sc_workflow_action_task (task_id), KEY idx_sc_workflow_action_instance (process_instance_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Flowable 审批动作记录';
-- 以下菜单为已有环境的幂等升级；新环境仍由 allinone_menu.sql 初始化。
INSERT IGNORE INTO sys_menu VALUES
(2109, '流程待办', 2100, 8, 'workflow-tasks', 'supply/workflow/tasks', '', 'SupplyWorkflowTasks', 1, 0, 'C', '0', '0', 'supply:workflow:task', 'list', 'admin', sysdate(), '', NULL, 'Flowable 采购订单流程待办'),
(2111, '流程设计', 2100, 9, 'workflow-designer', 'supply/workflow/index', '', 'SupplyWorkflowDesigner', 1, 0, 'C', '0', '0', 'supply:workflow:list', 'guide', 'admin', sysdate(), '', NULL, 'Flowable BPMN 流程设计与发布'),
(2112, '流程详情', 2100, 11, 'workflow/detail', 'supply/workflow/detail', '', 'SupplyWorkflowDetail', 1, 0, 'C', '1', '0', 'supply:workflow:task', '#', 'admin', sysdate(), '', NULL, 'Flowable 流程实例详情'),
(2180, '流程定义查看', 2111, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:workflow:list', '#', 'admin', sysdate(), '', NULL, ''),
(2181, '流程定义发布', 2111, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:workflow:deploy', '#', 'admin', sysdate(), '', NULL, ''),
(2182, '流程待办处理', 2109, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:workflow:task', '#', 'admin', sysdate(), '', NULL, '');

-- M2 数据范围:sys_user 增加职级维度(EXEC 总监看全部 / LEADER 部门领导看本部门 / STAFF 职员看本人)。
-- 可空:未配置的用户回退旧的"角色全局或本人"逻辑,避免范围收缩。
SET @add_sys_user_rank_sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sys_user' AND COLUMN_NAME='rank_level')=0,
  'ALTER TABLE sys_user ADD COLUMN rank_level varchar(16) DEFAULT NULL COMMENT ''职级:EXEC总监/LEADER部门领导/STAFF职员'' AFTER dept_id', 'SELECT 1'
);
PREPARE add_sys_user_rank_stmt FROM @add_sys_user_rank_sql;
EXECUTE add_sys_user_rank_stmt;
DEALLOCATE PREPARE add_sys_user_rank_stmt;

-- M4 付款 / 应付核销:新库见 allinone_biz.sql,此处为已有环境幂等建表。
CREATE TABLE IF NOT EXISTS payments (
  id bigint(20) NOT NULL, number varchar(64) NOT NULL, supplier_id bigint(20) NOT NULL,
  amount_cents bigint(20) NOT NULL, threshold_exceeded char(1) NOT NULL DEFAULT '0',
  status varchar(20) NOT NULL DEFAULT 'DRAFT', current_node varchar(32) DEFAULT NULL,
  creator_id bigint(20) NOT NULL, creation_key varchar(80) NOT NULL, revision int(11) NOT NULL DEFAULT 0,
  remark varchar(500), review_comment varchar(500), director_comment varchar(500),
  create_time datetime NOT NULL, update_time datetime NOT NULL, deleted char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id), UNIQUE KEY uk_pay_number (number), UNIQUE KEY uk_pay_creation (creator_id, creation_key),
  KEY idx_pay_status (status, update_time), KEY idx_pay_supplier (supplier_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链付款单';
CREATE TABLE IF NOT EXISTS payment_lines (
  id bigint(20) NOT NULL, payment_id bigint(20) NOT NULL, invoice_id bigint(20) NOT NULL,
  allocated_cents bigint(20) NOT NULL, create_time datetime NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_pay_line_invoice (payment_id, invoice_id), KEY idx_pay_line_invoice (invoice_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链付款核销明细';
-- M4 付款菜单(已有环境升级;新库由 allinone_menu.sql 初始化)。
INSERT IGNORE INTO sys_menu VALUES
(2115, '付款管理', 2100, 12, 'payment', 'supply/payment/index', '', 'SupplyPayment', 1, 0, 'C', '0', '0', 'supply:payment:query', 'money', 'admin', sysdate(), '', NULL, '采购付款与应付核销'),
(2190, '付款查询', 2115, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:payment:query', '#', 'admin', sysdate(), '', NULL, ''),
(2191, '付款新增', 2115, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:payment:add', '#', 'admin', sysdate(), '', NULL, ''),
(2192, '付款修改', 2115, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:payment:edit', '#', 'admin', sysdate(), '', NULL, ''),
(2193, '付款审批', 2115, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:payment:approve', '#', 'admin', sysdate(), '', NULL, '');

-- 生产复盘修复：付款每次动作保留独立记录；旧 APPROVED 保持历史已付款语义。
CREATE TABLE IF NOT EXISTS payment_events (
  id bigint NOT NULL PRIMARY KEY,
  payment_id bigint NOT NULL,
  actor_id bigint NOT NULL,
  actor_name varchar(64) NOT NULL,
  action varchar(32) NOT NULL,
  comment varchar(500),
  snapshot longtext NOT NULL,
  created_at datetime NOT NULL,
  KEY idx_payment_event (payment_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='付款业务审计与实际支付凭据';
-- 下一阶段：独立变更申请、带期限代理、不可变内容版本（无破坏性升级）。
CREATE TABLE IF NOT EXISTS review_cases (
 id bigint PRIMARY KEY, type varchar(32) NOT NULL, target_id bigint NOT NULL, owner_id bigint NOT NULL,
 dept_id bigint NOT NULL, title varchar(200) NOT NULL, status varchar(24) NOT NULL, node varchar(40),
 revision int NOT NULL DEFAULT 0, round int NOT NULL DEFAULT 0, base_version int NOT NULL DEFAULT 0,
 before_json longtext, after_json longtext NOT NULL, reason varchar(1000), sensitive boolean NOT NULL DEFAULT false,
 supervisor_id bigint, finance_id bigint, first_actor_id bigint, active_key varchar(100),
 created_at datetime NOT NULL, updated_at datetime NOT NULL,
 UNIQUE KEY uk_review_active(active_key), KEY idx_review_owner(owner_id,type,status), KEY idx_review_pending(status,supervisor_id,finance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS review_events (
 id bigint PRIMARY KEY, case_id bigint NOT NULL, round int NOT NULL, action varchar(32) NOT NULL,
 actor_id bigint NOT NULL, responsible_id bigint NOT NULL, delegation_id bigint, comment varchar(2000),
 snapshot longtext NOT NULL, request_key varchar(80), created_at datetime NOT NULL,
 UNIQUE KEY uk_review_request(case_id,request_key), KEY idx_review_event(case_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS review_config (
 type varchar(32) NOT NULL, target_key bigint NOT NULL DEFAULT 0, dept_id bigint NOT NULL,
 supervisor_id bigint NOT NULL, finance_id bigint, open_from datetime, open_until datetime,
 PRIMARY KEY(type,target_key,dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS review_files (
 id bigint PRIMARY KEY, case_id bigint NOT NULL, file_name varchar(200) NOT NULL,
 storage_path varchar(500) NOT NULL, sha256 varchar(64) NOT NULL, created_by bigint NOT NULL, created_at datetime NOT NULL,
 KEY idx_review_file(case_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS approval_delegation (
 id bigint PRIMARY KEY, principal_id bigint NOT NULL, agent_id bigint NOT NULL, dept_id bigint NOT NULL,
 principal_dept_id bigint NOT NULL, agent_dept_id bigint NOT NULL, type varchar(32) NOT NULL, node varchar(80) NOT NULL,
 max_cents bigint, starts_at datetime NOT NULL, ends_at datetime NOT NULL, status varchar(24) NOT NULL,
 KEY idx_delegate_agent(agent_id,status,ends_at), KEY idx_delegate_principal(principal_id,type,node,starts_at,ends_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS approval_actor_events (
 id bigint PRIMARY KEY, business_type varchar(32) NOT NULL, business_id varchar(80) NOT NULL, round_key varchar(80) NOT NULL,
 node varchar(80) NOT NULL, responsible_id bigint NOT NULL, actor_id bigint NOT NULL, delegation_id bigint,
 action varchar(32) NOT NULL, created_at datetime NOT NULL, KEY idx_actor_business(business_type,business_id,round_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS supplier_profile_version (
 supplier_id bigint NOT NULL, version int NOT NULL, snapshot longtext NOT NULL, case_id bigint, created_at datetime NOT NULL,
 PRIMARY KEY(supplier_id,version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS payment_account_snapshot (
 payment_id bigint PRIMARY KEY, supplier_id bigint NOT NULL, profile_version int NOT NULL,
 snapshot longtext NOT NULL, confirmed boolean NOT NULL DEFAULT true, updated_at datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS collect_data_version (
 data_id bigint NOT NULL, version int NOT NULL, template_version int NOT NULL, template_json longtext NOT NULL,
 mapping_json longtext NOT NULL, form_data longtext NOT NULL, cells_json longtext NOT NULL,
 case_id bigint, created_by varchar(64) NOT NULL, created_at datetime NOT NULL, PRIMARY KEY(data_id,version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS collect_mapping_binding (
 data_id bigint PRIMARY KEY, mapping_json longtext NOT NULL, created_at datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS collect_writeback_source (
 target_hash varchar(64) PRIMARY KEY, data_id bigint NOT NULL, table_name varchar(64) NOT NULL,
 key_json longtext NOT NULL, before_json longtext, after_json longtext NOT NULL, updated_at datetime NOT NULL,
 KEY idx_writeback_source(data_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
