---
name: warm-flow
description: Warm-Flow 国产工作流引擎 — 轻量级、7张表、jar包集成设计器，支持经典+仿钉钉双模式
metadata:
  type: reference
---

# Warm-Flow 工作流引擎开发 Skill

> 🔴 **集成状态：规划中** (Maven 依赖 + 数据库 + 前端均未开始) | 关联：[Luckysheet](luckysheet.md) · [JimuReport](jimureport.md) · [RuoYi-Vue](ruoyi-vue.md)

## 概述

Warm-Flow 是 Dromara 开源社区下的**国产工作流引擎**，以"简洁轻量、五脏俱全"为设计理念，解决 Flowable 和 Activiti 学习成本高、集成难等痛点。

**核心优势：** 仅 **7 张表**（Flowable 约 40+ 张）、通过 **jar 包直接集成设计器**、支持**经典和仿钉钉双模式**、**中文注释**。

**官方资源：**
- 文档: https://warm-flow.dromara.org/
- 官网: https://www.warm-flow.com/
- 演示: http://www.warm-flow.cn（admin/admin123）
- GitHub: https://github.com/dromara/warm-flow
- Gitee: https://gitee.com/dromara/warm-flow

---

## 与 Flowable / Activiti 对比

| 对比项 | Activiti | Flowable | **Warm-Flow** |
|--------|----------|----------|:-------------:|
| 表数量 | ~25 张 | ~40 张（部分 79 张） | **7 张** |
| 设计器 | 需独立部署 | 需额外配置 | **jar 包直接集成** |
| 设计器模式 | 经典 | 经典 | **经典 + 仿钉钉双模式** |
| ORM 支持 | 仅 MyBatis | 仅 MyBatis | **MyBatis/MP/JPA/Easy-Query** |
| 集成难度 | 高 | 高 | **低** |
| 注释语言 | 英文 | 英文 | **中文** |
| 框架支持 | 仅 Spring | 仅 Spring | **Spring + Solon** |
| Java 版本 | 17+ | 17+ | **8 / 11 / 17 / 21** |
| 商业版 | 有 | 有 | **承诺永久免费** |

---

## 核心功能

### 审批操作全覆盖

| 操作 | 说明 |
|------|------|
| **通过** | 审批通过，流转到下一节点 |
| **退回** | 驳回至上一节点或指定节点 |
| **撤销** | 发起人撤销未完成的流程 |
| **拿回** | 办理人拿回已提交但未流转的任务 |
| **终止** | 强制结束流程实例 |
| **转办** | 将任务转给他人办理 |
| **委派** | 指定他人代理办理 |
| **加签 / 减签** | 增加/减少会签人员 |
| **票签（会签）** | 多审批人投票通过/拒绝 |
| **任意跳转** | 流程流转到任意指定节点 |
| **互斥网关** | 条件分支排他路由 |
| **并行网关** | 多分支并行流转 |
| **自动审批** | 满足条件时自动通过 |

### 特色功能

- **流程设计器双模式**：经典模式（BPMN 风格）+ 仿钉钉模式（简洁直观）
- **条件表达式**：内置 `gt/ge/eq/ne/lt/le/like` 等 + **SpEL 表达式**，支持自定义扩展
- **办理人变量表达式**：`$(handler)` 默认 + `#{@user.evalVar(#handler2)}` SpEL 策略
- **4 种监听器**：全局监听器、流程监听器、节点监听器、分派办理人监听器
- **流程图**：自带流程图展示，支持自定义节点状态颜色
- **多租户 & 软删除**：原生支持
- **审批表单自定义**：可在流程定义中配置自定义表单路径
- **办件督办**：支持催办、督办能力

---

## 数据表结构（7 张表）

### 表关联关系

```
flow_definition ──┬── flow_node        (1:N 定义→节点)
                  ├── flow_skip        (1:N 定义→跳转路由)
                  └── flow_instance    (1:N 定义→实例)
                        │
                        └── flow_task  (1:N 实例→待办任务)
                              │
                              └── flow_user (1:N 任务→办理人)

flow_task ──完成→ flow_his_task (完成任务后归档至此表)
```

