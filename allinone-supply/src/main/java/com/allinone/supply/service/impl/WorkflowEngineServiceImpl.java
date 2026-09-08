package com.allinone.supply.service.impl;

import com.allinone.common.exception.ServiceException;
import com.allinone.common.utils.DateUtils;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.StringUtils;
import com.allinone.common.utils.uuid.IdUtils;
import com.allinone.supply.domain.PurchaseOrder;
import com.allinone.supply.domain.WorkflowActionLog;
import com.allinone.supply.domain.WorkflowCandidateOption;
import com.allinone.supply.domain.WorkflowDeploymentRequest;
import com.allinone.supply.domain.WorkflowDesign;
import com.allinone.supply.domain.WorkflowInstanceDetail;
import com.allinone.supply.domain.WorkflowProcessDefinition;
import com.allinone.supply.domain.WorkflowTask;
import com.allinone.supply.domain.WorkflowTaskDecision;
import com.allinone.supply.domain.WorkflowTimelineItem;
import com.allinone.supply.mapper.PurchaseOrderMapper;
import com.allinone.supply.mapper.WorkflowEngineMapper;
import com.allinone.supply.service.IWorkflowEngineService;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Flowable 设计、发布、任务和轨迹适配层；二阶段范围仍只接管采购订单。 */
@Service
public class WorkflowEngineServiceImpl implements IWorkflowEngineService {
    public static final String PURCHASE_ORDER_PROCESS_KEY = "purchase-order-approval";
    private static final String DEFAULT_RESOURCE = "processes/purchase-order-approval.bpmn20.xml";
    private static final String ACTION_APPROVE = "APPROVE";
    private static final String ACTION_REJECT = "REJECT";

    @Autowired private RepositoryService repositoryService;
    @Autowired private RuntimeService runtimeService;
    @Autowired private TaskService taskService;
    @Autowired private HistoryService historyService;
    @Autowired private IdentityService identityService;
    @Autowired private PurchaseOrderMapper purchaseOrderMapper;
    @Autowired private WorkflowEngineMapper workflowEngineMapper;
    @Autowired private com.allinone.supply.support.TaskTransferAuthorization transferAuthorization;

    @Override
    public void startPurchaseOrder(PurchaseOrder order, Long versionId, Long approverUserId) {
        if (order == null || order.getDocumentId() == null || versionId == null || approverUserId == null) {
            throw new ServiceException("采购订单流程启动参数不完整");
        }
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(PURCHASE_ORDER_PROCESS_KEY).latestVersion().active().singleResult();
        if (definition == null) throw new ServiceException("采购订单流程定义未部署或已停用，请先发布可用流程");
        Map<String, Object> variables = new HashMap<>();
        variables.put("documentId", String.valueOf(order.getDocumentId()));
        variables.put("documentNumber", order.getNumber());
        variables.put("documentVersionId", String.valueOf(versionId));
        variables.put("startedBy", String.valueOf(order.getCreatorId()));
        variables.put("approverUserId", String.valueOf(approverUserId));
        resolveDynamicAssignees(definition, order.getCreatorId(), variables);
        identityService.setAuthenticatedUserId(String.valueOf(order.getCreatorId()));
        try {
            ProcessInstance instance = runtimeService.startProcessInstanceById(
                    definition.getId(), String.valueOf(order.getDocumentId()), variables);
            if (instance == null) throw new ServiceException("采购订单流程启动失败");
        } finally {
            identityService.setAuthenticatedUserId(null);
        }
    }

    @Override
    public List<WorkflowProcessDefinition> selectProcessDefinitions() {
        return repositoryService.createProcessDefinitionQuery().processDefinitionKey(PURCHASE_ORDER_PROCESS_KEY)
                .orderByProcessDefinitionVersion().desc().list().stream().map(this::toDefinition).collect(Collectors.toList());
    }

