# M0 WIP 基线审计报告(供应链 Demo 之后的在途改动)

> 基线:提交 `b82d451`(2026-09-07,完成供应链采购审批与票据台账 Demo)。
> 审计对象:基线之上的全部未提交改动(33 个 M + 一批 ??)。
> 方法:组长自审(git 盘点、SQL/权限/密钥/杂项、离线编译尝试)+ 两个深审子代理(Flowable 引擎轨道 / 报表定义管理轨道与杂项),全部证据来自代码与 git diff,未修改任何文件。

---

## 1. 总览:在途改动 = 5 条轨道

| # | 轨道 | 涉及文件 | 冻结结论 |
|---|------|---------|---------|
| 1 | 报表定义管理(JimuReport 只读目录 + 配置页下拉) | allinone-report 后端 6 文件 + 前端 report/config 4 文件 + 测试 | ✅ 可提交(3 条可选小修) |
| 2 | 附件上传修正 | request.ts、api/supply.ts(上传行)、supplier/index.vue、DocumentAttachment{Controller,ServiceImpl} | ✅ 可提交 |
| 2b | CollectSheet 图标修复 | components/CollectSheet/index.vue、types/luckysheet.d.ts | ✅ 可提交(建议独立) |
| 3 | 下单只选"已准入供应商" | order/index.vue、SupplierOption.java、PurchaseOrder* 的 supplier-options hunk | ✅ 可提交(需按 hunk 与引擎改动拆开) |
| 4 | **Flowable 7.2 采购订单审批引擎** | pom×2、application.yml、FlowableTaskExecutorConfig、Workflow* 后端 20+ 文件、bpmn20.xml、单测×2、views/supply/workflow×3、bpmn-js 依赖、workflow 表与菜单 SQL | ⛔ **不可冻结**,登记为"二阶段 Demo 分支"(见 §4) |
| 5 | 杂项清理 | runtime/(上传产物)、auto-imports.d.ts(幻影 M)、scripts/start-local-windows.ps1、doc/真实交付计划.md | 单独处理(见 §5) |

---

## 2. 可提交轨道详查

### 2.1 报表定义管理(轨道 1)——✅ 干净,建议 3 条小修

**做什么**:不写引擎、不导入数据;把引擎 `jimu_report` 表目录以**只读**方式暴露,报表配置页的 JimuReport ID 由手填文本改为**下拉选择 + 保存前引用校验**(含逻辑删除)。

- 后端:`GET /report/config/jimu-reports`(`@PreAuthorize('report:config:list')`);Mapper/Service/Impl/XML 各 +1;`validateEngineReference` 在 insert/update 时对 `type='0'` 校验 ID 非空且引擎中存在。
- 新文件 `JimuReportDefinition.java`:只读 resultMap,与 `sql/jimureport.mysql5.7.create.sql` 的 `jimu_report` 列对齐。
- 前端:index.vue / edit.vue 的 el-input → el-select(filterable),rules 加 required。
- 单测:Mockito 纯单测(不依赖 DB),含"引擎中无此 ID 抛 ServiceException"用例。
- 契约核对:XML↔接口↔domain 对齐;别名由 `typeAliasesPackage: com.allinone.**.domain` 解析;无 URL 冲突(字面量 `/jimu-reports` 优先于 `/{reportId}` 模板)。
- 小修(可选):① 权限用 `report:config:list`,而独立编辑菜单 2016 挂的是 `report:config:edit`,只挂 edit 的角色打开 edit.vue 加载下拉会 403,建议核对授权或改挂 `report:config:query`;② 下拉未按引擎侧 type/status 语义过滤;③ resultMap 级联引擎表结构,升级需复核。④ 属行为收紧(`type='0'` 过去允许空 ID),提交说明写明。

### 2.2 附件上传修正(轨道 2)——✅ 自洽

根因:request.ts 全局默认 `Content-Type: application/json`,multipart 无 boundary 被 Spring 拒。修复:拦截器对 FormData 删除该头;api/supply.ts 去掉手工头;上传接口权限改为 `hasAnyPermi(supplier:add/edit, order:edit, invoice:edit)` 放行"仅 add"用户在新建草稿后立即传资质;服务端异常不再透出内部错误(ServiceException 原样上抛、其余记日志给通用文案);供应商资质附件改单选。契约无破坏(仅影响 FormData 请求)。

### 2.3 CollectSheet 图标(轨道 2b)——✅ 独立 UI 修复

显式 import `luckysheet/dist/assets/iconfont/iconfont.css`(工具栏图标为空)+ 类型声明补 module。

### 2.4 下单"已准入供应商"(轨道 3)——✅ 可提交,注意拆分