### 1. flow_definition — 流程定义表

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 主键 |
| `flow_code` | VARCHAR(40) | 流程编码（唯一） |
| `flow_name` | VARCHAR(100) | 流程名称 |
| `category` | VARCHAR(100) | 流程类别 |
| `version` | VARCHAR(20) | 版本号 |
| `is_publish` | BIT(1) | 发布状态（0未发布 1已发布 9失效） |
| `model_value` | VARCHAR(40) | 设计器模型（CLASSICS经典/MIMIC仿钉钉） |
| `form_custom` | CHAR(1) | 是否自定义审批表单（Y/N） |
| `form_path` | VARCHAR(100) | 审批表单路径 |
| `activity_status` | BIT(1) | 激活状态（0挂起 1激活） |
| `listener_type` | VARCHAR(100) | 监听器类型 |
| `listener_path` | VARCHAR(400) | 监听器路径 |
| `ext` | VARCHAR(500) | 扩展字段（JSON） |
| `tenant_id` | VARCHAR(40) | 租户 ID |
| `del_flag` | CHAR(1) | 删除标志 |

### 2. flow_node — 流程节点表

| 字段 | 说明 |
|------|------|
| `node_type` | 节点类型（0开始 1中间 2结束 3互斥网关 4并行网关） |
| `node_code` | 节点编码 |
| `node_name` | 节点名称 |
| `permission_flag` | 权限标识（角色/部门/用户，多个用 `@@` 分隔） |
| `node_ratio` | 签署比例值（会签时需达到的比例） |
| `listener_type` / `listener_path` | 节点监听器配置 |
| `handler_type` / `handler_path` | 办理人处理器配置 |
| `skip_any_node` | 是否允许任意跳转 |

### 3. flow_skip — 节点跳转关联表

| 字段 | 说明 |
|------|------|
| `now_node_code` | 当前节点编码 |
| `next_node_code` | 下一个节点编码 |
| `skip_name` | 跳转名称（如"提交"、"退回"） |
| `skip_type` | 跳转类型（PASS通过 / REJECT退回） |
| `skip_condition` | 跳转条件表达式 |

### 4. flow_instance — 流程实例表

| 字段 | 说明 |
|------|------|
| `definition_id` | 关联 flow_definition.id |
| `business_id` | 业务数据 ID |
| `flow_status` | 流程状态（见下方状态码） |
| `node_type` / `node_code` / `node_name` | 当前节点信息 |
| `variable` | 流程变量（TEXT） |
| `def_json` | 流程定义 JSON 快照 |
| `create_by` | 发起人 |

### 5. flow_task — 待办任务表

| 字段 | 说明 |
|------|------|
| `definition_id` | 关联流程定义 |
| `instance_id` | 关联流程实例 |
| `node_code` / `node_name` | 节点信息 |
| `flow_status` | 任务状态 |
| `form_custom` / `form_path` | 审批表单配置 |

### 6. flow_his_task — 历史任务记录表

| 字段 | 说明 |
|------|------|
| `task_id` | 原任务 ID |
| `handler` | 办理人 |
| `collaboration_type` | 协作类型（审批/转办/委派/会签/票签/加签/减签） |
| `flow_type` | 流转类型 |
| `message` | 审批意见 |
| `start_time` / `end_time` | 任务起止时间 |

### 7. flow_user — 流程用户表（办理人/权限人）

| 字段 | 说明 |
|------|------|
| `type` | 人员类型（1待办审批人 2转办人 3委托人） |
| `processed_by` | 权限人（实际办理人/审批人标识） |
| `associated` | 关联任务表 ID（flow_task.id） |

### 流程状态码

| 状态码 | 含义 |
|:------:|------|
| 0 | 待提交 |
| 1 | 审批中 |
| 2 | 审批通过 |
| 4 | 终止 |
| 5 | 作废 |
| 6 | 撤销 |
| 8 | 已完成 |
| 9 | 已退回 |
| 10 | 失效 |
| 11 | 拿回 |

---

## 快速集成（与 RuoYi-Vue 项目）

### 1. 添加 Maven 依赖

```xml
<dependency>
    <groupId>org.dromara</groupId>
    <artifactId>warm-flow-plugin-ui-sb-web</artifactId>
    <version>1.8.4</version> <!-- 请以最新版本为准 -->
</dependency>
```