    @Override
    public List<WorkflowCandidateOption> selectCandidateOptions() {
        List<WorkflowCandidateOption> options = new ArrayList<>();
        options.add(option(WorkflowBpmnSupport.TYPE_SUPERVISOR_CONFIG, "", "采购主管配置", "沿用供应链流程配置中的首位有效主管"));
        options.add(option(WorkflowBpmnSupport.TYPE_INITIATOR_MANAGER, "", "发起人部门负责人", "按采购订单创建人的所属部门动态查找"));
        options.addAll(workflowEngineMapper.selectUserOptions());
        options.addAll(workflowEngineMapper.selectRoleOptions());
        options.addAll(workflowEngineMapper.selectDepartmentLeaderOptions());
        return options;
    }

    @Override
    public WorkflowDesign selectDesign(String processKey) {
        requireProcessKey(processKey);
        WorkflowDesign design = workflowEngineMapper.selectDesignByProcessKey(processKey);
        if (design != null) return design;
        WorkflowDesign empty = new WorkflowDesign();
        empty.setProcessKey(PURCHASE_ORDER_PROCESS_KEY); empty.setName("采购订单审批");
        empty.setBpmnXml(readClasspath(DEFAULT_RESOURCE)); empty.setStatus("DRAFT"); empty.setRevision(0);
        return empty;
    }

    @Override
    @Transactional
    public WorkflowDesign saveDraft(WorkflowDesign design) {
        validateDesign(design);
        Date now = DateUtils.getNowDate();
        String username = SecurityUtils.getUsername();
        WorkflowDesign current = workflowEngineMapper.selectDesignByProcessKey(PURCHASE_ORDER_PROCESS_KEY);
        if (current == null) {
            if (!StringUtils.isEmpty(design.getId())) throw new ServiceException("流程草稿已不存在，请刷新后重试");
            design.setId(String.valueOf(IdUtils.nextLongId())); design.setRevision(0); design.setStatus("DRAFT");
            design.setCreateBy(username); design.setCreateTime(now); design.setUpdateBy(username); design.setUpdateTime(now);
            if (workflowEngineMapper.insertDesign(design) != 1) throw new ServiceException("流程草稿保存失败");
        } else {
            if (!Objects.equals(current.getId(), design.getId()) || !Objects.equals(current.getRevision(), design.getRevision())) {
                throw new ServiceException("流程草稿已被其他用户修改，请刷新后重试");
            }
            design.setUpdateBy(username); design.setUpdateTime(now);
            if (workflowEngineMapper.updateDesignDraft(design) != 1) throw new ServiceException("流程草稿已变化，请刷新后重试");
        }
        return workflowEngineMapper.selectDesignByProcessKey(PURCHASE_ORDER_PROCESS_KEY);
    }

    @Override
    @Transactional
    public WorkflowProcessDefinition publish(WorkflowDesign design) {
        WorkflowDesign saved = saveDraft(design);
        WorkflowProcessDefinition definition = deployXml(saved.getName(), saved.getBpmnXml());
        Date now = DateUtils.getNowDate();
        if (workflowEngineMapper.updateDesignPublished(saved.getId(), saved.getRevision(), definition.getId(),
                definition.getVersion(), SecurityUtils.getUsername(), now) != 1) throw new ServiceException("流程草稿发布状态保存失败");
        return definition;
    }

    @Override
    @Transactional
    public WorkflowProcessDefinition deploy(WorkflowDeploymentRequest request) {
        if (request == null) throw new ServiceException("流程发布参数不能为空");
        WorkflowDesign design = selectDesign(StringUtils.isEmpty(request.getProcessKey()) ? PURCHASE_ORDER_PROCESS_KEY : request.getProcessKey());
        design.setName(StringUtils.isEmpty(request.getDeploymentName()) ? design.getName() : request.getDeploymentName().trim());
        design.setBpmnXml(request.getBpmnXml());
        return publish(design);
    }

    @Override
    public String selectDefinitionXml(String definitionId) {
        return readDefinitionXml(requireDefinition(definitionId));
    }

    @Override
    @Transactional
    public int changeDefinitionState(String definitionId, boolean suspended) {
        ProcessDefinition definition = requireDefinition(definitionId);
        if (suspended && !definition.isSuspended()) repositoryService.suspendProcessDefinitionById(definitionId, false, null);
        if (!suspended && definition.isSuspended()) repositoryService.activateProcessDefinitionById(definitionId, false, null);
        return 1;
    }