新增 `GET /supply/orders/supplier-options`(联 documents+suppliers 取 APPROVED)替换前端 `listSuppliers({approvalStatus:'APPROVED'})`。纯新增、无破坏(旧 listSuppliers 仍被供应商页使用)。**注意**:SupplierOption.java 与 PurchaseOrder* 文件内同时混有 Flowable 引擎改动(同文件 2 组内容),提交时须按 hunk 拆分(见 §6)。小瑕疵:下单页按钮无 `v-hasPermi`,只读角色打开会话时 options 会 403(旧行为相近,建议核对是否按 add/edit 藏按钮)。

---

## 3. 编译与测试验证状态(审计的环境限制)

- 本机 Maven 3.9.16 + JDK 17 存在,但本地仓库缺少基础插件构件,**离线编译无法进行**(已实测:allinone-common 在 resources 阶段因缺 maven-filtering 等失败);Flowable 7.2.0 jar 从未缓存。→ **"能编译/能跑"在本机不可证实**,须在有网环境/CI 执行一次 `mvn -pl allinone-supply -am test`(或全量 verify)。
- 未连接真实 MySQL;runtime/ 目录存在 2026-09-07 的上传 PDF,说明此前有实际运行,但 ACT_* 引擎表是否已建、Flowable 是否已实际启动过**未验证**。
- 单测静态评估:两个引擎单测(WorkflowEngineServiceImplTest mock 路径 / WorkflowBpmnSupportTest 纯解析)+ ReportConfigServiceImplTest(Mockito)依赖齐备、写法合理,但从未实际运行;**缺真实 ProcessEngine(H2/MySQL)集成测试**(事务一致性、ACT 建表、claim/complete、REJECT 轨迹、多候选并发均无覆盖)。

---

## 4. Flowable 引擎轨道(轨道 4)——⛔ 不可冻结为基线

结构/契约质量不错(分层清晰、12 个端点全部有 @PreAuthorize 且与菜单权限串、前端 v-hasPermi 对得上;设计器保存/发布有服务端重解析校验;domain/前端契约对齐),但存在**4 个严重阻断项**与一批中/轻问题:

### S1 生命周期与 Flowable 未接线(状态分裂,最严重)
撤回/作废只操作旧表(`DocumentLifecycleServiceImpl`/Mapper 仅 `workflow_tasks/workflow_instances`);全代码库对 Flowable 的 `deleteProcessInstance` 只出现在 REJECT 分支。后果:Flowable 启动的新单被撤回/作废后,documents 变 RETURNED/VOID 但 **ACT_RU 实例与待办仍在**,审批人仍能处理,complete 回写因非 IN_REVIEW 抛异常 → "幽灵待办"、可重复提交出双实例。修复:撤回/作废按 businessKey 同步取消/删除 Flowable 实例(或禁止对引擎单撤回),并加单据状态 vs ACT 实例的对账。

### S2 双引擎缺少"谁在管"的唯一事实
submit 无条件进 Flowable;历史在途单靠旧"审批中心"人工续批;documents 无引擎字段、无自动迁移、无开关。审批人只看新"流程待办"页会漏掉旧单(滞留)。冻结前需:标明引擎归属 + 历史在途单处置/迁移说明 + 旧引擎保留期限。

### S3 完成回写单向依赖、无补偿
唯一推进口 `updateEngineWorkflowState` 硬性要求 IN_REVIEW;一旦单据状态被外部改动,Flowable 动作回滚后无自动恢复,两库漂移无巡检。需显式失败面与对账路径。

### S4 建表与上线配置未落地
`FLOWABLE_DATABASE_SCHEMA_UPDATE=true` + 业务库账号建 ACT_* 表(需 DDL 权限),仅注释级指引;且 `flowable.async-executor-activate` 键名(官方样例为 `flowable.process.async-executor-activate`)是否被 Flowable 7 Boot 绑定**未验证**,若是死配置则自定义线程池无效。需:初始化脚本固化"首启建表→置 false"、明确 DDL 账号/备份影响、验证属性键与 Flowable 7.2.0 + Boot 3.5 + MySQL Connector/J 9.2 真实兼容。