### 2. 初始化数据库

执行对应数据库的脚本：
- 首次安装：执行 `warm-flow-all.sql`（全量建表脚本）
- 版本升级：执行对应版本 `warm-flow_x.x.x.sql`

脚本位于依赖 jar 包中，或从 GitHub 仓库获取。

### 3. Security 放行设计器路径

```java
// Spring Security 配置中放行（注意使用 requestMatchers）
.requestMatchers("/warm-flow-ui/**").permitAll()
```

### 4. 配置 application.yml

```yaml
warm-flow:
  # 是否启用流程设计器界面（默认 true）
  ui-enable: true
  # 数据库类型，不配置则自动识别
  database-type: mysql
```

### 5. 实现办理人权限接口

```java
/**
 * 提供流程设计器中的办理人选择数据
 */
@Component
public class FlowHandlerSelectService implements HandlerSelectService {
    
    @Override
    public List<String> getHandlerType() {
        // 返回可选的办理人类型，如：user, role, dept
        return Arrays.asList("user", "role", "dept");
    }
    
    @Override
    public List<HandlerSelectVo> getHandlerSelect(HandlerQuery query) {
        // 根据类型返回具体可选人员/角色列表
        // query.getHandlerType() 区分 user/role/dept
        // query.getKeyword() 搜索关键字
        List<HandlerSelectVo> list = new ArrayList<>();
        if ("user".equals(query.getHandlerType())) {
            // 查询用户列表
        } else if ("role".equals(query.getHandlerType())) {
            // 查询角色列表
        }
        return list;
    }
}
```

### 6. 访问流程设计器

启动项目后访问（前后端不分离）：
```
http://localhost:8080/warm-flow-ui/index
```

如需在若依菜单中集成，添加外链菜单指向该地址。

---

## 核心 API 参考

### 流程定义服务（DefService）

```java
@Autowired
private DefService defService;

// 导入流程定义（XML）
defService.importXml(inputStream);

// 导入流程定义（JSON）
defService.importJson(jsonString);

// 发布流程定义
defService.publish(definitionId);

// 导出流程定义 JSON
defService.exportJson(definitionId);

// 导出流程定义 XML
defService.exportXml(definitionId);
```

### 流程实例服务（InsService）

```java
@Autowired
private InsService insService;

// 发起/开启流程
Instance instance = insService.start(definitionId, handler);
// handler 为当前用户对象，包含 userId, userName

// 跳转（审批流转）
Instance instance = insService.skip(instanceId, getUser()
    .skipType(SkipType.PASS.getKey())
    .permissionFlag(Arrays.asList("role:1", "role:2"))
    .message("同意")
    .data(businessData)
);
```

### 任务服务（TaskService）

```java
@Autowired
private TaskService taskService;

// 审批通过
taskService.skip(instanceId, getUser()
    .skipType(SkipType.PASS.getKey())
    .permissionFlag(permissionFlags)
    .message("审批通过"));

// 退回
taskService.skip(instanceId, getUser()
    .skipType(SkipType.REJECT.getKey())
    .message("退回修改"));

// 任意跳转到指定节点
taskService.skip(instanceId, getUser()
    .skipType(SkipType.PASS.getKey())
    .nodeCode("node_4")
    .permissionFlag(permissionFlags));

// 终止流程
taskService.termination(instanceId, getUser());

// 撤销流程
taskService.withdraw(instanceId, getUser());

// 拿回任务
taskService.rollback(instanceId, getUser());

// 转办
taskService.transfer(instanceId, userId);

// 委派
taskService.delegate(instanceId, userId);

// 加签
taskService.addSignature(instanceId, userId);

// 减签
taskService.deleteSignature(instanceId, userId);
```

### 节点服务（NodeService）

```java
@Autowired
private NodeService nodeService;

// 获取前驱节点列表
List<Node> prevNodes = nodeService.previousNodeList(definitionId, nodeCode);

// 获取后继节点列表
List<Node> nextNodes = nodeService.suffixNodeList(definitionId, nodeCode);
```

### 流程图服务（ChartService）