    @Override
    public List<WorkflowTask> selectMyPurchaseOrderTasks() {
        String userId = String.valueOf(SecurityUtils.getUserId());
        Map<String, Task> tasks = new LinkedHashMap<>();
        addTasks(tasks, taskService.createTaskQuery().processDefinitionKey(PURCHASE_ORDER_PROCESS_KEY)
                .taskAssignee(userId).active().orderByTaskCreateTime().desc().list());
        addTasks(tasks, taskService.createTaskQuery().processDefinitionKey(PURCHASE_ORDER_PROCESS_KEY)
                .taskCandidateUser(userId).active().orderByTaskCreateTime().desc().list());
        List<String> roleKeys = workflowEngineMapper.selectUserRoleKeys(SecurityUtils.getUserId());
        if (!roleKeys.isEmpty()) addTasks(tasks, taskService.createTaskQuery().processDefinitionKey(PURCHASE_ORDER_PROCESS_KEY)
                .taskCandidateGroupIn(roleKeys).active().orderByTaskCreateTime().desc().list());
        return tasks.values().stream().sorted((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()))
                .map(this::toTask).collect(Collectors.toList());
    }

    @Override
    public List<WorkflowInstanceDetail> selectMyStartedInstances() {
        String userId = String.valueOf(SecurityUtils.getUserId());
        return historyService.createHistoricProcessInstanceQuery().processDefinitionKey(PURCHASE_ORDER_PROCESS_KEY)
                .variableValueEquals("startedBy", userId).includeProcessVariables().orderByProcessInstanceStartTime().desc()
                .list().stream().map(this::toInstanceSummary).collect(Collectors.toList());
    }

    @Override
    public WorkflowInstanceDetail selectInstanceDetail(String instanceId) {
        if (StringUtils.isEmpty(instanceId)) throw new ServiceException("流程实例不能为空");
        HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery().processInstanceId(instanceId)
                .includeProcessVariables().singleResult();
        if (instance == null || !PURCHASE_ORDER_PROCESS_KEY.equals(instance.getProcessDefinitionKey())) throw new ServiceException("流程实例不存在");
        requireInstanceViewer(instance);
        WorkflowInstanceDetail detail = toInstanceSummary(instance);
        Map<String, WorkflowActionLog> logs = workflowEngineMapper.selectActionLogs(instanceId).stream()
                .collect(Collectors.toMap(WorkflowActionLog::getTaskId, item -> item, (a, b) -> b));
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery().processInstanceId(instanceId)
                .orderByHistoricTaskInstanceStartTime().asc().list();
        detail.setTimeline(tasks.stream().map(task -> toTimeline(task, logs.get(task.getId()))).collect(Collectors.toList()));
        return detail;
    }

