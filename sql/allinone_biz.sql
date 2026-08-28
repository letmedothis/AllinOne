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
-- 报表管理系统 (work_report*) — 全新安装也需要这些表
-- 与 allinone_biz_update.sql 中的定义保持一致（均为 IF NOT EXISTS，重复执行安全）
-- ============================================================

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