```java
@Autowired
private ChartService chartService;

// 获取流程实例的流程图数据（用于展示审批进度）
chartService.chartIns(instanceId);

// 获取流程定义的流程图数据（用于设计器预览）
chartService.chartDef(definitionId);
```

---

## 监听器配置

Warm-Flow 提供 **4 种监听器**：

| 监听器类型 | 触发时机 | 配置位置 |
|-----------|---------|---------|
| 全局监听器 | 所有流程的所有节点事件 | 全局配置 |
| 流程监听器 | 指定流程的所有节点事件 | 流程定义 |
| 节点监听器 | 指定节点的特定事件 | 节点配置 |
| 分派办理人监听器 | 节点分配办理人时 | 节点配置 |

### 监听器实现示例

```java
/**
 * 节点监听器 — 当节点进入/离开时触发
 */
@Component
public class MyNodeListener implements GlobalNodeListener {
    
    @Override
    public void notify(NodeContext context) {
        // 获取当前流程上下文
        Long definitionId = context.getDefinitionId();
        Long instanceId = context.getInstanceId();
        String nodeCode = context.getNodeCode();
        String nodeName = context.getNodeName();
        
        // 根据节点事件类型处理
        // 可在此处：发送通知、记录日志、调用外部系统等
        if ("start".equals(context.getEventType())) {
            System.out.println("进入节点: " + nodeName);
        } else if ("end".equals(context.getEventType())) {
            System.out.println("离开节点: " + nodeName);
        }
    }
}
```

### SpEL 表达式监听器

在流程定义中配置监听器路径为 SpEL 表达式：
```
spel@@#(@myListener.notify(#nodeContext))
```

---

## 条件表达式

### 内置表达式

| 表达式 | 含义 | 示例 |
|--------|------|------|
| `gt` | 大于 | `gt@@amount\|1000` |
| `ge` | 大于等于 | `ge@@amount\|1000` |
| `eq` | 等于 | `eq@@type\|1` |
| `ne` | 不等于 | `ne@@type\|0` |
| `lt` | 小于 | `lt@@amount\|500` |
| `le` | 小于等于 | `le@@amount\|500` |
| `like` | 包含 | `like@@name\|张三` |

**格式：** `表达式@@字段名|值`

### SpEL 表达式

```java
spel@@#(@user.eval(#flag))
// 调用 Spring Bean 'user' 的 eval 方法，传入 #flag 变量
```

### 办理人变量表达式

```java
// 默认方式：直接引用 handler 变量
$(handler)

// SpEL 策略：调用 Spring Bean 解析
#{@user.evalVar(#handler2)}
```

---

## 与 Luckysheet 结合的方案（本项目场景）

根据 AllinOne 项目的远期规划，将 **Warm-Flow 工作流**与 **Luckysheet 在线表格**结合，实现"数据填报 → 审批流转 → 报表归档"的完整链路。

### 实现思路

1. **表单模板**：使用 Luckysheet 创建填报模板，关联流程定义的 `form_path`
2. **发起审批**：提交 Luckysheet 数据时，调用 `insService.start()` 开启流程实例
3. **审批流转**：审批人在线查看 Luckysheet 数据，通过 `taskService.skip()` 审批
4. **数据归档**：审批通过后，数据写入业务表并同步至 JimuReport 报表数据源

### 关键代码示意

```java
// 1. 发起审批（提交 Luckysheet 数据时）
@PostMapping("/approval/start")
public AjaxResult startApproval(@RequestBody ApprovalStartDto dto) {
    // 保存 Luckysheet 数据到业务表
    Long businessId = saveSheetData(dto.getSheetData());
    
    // 开启流程实例
    Instance instance = insService.start(dto.getDefinitionId(), getCurrentUser());
    
    // 更新业务表关联流程实例 ID
    updateBusinessFlowId(businessId, instance.getId());
    
    return success(instance);
}

// 2. 审批处理
@PostMapping("/approval/handle")
public AjaxResult handleApproval(@RequestBody ApprovalHandleDto dto) {
    taskService.skip(dto.getInstanceId(), getCurrentUser()
        .skipType(dto.getSkipType())
        .message(dto.getComment())
        .data(dto.getBusinessData()));
    
    // 如果审批通过且是最后一个节点，触发数据归档
    if (SkipType.PASS.getKey().equals(dto.getSkipType())) {
        checkAndArchive(dto.getInstanceId());
    }
    
    return success();
}
```