    @Override
    @Transactional
    public int completePurchaseOrderTask(String taskId, WorkflowTaskDecision decision) {
        if (StringUtils.isEmpty(taskId) || decision == null || StringUtils.isEmpty(decision.getAction())) throw new ServiceException("待办任务和审批动作不能为空");
        String action = decision.getAction().trim().toUpperCase(Locale.ROOT);
        if (!ACTION_APPROVE.equals(action) && !ACTION_REJECT.equals(action)) throw new ServiceException("不支持的审批动作");
        Task task = taskService.createTaskQuery().taskId(taskId).processDefinitionKey(PURCHASE_ORDER_PROCESS_KEY).singleResult();
        String userId = String.valueOf(SecurityUtils.getUserId());
        if (task == null || !canHandle(task, userId)) throw new ServiceException("待办不存在、已处理或无权处理");
        ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
        Long documentId = toDocumentId(instance == null ? null : instance.getBusinessKey());
        transferAuthorization.lockDocument(documentId);
        task = taskService.createTaskQuery().taskId(taskId).processDefinitionKey(PURCHASE_ORDER_PROCESS_KEY).active().singleResult();
        if (task == null || !canHandle(task, userId)) throw new ServiceException("待办已变化或无权处理，请刷新后重试");
        requireFlowableDocument(documentId);
        if (StringUtils.isEmpty(task.getAssignee())) taskService.claim(task.getId(), userId);
        String comment = StringUtils.defaultString(decision.getComment()).trim();
        if (comment.length() > 2000) throw new ServiceException("审批意见不能超过 2000 个字符");
        WorkflowActionLog log = new WorkflowActionLog();
        log.setId(String.valueOf(IdUtils.nextLongId())); log.setProcessInstanceId(task.getProcessInstanceId()); log.setTaskId(task.getId());
        log.setTaskDefinitionKey(task.getTaskDefinitionKey()); log.setAction(action); log.setComment(comment);
        log.setActorId(userId); log.setActorName(SecurityUtils.getUsername()); log.setCreatedAt(DateUtils.getNowDate());
        if (workflowEngineMapper.insertActionLog(log) != 1) throw new ServiceException("审批记录保存失败");
        Task nextTask = null;
        if (ACTION_REJECT.equals(action)) {
            runtimeService.deleteProcessInstance(task.getProcessInstanceId(), "REJECTED");
        } else {
            Map<String, Object> variables = new HashMap<>(); variables.put("approvalAction", action); variables.put("approvalComment", comment);
            taskService.complete(task.getId(), variables);
            nextTask = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
        }
        String status = ACTION_REJECT.equals(action) ? "RETURNED" : nextTask == null ? "APPROVED" : "IN_REVIEW";
        String currentNode = nextTask == null ? null : nextTask.getTaskDefinitionKey();
        if (purchaseOrderMapper.updateEngineWorkflowState(documentId, status, currentNode, DateUtils.getNowDate()) == 0) throw new ServiceException("订单状态已变化，请刷新后重试");
        Long versionId = purchaseOrderMapper.selectCurrentVersionId(documentId);
        if (versionId != null) purchaseOrderMapper.insertAudit(IdUtils.nextLongId(), documentId, versionId, SecurityUtils.getUserId(), SecurityUtils.getUsername(), "FLOWABLE_" + action, DateUtils.getNowDate());
        return 1;
    }

    @Override
    @Transactional
    public int transferPurchaseOrderTask(String taskId, Long assigneeId) {
        if (StringUtils.isEmpty(taskId) || assigneeId == null) throw new ServiceException("任务和接替人不能为空");
        Task task = taskService.createTaskQuery().taskId(taskId).processDefinitionKey(PURCHASE_ORDER_PROCESS_KEY).active().singleResult();
        if (task == null) throw new ServiceException("流程任务不存在或已处理");
        ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
        Long documentId = toDocumentId(instance == null ? null : instance.getBusinessKey());
        transferAuthorization.requireOperator(documentId, "supply:workflow:transfer");
        transferAuthorization.lockDocument(documentId);
        task = taskService.createTaskQuery().taskId(taskId).processDefinitionKey(PURCHASE_ORDER_PROCESS_KEY).active().singleResult();
        if (task == null) throw new ServiceException("流程任务不存在或已处理");
        requireFlowableDocument(documentId);
        transferAuthorization.requireRecipient(documentId, assigneeId, "supply:workflow:task");
        if (workflowEngineMapper.selectActionLogs(task.getProcessInstanceId()).stream()
                .anyMatch(log -> String.valueOf(assigneeId).equals(log.getActorId()) && ACTION_APPROVE.equals(log.getAction()))) {
            throw new ServiceException("接替人已办理本轮其他审批节点");
        }
        requireTransferNodeRecipient(task, documentId, assigneeId);
        if (String.valueOf(assigneeId).equals(task.getAssignee())) throw new ServiceException("接替人不能与当前办理人相同");
        WorkflowActionLog log = new WorkflowActionLog();
        log.setId(String.valueOf(IdUtils.nextLongId())); log.setProcessInstanceId(task.getProcessInstanceId());
        log.setTaskId(task.getId()); log.setTaskDefinitionKey(task.getTaskDefinitionKey()); log.setAction("TRANSFER");
        log.setComment("原办理人：" + task.getAssignee() + "；接替人：" + assigneeId);
        log.setActorId(String.valueOf(SecurityUtils.getUserId())); log.setActorName(SecurityUtils.getUsername());
        log.setCreatedAt(DateUtils.getNowDate());
        taskService.setAssignee(task.getId(), String.valueOf(assigneeId));
        if (workflowEngineMapper.insertActionLog(log) != 1) throw new ServiceException("转交审计保存失败");
        return 1;
    }

