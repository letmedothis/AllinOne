-- ============================================================
-- AllinOne 企业级报表管理系统 — 业务表建表脚本
-- 版本: 1.0.0
-- 说明: 包含 collect_*/report_* 业务表，基于 RuoYi 表设计规范
-- 字符集: utf8mb4
-- ============================================================

-- ----------------------------
-- 1. 填报模板分类表
-- ----------------------------
drop table if exists collect_category;
create table collect_category (
  category_id      bigint(20)      not null                   comment '雪花主键',
  category_name    varchar(100)    not null                   comment '分类名称',
  parent_id        bigint(20)      default 0                  comment '父分类ID',
  ancestors        varchar(500)    default ''                 comment '祖级列表',
  order_num        int(4)          default 0                  comment '显示顺序',
  status           char(1)         default '0'                comment '状态（0正常 1停用）',
  remark           varchar(500)    default null               comment '备注',
  del_flag         char(1)         not null default '0'       comment '删除标志（0存在 2删除）',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        varchar(64)     default ''                 comment '更新者',
  update_time      datetime                                   comment '更新时间',
  primary key (category_id),
  key idx_cc_parent (parent_id),
  key idx_cc_order (parent_id, order_num),
  key idx_cc_status (status)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment = '填报模板分类表';

-- ----------------------------
-- 2. 填报模板表
-- ----------------------------
drop table if exists collect_template;
create table collect_template (
  template_id      bigint(20)      not null                   comment '雪花主键',
  template_name    varchar(200)    not null                   comment '模板名称',
  template_code    varchar(64)     not null                   comment '模板编码',
  category_id      bigint(20)      default null               comment '所属分类ID',
  template_type    char(1)         default '0'                comment '模板类型（0普通 1带流程）',
  template_json    longtext                                   comment '模板JSON（Luckysheet完整配置）',
  status           char(1)         default '0'                comment '发布状态（0未发布 1已发布）',
  version          int(8)          default 1                  comment '版本号',
  remark           varchar(500)    default null               comment '备注',
  del_flag         char(1)         not null default '0'       comment '删除标志（0存在 2删除）',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        varchar(64)     default ''                 comment '更新者',
  update_time      datetime                                   comment '更新时间',
  primary key (template_id),
  unique key uk_ct_code (template_code),
  key idx_ct_category (category_id),
  key idx_ct_status (status),
  key idx_ct_create_time (create_time)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment = '填报模板表';

-- ----------------------------
-- 3. 填报数据表
-- ----------------------------
drop table if exists collect_data;
create table collect_data (
  data_id          bigint(20)      not null                   comment '雪花主键',
  template_id      bigint(20)      not null                   comment '所属模板ID',
  form_data        longtext                                   comment '填报数据JSON（Tier 1快照）',
  biz_status       varchar(10)     default 'draft'            comment '业务状态（draft草稿 submitted已提交）',
  dept_id          bigint(20)      default null               comment '所属部门ID',
  flow_instance_id varchar(64)     default null               comment '关联流程实例ID（V2.0）',
  data_code        varchar(64)     default null               comment '业务编码',
  version          int(8)          default 1                  comment '乐观锁版本号',
  template_version int(8)          default null               comment '填报/提交时模板版本快照',
  submit_by        varchar(64)     default null               comment '提交人',
  submit_time      datetime                                   comment '提交时间',
  remark           varchar(2000)   default null               comment '备注（驳回原因等）',
  del_flag         char(1)         not null default '0'       comment '删除标志（0存在 2删除）',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        varchar(64)     default ''                 comment '更新者',
  update_time      datetime                                   comment '更新时间',
  primary key (data_id),
  key idx_cd_template (template_id),
  key idx_cd_submit (submit_by, submit_time),
  key idx_cd_dept (dept_id),
  key idx_cd_creator (template_id, create_by),
  key idx_cd_del (del_flag, create_time),
  key idx_cd_biz (biz_status),
  key idx_cd_code (data_code)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment = '填报数据表';

-- ----------------------------
-- 4. 填报单元格数据表（三层架构 Tier 2）
-- ----------------------------
drop table if exists collect_data_cell;
create table collect_data_cell (
  cell_id            bigint(20)      not null                   comment '雪花主键',
  data_id            bigint(20)      not null                   comment '所属填报数据ID',
  template_id        bigint(20)      not null                   comment '所属模板ID（冗余字段）',
  sheet_index        int(4)          default 0                  comment '工作表索引',
  row_index          int(8)          not null                   comment '行号（0-based）',
  col_index          int(8)          not null                   comment '列号（0-based）',
  cell_text          text                                       comment '显示文本（m值）',
  cell_value         text                                       comment '原始值（v值）',
  cell_numeric_value decimal(20,4)   default null               comment '预解析数值',
  cell_type          varchar(20)     default null               comment '单元格类型',
  cell_format        varchar(50)     default null               comment '数字格式',
  is_formula         char(1)         default '0'                comment '是否为公式（0否 1是）',
  formula_expr       text                                       comment '公式表达式',
  del_flag           char(1)         not null default '0'       comment '删除标志（0存在 2删除）',
  create_by          varchar(64)     default ''                 comment '创建者',
  create_time        datetime                                   comment '创建时间',
  update_by          varchar(64)     default ''                 comment '更新者',
  update_time        datetime                                   comment '更新时间',
  primary key (cell_id),
  unique key uk_cdc_data_rc (data_id, sheet_index, row_index, col_index),
  key idx_cdc_template (template_id),
  key idx_cdc_sheet (data_id, sheet_index),
  key idx_cdc_tmpl_rc (template_id, row_index, col_index, sheet_index)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment = '填报单元格数据表（三层架构Tier 2）';

-- ----------------------------
-- 5. 字段映射配置表（三层架构 Tier 3）
-- ----------------------------
drop table if exists collect_field_mapping;
create table collect_field_mapping (
  mapping_id       bigint(20)      not null                   comment '雪花主键',
  template_id      bigint(20)      not null                   comment '关联模板ID',
  cell_ref         varchar(20)     default null               comment '单元格坐标（如B3）',
  sheet_index      int(8)          not null default 0         comment 'Sheet序号（0-based）',
  row_index        int(8)          not null                   comment '行号（0-based）',
  col_index        int(8)          not null                   comment '列号（0-based）',
  target_table     varchar(100)    not null                   comment '目标表名',
  target_column    varchar(100)    not null                   comment '目标列名',
  data_type        varchar(50)     default null               comment '数据类型',
  pk_order         tinyint(1)      default 0                  comment '主键顺序（0非主键）',
  default_value    varchar(200)    default null               comment '默认值',
  transform_type   char(1)         default '0'                comment '转换类型（0无 1格式化 2脚本 3Bean）',
  transform_script text            default null               comment '转换脚本',
  order_num        int(4)          default 0                  comment '处理顺序',
  remark           varchar(500)    default null               comment '备注',
  del_flag         char(1)         not null default '0'       comment '删除标志（0存在 2删除）',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        varchar(64)     default ''                 comment '更新者',
  update_time      datetime                                   comment '更新时间',
  primary key (mapping_id),
  unique key uk_cfm_rc (template_id, sheet_index, row_index, col_index),
  key idx_cfm_table (target_table, target_column)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment = '字段映射配置表（三层架构Tier 3）';

-- ----------------------------
-- 6. 报表分类表
-- ----------------------------
drop table if exists report_category;
create table report_category (
  category_id      bigint(20)      not null                   comment '雪花主键',
  category_name    varchar(100)    not null                   comment '分类名称',
  parent_id        bigint(20)      default 0                  comment '父分类ID',
  ancestors        varchar(500)    default ''                 comment '祖级列表',
  order_num        int(4)          default 0                  comment '显示顺序',
  status           char(1)         default '0'                comment '状态（0正常 1停用）',
  remark           varchar(500)    default null               comment '备注',
  del_flag         char(1)         not null default '0'       comment '删除标志（0存在 2删除）',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        varchar(64)     default ''                 comment '更新者',
  update_time      datetime                                   comment '更新时间',
  primary key (category_id),
  key idx_rcat_parent (parent_id),
  key idx_rcat_order (parent_id, order_num),
  key idx_rcat_status (status)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment = '报表分类表';

-- ----------------------------
-- 7. 报表配置表
-- ----------------------------
drop table if exists report_config;
create table report_config (
  report_id        bigint(20)      not null                   comment '雪花主键',
  report_name      varchar(200)    not null                   comment '报表名称',
  report_code      varchar(64)     not null                   comment '报表编码',
  report_type      char(1)         default '0'                comment '报表类型（0报表 1大屏 2仪表盘）',
  jimu_report_id   varchar(64)     default null               comment 'JimuReport报表ID（type=0时使用）',
  jmbi_id          varchar(64)     default null               comment 'JimuBI大屏/仪表盘ID（type=1/2时使用）',
  category_id      bigint(20)      default null               comment '所属分类ID',
  icon             varchar(100)    default null               comment '图标',
  order_num        int(4)          default 0                  comment '显示顺序',
  status           char(1)         default '0'                comment '状态（0正常 1停用）',
  remark           varchar(500)    default null               comment '备注',
  del_flag         char(1)         not null default '0'       comment '删除标志（0存在 2删除）',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        varchar(64)     default ''                 comment '更新者',
  update_time      datetime                                   comment '更新时间',
  primary key (report_id),
  unique key uk_rc_code (report_code),
  key idx_rc_type (report_type),
  key idx_rc_category (category_id),
  key idx_rc_jimu (jimu_report_id),
  key idx_rc_jmbi (jmbi_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment = '报表配置表';

-- ============================================================
-- 字典数据初始化
-- ============================================================
-- 字典类型
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time) VALUES
('模板状态', 'collect_template_status', '0', 'admin', sysdate()),
('数据业务状态', 'collect_data_status', '0', 'admin', sysdate()),
('报表类型', 'report_config_type', '0', 'admin', sysdate());

-- 字典数据
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, status, create_by, create_time) VALUES
-- 模板状态
(1, '未发布', '0', 'collect_template_status', NULL, 'info', '0', 'admin', sysdate()),
(2, '已发布', '1', 'collect_template_status', NULL, 'success', '0', 'admin', sysdate()),
-- 数据业务状态
(1, '草稿', 'draft', 'collect_data_status', NULL, 'info', '0', 'admin', sysdate()),
(2, '已提交', 'submitted', 'collect_data_status', NULL, 'primary', '0', 'admin', sysdate()),
-- 报表类型
(1, '报表', '0', 'report_config_type', NULL, 'primary', '0', 'admin', sysdate()),
(2, '大屏', '1', 'report_config_type', NULL, 'success', '0', 'admin', sysdate()),
(3, '仪表盘', '2', 'report_config_type', NULL, 'warning', '0', 'admin', sysdate());

-- ============================================================
-- WorkReport（报表管理系统 work_report*）已于 2026-08-30 下线（设计落地计划清单 4.3 选项B）：
-- 全新安装不再创建其表；存量库的表与数据保留，清理方式见 allinone_biz_update.sql 下线段。
-- 历史代码快照见 tag archive/workreport-20260830。
-- ============================================================

-- ----------------------------
-- 填报数据异步导出任务
-- ----------------------------
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

-- ============================================================
-- 供应链采购审批与票据台账（V1.0 基础表）
-- 说明：业务明细、附件和发票分配表在后续阶段追加；本段先建立公共单据、上游业务和固定审批流基础。
-- ============================================================
DROP TABLE IF EXISTS workflow_tasks;
DROP TABLE IF EXISTS workflow_instances;
DROP TABLE IF EXISTS document_versions;
DROP TABLE IF EXISTS version_attachments;
DROP TABLE IF EXISTS document_comments;
DROP TABLE IF EXISTS audit_events;
DROP TABLE IF EXISTS document_attachments;
DROP TABLE IF EXISTS attachment_files;
DROP TABLE IF EXISTS invoice_allocations;
DROP TABLE IF EXISTS invoice_identities;
DROP TABLE IF EXISTS invoice_lines;
DROP TABLE IF EXISTS invoices;
DROP TABLE IF EXISTS receipt_lines;
DROP TABLE IF EXISTS receipts;
DROP TABLE IF EXISTS workflow_configs;
DROP TABLE IF EXISTS purchase_order_lines;
DROP TABLE IF EXISTS purchase_orders;
DROP TABLE IF EXISTS suppliers;
DROP TABLE IF EXISTS documents;
DROP TABLE IF EXISTS number_sequences;
DROP TABLE IF EXISTS warehouses;
DROP TABLE IF EXISTS company_profile;

CREATE TABLE company_profile (
  id              bigint(20)   NOT NULL,
  name            varchar(200) NOT NULL,
  tax_id          varchar(32)  NOT NULL,
  timezone        varchar(64)  NOT NULL DEFAULT 'Asia/Shanghai',
  currency        char(3)      NOT NULL DEFAULT 'CNY',
  revision        int(11)      NOT NULL DEFAULT 1,
  create_time     datetime     NOT NULL,
  update_time     datetime     NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sc_company_tax (tax_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链公司配置';

CREATE TABLE warehouses (
  id              bigint(20)   NOT NULL,
  name            varchar(100) NOT NULL,
  enabled         char(1)      NOT NULL DEFAULT '1',
  create_by       varchar(64)  DEFAULT '',
  create_time     datetime,
  update_by       varchar(64)  DEFAULT '',
  update_time     datetime,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sc_warehouse_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链仓库';

CREATE TABLE number_sequences (
  document_type   varchar(32) NOT NULL,
  business_date   date        NOT NULL,
  next_value      int(11)     NOT NULL DEFAULT 1,
  PRIMARY KEY (document_type, business_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链单号序列';

CREATE TABLE documents (
  id              bigint(20)   NOT NULL,
  type            varchar(32)  NOT NULL,
  number          varchar(64)  NOT NULL,
  creator_id      bigint(20)   NOT NULL,
  creation_key    varchar(80)  NOT NULL,
  approval_status varchar(20)  NOT NULL DEFAULT 'DRAFT',
  current_node    varchar(20)  DEFAULT NULL,
  current_version int(11)      NOT NULL DEFAULT 0,
  revision        int(11)      NOT NULL DEFAULT 0,
  deleted         char(1)      NOT NULL DEFAULT '0',
  create_time     datetime     NOT NULL,
  update_time     datetime     NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sc_document_number (number),
  UNIQUE KEY uk_sc_document_creation (creator_id, creation_key),
  KEY idx_sc_document_type_status (type, approval_status, update_time),
  KEY idx_sc_document_creator (creator_id, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链公共单据';

CREATE TABLE suppliers (
  document_id     bigint(20)   NOT NULL,
  name            varchar(200) NOT NULL,
  tax_id          varchar(32)  NOT NULL,
  dedup_key       varchar(32)  NOT NULL,
  contact         varchar(100) NOT NULL,
  phone           varchar(50)  NOT NULL,
  address         varchar(500) NOT NULL,
  bank_name       varchar(200) NOT NULL,
  account_name    varchar(200) NOT NULL,
  bank_account    varchar(100) NOT NULL,
  attachment_paths varchar(4000),
  remark          varchar(2000),
  PRIMARY KEY (document_id),
  UNIQUE KEY uk_sc_supplier_dedup (dedup_key),
  KEY idx_sc_supplier_tax (tax_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链供应商准入';

CREATE TABLE purchase_orders (
  document_id     bigint(20)   NOT NULL,
  supplier_id     bigint(20)   NOT NULL,
  buyer_id        bigint(20)   NOT NULL,
  expected_date   date,
  currency        char(3)      NOT NULL DEFAULT 'CNY',
  amount_cents    bigint(20)   NOT NULL DEFAULT 0,
  tax_cents       bigint(20)   NOT NULL DEFAULT 0,
  total_cents     bigint(20)   NOT NULL DEFAULT 0,
  remark          varchar(2000),
  PRIMARY KEY (document_id),
  KEY idx_sc_po_supplier (supplier_id),
  KEY idx_sc_po_buyer (buyer_id),
  KEY idx_sc_po_expected (expected_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购订单';

CREATE TABLE purchase_order_lines (
  id              bigint(20)    NOT NULL,
  order_id        bigint(20)    NOT NULL,
  line_no         int(11)       NOT NULL,
  is_current      char(1)       NOT NULL DEFAULT '1',
  name            varchar(200)  NOT NULL,
  specification   varchar(500),
  unit            varchar(32)   NOT NULL,
  quantity_q4     decimal(20,4) NOT NULL,
  price_p6        decimal(20,6) NOT NULL,
  rate_r4         decimal(20,4) NOT NULL DEFAULT 0,
  amount_cents    bigint(20)    NOT NULL DEFAULT 0,
  tax_cents       bigint(20)    NOT NULL DEFAULT 0,
  total_cents     bigint(20)    NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_sc_po_line_current (order_id, line_no, is_current),
  KEY idx_sc_po_line_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购订单明细';

CREATE TABLE workflow_configs (
  id                  bigint(20)   NOT NULL,
  flow_type           varchar(32)  NOT NULL,
  version             int(11)      NOT NULL,
  supervisor_candidates varchar(2000) DEFAULT NULL,
  finance_candidates  varchar(2000) DEFAULT NULL,
  updated_by          bigint(20),
  updated_at          datetime,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sc_flow_config (flow_type, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='固定审批流配置';

CREATE TABLE document_versions (
  id              bigint(20)   NOT NULL,
  document_id     bigint(20)   NOT NULL,
  version_no      int(11)      NOT NULL,
  snapshot_json   longtext     NOT NULL,
  content_hash    varchar(128) NOT NULL,
  submitted_by    bigint(20)   NOT NULL,
  submitted_at    datetime     NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sc_document_version (document_id, version_no),
  KEY idx_sc_version_document (document_id, submitted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链提交版本快照';

CREATE TABLE workflow_instances (
  id              bigint(20)  NOT NULL,
  document_id     bigint(20)  NOT NULL,
  version_id      bigint(20)  NOT NULL,
  config_version  int(11)     NOT NULL,
  status          varchar(20) NOT NULL,
  started_at      datetime    NOT NULL,
  finished_at     datetime,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sc_workflow_version (document_id, version_id),
  KEY idx_sc_workflow_document (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链审批流程实例';

CREATE TABLE workflow_tasks (
  id              bigint(20)  NOT NULL,
  instance_id     bigint(20)  NOT NULL,
  node            varchar(20) NOT NULL,
  sequence_no     int(11)     NOT NULL,
  assignee_id     bigint(20)  NOT NULL,
  status          varchar(20) NOT NULL DEFAULT 'PENDING',
  decision        varchar(20),
  comment         varchar(2000),
  acted_by        bigint(20),
  acted_at        datetime,
  revision        int(11)     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sc_workflow_task (instance_id, sequence_no),
  KEY idx_sc_task_assignee (assignee_id, status, id),
  KEY idx_sc_task_instance (instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链审批任务';

CREATE TABLE audit_events (
  id              bigint(20)   NOT NULL,
  document_id     bigint(20)   NOT NULL,
  version_id      bigint(20),
  actor_id        bigint(20)   NOT NULL,
  actor_snapshot  varchar(200) NOT NULL,
  action          varchar(50)  NOT NULL,
  reason          varchar(2000),
  changes_json    longtext,
  request_id      varchar(64),
  created_at      datetime     NOT NULL,
  PRIMARY KEY (id),
  KEY idx_sc_audit_document (document_id, created_at),
  KEY idx_sc_audit_request (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链审计事件';

CREATE TABLE document_comments (
  id bigint(20) NOT NULL, document_id bigint(20) NOT NULL, version_id bigint(20), author_id bigint(20) NOT NULL,
  author_snapshot varchar(200) NOT NULL, content varchar(2000) NOT NULL, created_at datetime NOT NULL,
  PRIMARY KEY (id), KEY idx_sc_comment_document (document_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链单据追加评论';

CREATE TABLE receipts (
  document_id   bigint(20) NOT NULL,
  order_id      bigint(20) NOT NULL,
  warehouse_id  bigint(20) NOT NULL,
  business_date date NOT NULL,
  confirmed_by  bigint(20) NOT NULL,
  confirmed_at  datetime NOT NULL,
  remark        varchar(2000),
  PRIMARY KEY (document_id),
  KEY idx_sc_receipt_order (order_id, confirmed_at),
  KEY idx_sc_receipt_date (business_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链入库单';

CREATE TABLE receipt_lines (
  id            bigint(20) NOT NULL,
  receipt_id    bigint(20) NOT NULL,
  order_line_id bigint(20) NOT NULL,
  quantity_q4   decimal(20,4) NOT NULL,
  item_snapshot varchar(600) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sc_receipt_line (receipt_id, order_line_id),
  KEY idx_sc_receipt_line_order (order_line_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链入库明细';

CREATE TABLE invoices (
  document_id bigint(20) NOT NULL, order_id bigint(20) NOT NULL, invoice_number varchar(64) NOT NULL,
  invoice_type varchar(32) NOT NULL DEFAULT 'VAT_SPECIAL', issue_date date NOT NULL, seller_name varchar(200) NOT NULL,
  seller_tax_id varchar(32) NOT NULL, buyer_name varchar(200) NOT NULL, buyer_tax_id varchar(32) NOT NULL,
  amount_cents bigint(20) NOT NULL DEFAULT 0, tax_cents bigint(20) NOT NULL DEFAULT 0, total_cents bigint(20) NOT NULL DEFAULT 0,
  difference_note varchar(2000), manual_confirmed char(1) NOT NULL DEFAULT '0', confirmed_content_hash varchar(64), confirmed_by bigint(20), confirmed_at datetime, PRIMARY KEY (document_id),
  KEY idx_sc_invoice_order (order_id), KEY idx_sc_invoice_issue (issue_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链发票';

CREATE TABLE invoice_lines (
  id bigint(20) NOT NULL, invoice_id bigint(20) NOT NULL, line_no int(11) NOT NULL, is_current char(1) NOT NULL DEFAULT '1', order_line_id bigint(20) NOT NULL,
  name varchar(200) NOT NULL, unit varchar(32) NOT NULL, quantity_q4 decimal(20,4) NOT NULL,
  price_p6 decimal(20,6) NOT NULL, rate_r4 decimal(20,4) NOT NULL DEFAULT 0, amount_cents bigint(20) NOT NULL DEFAULT 0,
  tax_cents bigint(20) NOT NULL DEFAULT 0, total_cents bigint(20) NOT NULL DEFAULT 0, PRIMARY KEY (id),
  UNIQUE KEY uk_sc_invoice_line (invoice_id, line_no, is_current), KEY idx_sc_invoice_line_order (order_line_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链发票明细';

CREATE TABLE invoice_identities (
  id bigint(20) NOT NULL, seller_tax_id varchar(32) NOT NULL, invoice_number varchar(64) NOT NULL,
  invoice_id bigint(20) NOT NULL, ever_submitted char(1) NOT NULL DEFAULT '1', created_at datetime NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_sc_invoice_identity (seller_tax_id, invoice_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链发票身份唯一性';

CREATE TABLE invoice_allocations (
  id bigint(20) NOT NULL, invoice_id bigint(20) NOT NULL, version_id bigint(20) NOT NULL, invoice_line_id bigint(20) NOT NULL,
  receipt_line_id bigint(20) NOT NULL, quantity_q4 decimal(20,4) NOT NULL, state varchar(20) NOT NULL DEFAULT 'RESERVED',
  PRIMARY KEY (id), UNIQUE KEY uk_sc_invoice_alloc (invoice_id, version_id, invoice_line_id, receipt_line_id), KEY idx_sc_invoice_alloc_receipt (receipt_line_id, state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链发票入库分配';

CREATE TABLE attachment_files (
  id bigint(20) NOT NULL, original_name varchar(255) NOT NULL, storage_path varchar(1000) NOT NULL,
  media_type varchar(100), size_bytes bigint(20) NOT NULL, sha256 varchar(64) NOT NULL,
  uploaded_by bigint(20) NOT NULL, uploaded_at datetime NOT NULL, PRIMARY KEY (id),
  KEY idx_sc_attachment_hash (sha256), KEY idx_sc_attachment_uploader (uploaded_by, uploaded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链附件文件';

CREATE TABLE document_attachments (
  id bigint(20) NOT NULL, document_id bigint(20) NOT NULL, file_id bigint(20) NOT NULL,
  version_id bigint(20), attachment_type varchar(32) NOT NULL DEFAULT 'SUPPORTING',
  created_by bigint(20) NOT NULL, created_at datetime NOT NULL, PRIMARY KEY (id),
  UNIQUE KEY uk_sc_document_file (document_id, file_id), KEY idx_sc_document_attachment (document_id, version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链单据附件绑定';

CREATE TABLE version_attachments (
  version_id bigint(20) NOT NULL, file_id bigint(20) NOT NULL, name_snapshot varchar(255) NOT NULL,
  sha256 varchar(64) NOT NULL, PRIMARY KEY (version_id, file_id), KEY idx_sc_version_attachment_file (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应链提交版本附件快照';
