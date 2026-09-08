package com.allinone.supply.service.impl;

import com.allinone.common.utils.SecurityUtils;
import com.allinone.supply.mapper.WorkflowEngineMapper;
import com.allinone.supply.support.TaskTransferAuthorization;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.flowable.engine.*;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkflowTransferTest {
    private final TaskService tasks = mock(TaskService.class, RETURNS_DEEP_STUBS);
    private final RuntimeService runtime = mock(RuntimeService.class, RETURNS_DEEP_STUBS);
    private final RepositoryService repository = mock(RepositoryService.class, RETURNS_DEEP_STUBS);
    private final WorkflowEngineMapper mapper = mock(WorkflowEngineMapper.class);
    private final TaskTransferAuthorization auth = mock(TaskTransferAuthorization.class);
    private final WorkflowEngineServiceImpl service = new WorkflowEngineServiceImpl();

    private void setup(String type, String value, String assignment) {
        ReflectionTestUtils.setField(service,"taskService",tasks);
        ReflectionTestUtils.setField(service,"runtimeService",runtime);
        ReflectionTestUtils.setField(service,"repositoryService",repository);
        ReflectionTestUtils.setField(service,"workflowEngineMapper",mapper);
        ReflectionTestUtils.setField(service,"transferAuthorization",auth);
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("task"); when(task.getProcessInstanceId()).thenReturn("instance");
        when(task.getProcessDefinitionId()).thenReturn("definition"); when(task.getTaskDefinitionKey()).thenReturn("approve");
        when(task.getAssignee()).thenReturn("20");
        var taskQuery = mock(org.flowable.task.api.TaskQuery.class, RETURNS_SELF);
        when(tasks.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(task);
        ProcessInstance instance = mock(ProcessInstance.class); when(instance.getBusinessKey()).thenReturn("100");
        var instanceQuery = mock(org.flowable.engine.runtime.ProcessInstanceQuery.class, RETURNS_SELF);
        when(runtime.createProcessInstanceQuery()).thenReturn(instanceQuery);
        when(instanceQuery.singleResult()).thenReturn(instance);
        when(mapper.selectDocumentWorkflowEngine(100L)).thenReturn("FLOWABLE");
        when(mapper.selectDocumentApprovalStatus(100L)).thenReturn("IN_REVIEW");
        ProcessDefinition definition = mock(ProcessDefinition.class);
        when(definition.getKey()).thenReturn("purchase-order-approval");
        when(definition.getDeploymentId()).thenReturn("deployment"); when(definition.getResourceName()).thenReturn("xml");
        var definitionQuery = mock(org.flowable.engine.repository.ProcessDefinitionQuery.class, RETURNS_SELF);
        when(repository.createProcessDefinitionQuery()).thenReturn(definitionQuery);
        when(definitionQuery.singleResult()).thenReturn(definition);
        String xml = "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:flowable=\"http://flowable.org/bpmn\" xmlns:allinone=\"https://allinone.local/workflow\"><process id=\"purchase-order-approval\"><userTask id=\"approve\" name=\"审核\" " + assignment + " allinone:assigneeType=\"" + type + "\" allinone:assigneeValue=\"" + value + "\"/></process></definitions>";
        xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + xml;
        when(repository.getResourceAsStream("deployment","xml")).thenReturn(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    @Test void rejectsRecipientOutsidePublishedRole() {
        setup("ROLE","finance","flowable:candidateGroups=\"finance\"");
        assertThatThrownBy(() -> service.transferPurchaseOrderTask("task",30L)).hasMessageContaining("节点资格");
        verify(tasks,never()).setAssignee(anyString(),anyString());
    }

    @Test void fixedUserNodeCannotBeTransferredToArbitraryUser() {
        setup("USER","20","flowable:assignee=\"20\"");
        assertThatThrownBy(() -> service.transferPurchaseOrderTask("task",30L)).hasMessageContaining("固定人员");
        verify(tasks,never()).setAssignee(anyString(),anyString());
    }

    @Test void qualifiedRoleRecipientIsAssignedAndAudited() {
        setup("ROLE","finance","flowable:candidateGroups=\"finance\"");
        when(auth.hasRole(30L,"finance")).thenReturn(true);
        when(mapper.insertActionLog(any())).thenReturn(1);
        try (var security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getUserId).thenReturn(40L);
            security.when(SecurityUtils::getUsername).thenReturn("manager");
            assertThat(service.transferPurchaseOrderTask("task",30L)).isEqualTo(1);
        }
        verify(auth).requireOperator(100L,"supply:workflow:transfer");
        verify(auth).requireRecipient(100L,30L,"supply:workflow:task");
        verify(tasks).setAssignee("task","30");
        verify(mapper).insertActionLog(argThat(log -> "TRANSFER".equals(log.getAction()) && "40".equals(log.getActorId()) && log.getComment().contains("20") && log.getComment().contains("30")));
    }
}