---

## 常见开发场景

### 场景一：创建一个请假审批流程

1. **流程设计**：访问 `/warm-flow-ui/index`，拖拽设计"发起→部门审批→HR审批→结束"流程
2. **发布流程**：设计完成后点击发布
3. **业务表**：创建 `leave_apply` 表存请假数据
4. **发起审批**：提交请假单时调用 `insService.start(definitionId, user)`
5. **审批列表**：查询 `flow_task` 表获取当前用户的待办任务
6. **审批处理**：调用 `taskService.skip()` 完成审批

### 场景二：查询待办任务列表

```java
@GetMapping("/task/todo")
public TableDataInfo getTodoList() {
    // 查询当前用户的待办任务
    LambdaQueryWrapper<FlowTask> wrapper = new LambdaQueryWrapper<>();
    // 通过 flow_user 表关联查询当前用户的待办
    // ...
    return getDataTable(taskList);
}
```

### 场景三：查看审批进度

```java
@GetMapping("/chart/instance/{instanceId}")
public AjaxResult getChart(@PathVariable Long instanceId) {
    // 获取流程图数据（含当前进度高亮）
    Object chartData = chartService.chartIns(instanceId);
    return success(chartData);
}
```

---

## 前端 API 调用封装

```typescript
// src/api/workflow/definition.ts
import request from '@/utils/request'

// 流程定义列表
export function listDefinition(params: any) {
  return request({ url: '/workflow/definition/list', method: 'get', params })
}

// 发布流程定义
export function publishDefinition(id: number) {
  return request({ url: `/workflow/definition/${id}/publish`, method: 'put' })
}

// 流程实例
export function startProcess(data: { definitionId: number; businessData: any }) {
  return request({ url: '/workflow/instance/start', method: 'post', data })
}

// 待办任务列表
export function listTodoTasks(params: { pageNum: number; pageSize: number }) {
  return request({ url: '/workflow/task/todo', method: 'get', params })
}

// 审批/流转
export function completeTask(data: {
  instanceId: number
  skipType: string  // 'PASS' | 'REJECT'
  message: string
}) {
  return request({ url: '/workflow/task/complete', method: 'post', data })
}

// 获取流程图（审批进度）
export function getFlowChart(instanceId: number) {
  return request({ url: `/workflow/instance/${instanceId}/chart`, method: 'get' })
}
```

**前端页面中使用示例：**
```vue
<template>
  <div>
    <!-- 待办列表 -->
    <el-table :data="taskList" v-loading="loading">
      <el-table-column prop="instanceId" label="流程编号" />
      <el-table-column prop="nodeName" label="当前节点" />
      <el-table-column label="操作">
        <template #default="scope">
          <el-button type="success" @click="handleApprove(scope.row)">通过</el-button>
          <el-button type="danger" @click="handleReject(scope.row)">退回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 流程图弹窗 -->
    <el-dialog v-model="chartVisible" title="审批进度" width="800px">
      <div v-html="flowChartHtml" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { listTodoTasks, completeTask, getFlowChart } from '@/api/workflow/definition'

const handleApprove = async (row: any) => {
  await completeTask({ instanceId: row.instanceId, skipType: 'PASS', message: '同意' })
  getList()
}
</script>
```

---

## 常见问题

### 1. 设计器访问 404
- 检查是否添加了 `warm-flow-plugin-ui-sb-web` 依赖
- 检查 Security 是否放行了 `/warm-flow-ui/**`
- 检查是否配置了 `warm-flow.ui-enable=true`

### 2. 办理人选择器无数据
- 检查是否实现了 `HandlerSelectService` 接口
- 检查返回的数据格式是否符合 `HandlerSelectVo` 规范

### 3. 条件跳转不生效
- 检查条件表达式格式是否正确（`表达式@@字段|值`）
- 检查流程变量中是否包含表达式中使用的字段

### 4. 流程状态代码含义
参考上方"流程状态码"表，统一为数字码，注意 0=待提交 1=审批中 2=通过 等。
