-- Flowable 工作流二阶段：已有环境执行本脚本新增业务表和菜单权限。
-- ACT_* 引擎表无需手工创建：应用首次启动时会自动初始化；完成后可设置
-- FLOWABLE_DATABASE_SCHEMA_UPDATE=false 固定引擎表结构。
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
INSERT IGNORE INTO sys_menu VALUES
(2109, '流程待办', 2100, 8, 'workflow-tasks', 'supply/workflow/tasks', '', 'SupplyWorkflowTasks', 1, 0, 'C', '0', '0', 'supply:workflow:task', 'list', 'admin', sysdate(), '', NULL, 'Flowable 采购订单流程待办'),
(2111, '流程设计', 2100, 9, 'workflow-designer', 'supply/workflow/index', '', 'SupplyWorkflowDesigner', 1, 0, 'C', '0', '0', 'supply:workflow:list', 'guide', 'admin', sysdate(), '', NULL, 'Flowable BPMN 流程设计与发布'),
(2112, '流程详情', 2100, 11, 'workflow/detail', 'supply/workflow/detail', '', 'SupplyWorkflowDetail', 1, 0, 'C', '1', '0', 'supply:workflow:task', '#', 'admin', sysdate(), '', NULL, 'Flowable 流程实例详情'),
(2180, '流程定义查看', 2111, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:workflow:list', '#', 'admin', sysdate(), '', NULL, ''),
(2181, '流程定义发布', 2111, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:workflow:deploy', '#', 'admin', sysdate(), '', NULL, ''),
(2182, '流程待办处理', 2109, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'supply:workflow:task', '#', 'admin', sysdate(), '', NULL, '');