    private void requireTransferNodeRecipient(Task task, Long documentId, Long assigneeId) {
        WorkflowBpmnSupport.Assignment assignment = WorkflowBpmnSupport.parseAndValidate(
                readDefinitionXml(requireDefinition(task.getProcessDefinitionId())), PURCHASE_ORDER_PROCESS_KEY)
                .assignments().stream().filter(item -> item.nodeId().equals(task.getTaskDefinitionKey()))
                .findFirst().orElseThrow(() -> new ServiceException("当前节点缺少审批资格配置，不能转交"));
        String recipient = String.valueOf(assigneeId);
        boolean eligible = switch (assignment.type()) {
            case WorkflowBpmnSupport.TYPE_SUPERVISOR_CONFIG -> transferAuthorization.isSupervisor(assigneeId);
            case WorkflowBpmnSupport.TYPE_ROLE -> transferAuthorization.hasRole(assigneeId, assignment.value());
            case WorkflowBpmnSupport.TYPE_USER -> recipient.equals(assignment.value());
            case WorkflowBpmnSupport.TYPE_DEPT_LEADER -> recipient.equals(workflowEngineMapper.selectLeaderUserIdByDeptId(assignment.value()));
            case WorkflowBpmnSupport.TYPE_INITIATOR_MANAGER -> recipient.equals(workflowEngineMapper.selectInitiatorLeaderUserId(workflowEngineMapper.selectDocumentCreatorId(documentId)));
            default -> false;
        };
        if (!eligible) throw new ServiceException("接替人不符合已发布流程的当前节点资格；固定人员节点不能任意换人");
    }