### 中等问题(择要)
- M1 节点词汇不一致:submit 写死 `current_node='SUPERVISOR'`,complete 写 Flowable taskDefinitionKey;documents.current_node varchar(20) vs BPMN 节点 id 可 63 字符 → 截断风险。
- M2 候选人自审回归:非提交人互斥只在旧 SUPERVISOR_CONFIG 保留;ROLE/USER/DEPT_LEADER/INITIATOR_MANAGER 无互斥,部门负责人=发起人时可自审。
- M3 submit 仍被旧 workflow_configs 绑架(要求 legacy 主管配置非空),与"按 BPMN 首节点类型解析"的引擎语义矛盾。
- M4 启停按单版本 + 启动取 `latestVersion().active()` → 停用最新版自动回落旧版;且 Flowable 默认自动部署 classpath*:/processes/** 与手工 deploy 双入口,重启可能重复产生自动部署版本。
- M5 异常吞错丢 cause(PurchaseOrderServiceImpl L88、WorkflowEngineServiceImpl L367/373)。
- M6 关键行为无真实引擎集成测试。
- M7 审批上下文缺口:任务页看不到被审订单行/金额,无法页内决策。

### 轻问题(择要)
L1 PO 侧 insertWorkflow/insertTask 成死代码(供应商/发票仍用旧表,勿删表);L2 audit_events 动作词汇分裂('APPROVE' vs 'FLOWABLE_APPROVE');L3 SQL/菜单三处重复(flowable_workflow_menu.sql 与 allinone_biz_update.sql、allinone_menu.sql);L4 /candidates 全量用户/角色无分页最小化且仅 list 权限即可看 XML 与候选人清单;L5 状态字符串字面量(建议用 SupplyApprovalStatus 枚举);L6 REJECT(deleteProcessInstance)实例在前端只显示"已结束",无退回语义。

**登记结论**:按"二阶段 Demo 分支(不可冻结)"状态登记;修复 S1–S4 + 真实构建/集成测试后再进基线评审。

---

## 5. 杂项清理(轨道 5)

- `runtime/`(含 2026-09-07 实际上传 PDF)未被 .gitignore 覆盖 → **绝不能入库**,建议 `.gitignore` 增加 `runtime/`(如另有 upload 路径也一并确认);`logs/` 同样建议忽略。
- `auto-imports.d.ts`:git 恒报 M 但 blob 逐字节一致 → dev watcher 幻影 M,无实质改动,提交前 `git status` 复核,不需要特意提交。
- `scripts/start-local-windows.ps1`:本地开发脚本(帮助注释含 WSL MySQL/Redis 123456,JWT 未设时生成随机密钥),不属泄密,但**提交与否需你确认**。
- `doc/真实交付_供应链采购票据系统_差距与里程碑计划.md`:文档,单独提交,与代码组无关。
- 依赖:package.json/lock 仅新增 bpmn-js 18.16.1(+20 个纯 bpmn.io 传递依赖),lock 与 manifest 一致,归属引擎组。

---

## 6. 建议的提交分组与执行顺序(待你确认后执行)

| 步 | 内容 | 方式 | 说明 |
|---|---|---|---|
| 6.1 | .gitignore 增 `runtime/`、`logs/` | 普通提交 | 防上传产物入库 |
| 6.2 | 轨道 1 报表定义管理(全部 11 文件) | 一个提交 | 建议顺手做 ① 权限串小修 |
| 6.3 | 轨道 2 附件修正(request.ts、附件 controller/service、supplier/index.vue 附件 hunk)+ 2b CollectSheet 图标 | 一个或两个提交 | 与 6.4 同文件的 api/supply.ts 需按 hunk 分离 |
| 6.4 | 轨道 3 supplier-options(SupplierOption.java + 后端 hunk + order/index.vue + api/types 行) | hunk 级 `git add -p` | PurchaseOrder*、api/supply.ts、types/api/supply 中仅取 supplier-options 相关行 |
| 6.5 | **Flowable 引擎轨**:新建分支 `feat/flowable-purchase-approval`,在分支上整组提交(含 bpmn-js、pom/yml、Workflow*、workflow SQL/菜单、views/supply/workflow、引擎单测) | 分支提交 | 保留全部 WIP 又不污染基线;主分支维持"可提交组 + 未含引擎"的干净状态 |
| 6.6 | 文档提交(doc/真实交付计划、本报告) | 一个提交 | 与代码无关 |

> 关键风险提示:6.3/6.4 必须**逐 hunk 拆分**——api/supply.ts、types/api/supply/index.ts、PurchaseOrder* 等文件同时含"附件/下拉(干净)"与"Flowable(引擎)"两类改动;拆分后主分支应保持**可编译**(引擎改动全部留在 6.5 分支)。
> 遗留待办(不阻塞提交,属 M1 开发):S1–S4 修复、真实构建与集成测试、flowable_workflow_menu.sql 三处去重。

---

## 7. 证据索引(关键行号)

- 引擎接线:`PurchaseOrderServiceImpl` submit(L72-89)调用 `workflowEngineService.startPurchaseOrder`,注释"历史 workflow_* 实例仍由原审批中心继续处理";`WorkflowEngineController` 12 端点全 @PreAuthorize;`application.yml` L165-169 flowable 段;`FlowableTaskExecutorConfig` 线程池 4/8/1000。
- 生命周期未接线:`DocumentLifecycleMapper.xml` L9/L13 仅旧表;`WorkflowEngineServiceImpl` L234 是唯一 deleteProcessInstance。
- SQL 三处重复:workflow 表/菜单种子同内容出现在 `allinone_biz.sql`、`allinone_biz_update.sql`、`flowable_workflow_menu.sql`,菜单另在 `allinone_menu.sql`。
- 菜单权限串一致性:后端 `supply:workflow:{list,deploy,task}` ↔ 前端 v-hasPermi ↔ sql 种子(menu 2109/2111/2112 + 按钮 2180-2182)对得上。
- 编译环境限制:本机离线编译实测失败(缺 maven-filtering 等基础构件),Flowable jar 未缓存。