    private WorkflowProcessDefinition deployXml(String name, String xml) {
        validateBpmnReferences(xml);
        Deployment deployment = repositoryService.createDeployment().name(StringUtils.isEmpty(name) ? "采购订单审批流程" : name.trim())
                .addString(DEFAULT_RESOURCE, xml.trim()).deploy();
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId())
                .processDefinitionKey(PURCHASE_ORDER_PROCESS_KEY).singleResult();
        if (definition == null) {
            repositoryService.deleteDeployment(deployment.getId(), true);
            throw new ServiceException("发布结果中未找到采购订单流程定义");
        }
        return toDefinition(definition);
    }

    private void validateDesign(WorkflowDesign design) {
        if (design == null) throw new ServiceException("流程草稿不能为空");
        requireProcessKey(design.getProcessKey());
        if (StringUtils.isEmpty(design.getName()) || design.getName().trim().length() > 100) throw new ServiceException("流程名称不能为空且不能超过 100 个字符");
        if (design.getBpmnXml() != null && design.getBpmnXml().length() > 2_000_000) throw new ServiceException("流程定义不能超过 2MB");
        validateBpmnReferences(design.getBpmnXml());
        design.setName(design.getName().trim()); design.setBpmnXml(design.getBpmnXml().trim());
    }

    private void validateBpmnReferences(String xml) {
        WorkflowBpmnSupport.ParsedBpmn parsed = WorkflowBpmnSupport.parseAndValidate(xml, PURCHASE_ORDER_PROCESS_KEY);
        for (WorkflowBpmnSupport.Assignment assignment : parsed.assignments()) {
            if (WorkflowBpmnSupport.TYPE_USER.equals(assignment.type()) && workflowEngineMapper.countActiveUser(assignment.value()) != 1) {
                throw new ServiceException("节点“" + assignment.nodeName() + "”选择的用户不存在或已停用");
            }
            if (WorkflowBpmnSupport.TYPE_ROLE.equals(assignment.type()) && workflowEngineMapper.countActiveRole(assignment.value()) != 1) {
                throw new ServiceException("节点“" + assignment.nodeName() + "”选择的角色不存在或已停用");
            }
            if (WorkflowBpmnSupport.TYPE_DEPT_LEADER.equals(assignment.type()) && StringUtils.isEmpty(workflowEngineMapper.selectLeaderUserIdByDeptId(assignment.value()))) {
                throw new ServiceException("节点“" + assignment.nodeName() + "”选择的部门未配置有效负责人");
            }
        }
    }

    private void resolveDynamicAssignees(ProcessDefinition definition, Long starterId, Map<String, Object> variables) {
        for (WorkflowBpmnSupport.Assignment assignment : WorkflowBpmnSupport.parseAndValidate(readDefinitionXml(definition), PURCHASE_ORDER_PROCESS_KEY).assignments()) {
            String userId = null;
            if (WorkflowBpmnSupport.TYPE_DEPT_LEADER.equals(assignment.type())) userId = workflowEngineMapper.selectLeaderUserIdByDeptId(assignment.value());
            if (WorkflowBpmnSupport.TYPE_INITIATOR_MANAGER.equals(assignment.type())) userId = workflowEngineMapper.selectInitiatorLeaderUserId(starterId);
            if ((WorkflowBpmnSupport.TYPE_DEPT_LEADER.equals(assignment.type()) || WorkflowBpmnSupport.TYPE_INITIATOR_MANAGER.equals(assignment.type())) && StringUtils.isEmpty(userId)) {
                throw new ServiceException("节点“" + assignment.nodeName() + "”没有可用审批人，请检查部门负责人配置");
            }
            if (userId != null) variables.put("wfAssignee_" + assignment.nodeId(), userId);
        }
    }

    private void requireFlowableDocument(Long documentId) {
        if (!"FLOWABLE".equals(workflowEngineMapper.selectDocumentWorkflowEngine(documentId))) {
            throw new ServiceException("该采购订单仍由原审批中心处理，请在“审批中心”操作");
        }
        String status = workflowEngineMapper.selectDocumentApprovalStatus(documentId);
        if (!"IN_REVIEW".equals(status)) {
            throw new ServiceException("订单状态已变化（当前：" + status + "），请刷新后重试");
        }
    }

    private boolean canHandle(Task task, String userId) {
        if (userId.equals(task.getAssignee())) return true;
        if (!StringUtils.isEmpty(task.getAssignee())) return false;
        if (taskService.createTaskQuery().taskId(task.getId()).taskCandidateUser(userId).count() > 0) return true;
        List<String> roles = workflowEngineMapper.selectUserRoleKeys(SecurityUtils.getUserId());
        return !roles.isEmpty() && taskService.createTaskQuery().taskId(task.getId()).taskCandidateGroupIn(roles).count() > 0;
    }

    private void requireInstanceViewer(HistoricProcessInstance instance) {
        if (SecurityUtils.isAdmin()) return;
        String userId = String.valueOf(SecurityUtils.getUserId());
        if (userId.equals(String.valueOf(instance.getProcessVariables().get("startedBy")))) return;
        if (historyService.createHistoricTaskInstanceQuery().processInstanceId(instance.getId()).taskAssignee(userId).count() > 0) return;
        if (selectMyPurchaseOrderTasks().stream().anyMatch(task -> instance.getId().equals(task.getProcessInstanceId()))) return;
        Long documentId = parseLong(instance.getBusinessKey());
        if (documentId != null && SecurityUtils.getUserId().equals(workflowEngineMapper.selectDocumentCreatorId(documentId))) return;
        throw new ServiceException("无权查看该流程实例");
    }

    private WorkflowInstanceDetail toInstanceSummary(HistoricProcessInstance source) {
        WorkflowInstanceDetail target = new WorkflowInstanceDetail();
        target.setId(source.getId()); target.setProcessDefinitionId(source.getProcessDefinitionId());
        target.setProcessDefinitionName(source.getProcessDefinitionName()); target.setProcessDefinitionVersion(source.getProcessDefinitionVersion());
        target.setDocumentId(source.getBusinessKey()); target.setDocumentNumber(stringVariable(source, "documentNumber"));
        String startedBy = stringVariable(source, "startedBy"); target.setStartedBy(startedBy);
        target.setStartedByName(StringUtils.isEmpty(startedBy) ? null : workflowEngineMapper.selectUserDisplayName(startedBy));
        target.setStartTime(source.getStartTime()); target.setEndTime(source.getEndTime()); target.setStatus(source.getEndTime() == null ? "RUNNING" : "COMPLETED");
        return target;
    }

    private WorkflowTimelineItem toTimeline(HistoricTaskInstance task, WorkflowActionLog log) {
        WorkflowTimelineItem item = new WorkflowTimelineItem();
        item.setTaskId(task.getId()); item.setNodeId(task.getTaskDefinitionKey()); item.setNodeName(task.getName()); item.setAssigneeId(task.getAssignee());
        item.setAssigneeName(StringUtils.isEmpty(task.getAssignee()) ? null : workflowEngineMapper.selectUserDisplayName(task.getAssignee()));
        item.setStartTime(task.getStartTime()); item.setEndTime(task.getEndTime()); item.setStatus(task.getEndTime() == null ? "PENDING" : "COMPLETED");
        if (log != null) {
            item.setAction(log.getAction()); item.setComment(log.getComment());
            if (!"TRANSFER".equals(log.getAction())) { item.setAssigneeId(log.getActorId()); item.setAssigneeName(log.getActorName()); }
        }
        return item;
    }

    private WorkflowTask toTask(Task source) {
        WorkflowTask target = new WorkflowTask();
        target.setId(source.getId()); target.setName(source.getName()); target.setTaskDefinitionKey(source.getTaskDefinitionKey());
        target.setProcessInstanceId(source.getProcessInstanceId()); target.setProcessDefinitionId(source.getProcessDefinitionId()); target.setCreateTime(source.getCreateTime());
        Map<String, Object> variables = taskService.getVariables(source.getId());
        target.setDocumentId(String.valueOf(variables.getOrDefault("documentId", ""))); target.setDocumentNumber(String.valueOf(variables.getOrDefault("documentNumber", "")));
        return target;
    }

    private WorkflowProcessDefinition toDefinition(ProcessDefinition source) {
        WorkflowProcessDefinition target = new WorkflowProcessDefinition();
        target.setId(source.getId()); target.setKey(source.getKey()); target.setName(source.getName()); target.setVersion(source.getVersion());
        target.setDeploymentId(source.getDeploymentId()); target.setResourceName(source.getResourceName()); target.setSuspended(source.isSuspended());
        return target;
    }

    private ProcessDefinition requireDefinition(String definitionId) {
        if (StringUtils.isEmpty(definitionId)) throw new ServiceException("流程定义不能为空");
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery().processDefinitionId(definitionId).singleResult();
        if (definition == null || !PURCHASE_ORDER_PROCESS_KEY.equals(definition.getKey())) throw new ServiceException("流程定义不存在");
        return definition;
    }

    private void requireProcessKey(String processKey) {
        if (!PURCHASE_ORDER_PROCESS_KEY.equals(processKey)) throw new ServiceException("当前仅支持采购订单流程：" + PURCHASE_ORDER_PROCESS_KEY);
    }

    private String readDefinitionXml(ProcessDefinition definition) {
        try (InputStream input = repositoryService.getResourceAsStream(definition.getDeploymentId(), definition.getResourceName())) {
            if (input == null) throw new ServiceException("流程定义文件不存在");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (ServiceException e) { throw e; } catch (Exception e) { throw new ServiceException("读取流程定义失败").setDetailMessage(String.valueOf(e.getMessage())); }
    }

    private String readClasspath(String path) {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) { throw new ServiceException("默认流程样板读取失败").setDetailMessage(String.valueOf(e.getMessage())); }
    }

    private WorkflowCandidateOption option(String type, String value, String label, String description) {
        WorkflowCandidateOption option = new WorkflowCandidateOption(); option.setType(type); option.setValue(value); option.setLabel(label); option.setDescription(description); return option;
    }

    private void addTasks(Map<String, Task> target, List<Task> source) { for (Task task : source) target.putIfAbsent(task.getId(), task); }
    private String stringVariable(HistoricProcessInstance instance, String key) { Object value = instance.getProcessVariables().get(key); return value == null ? "" : String.valueOf(value); }
    private Long parseLong(String value) { try { return Long.valueOf(value); } catch (Exception e) { return null; } }
    private Long toDocumentId(String businessKey) { Long value = parseLong(businessKey); if (value == null) throw new ServiceException("流程实例未绑定有效采购订单"); return value; }
}
