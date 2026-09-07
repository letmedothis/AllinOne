package com.allinone.supply.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.allinone.supply.domain.PurchaseOrder;
import com.allinone.supply.mapper.WorkflowEngineMapper;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowEngineServiceImplTest {
    @Mock private RepositoryService repositoryService;
    @Mock private RuntimeService runtimeService;
    @Mock private IdentityService identityService;
    @Mock private WorkflowEngineMapper workflowEngineMapper;
    @InjectMocks private WorkflowEngineServiceImpl service;

    @Test
    void startsLatestPurchaseOrderDefinitionWithStringIds() {
        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);
        ProcessDefinition definition = mock(ProcessDefinition.class);
        ProcessInstance instance = mock(ProcessInstance.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
        when(query.processDefinitionKey(WorkflowEngineServiceImpl.PURCHASE_ORDER_PROCESS_KEY)).thenReturn(query);
        when(query.latestVersion()).thenReturn(query);
        when(query.active()).thenReturn(query);
        when(query.singleResult()).thenReturn(definition);
        when(definition.getId()).thenReturn("purchase-order-approval:3:9001");
        when(definition.getDeploymentId()).thenReturn("deployment-3");
        when(definition.getResourceName()).thenReturn("processes/purchase-order-approval.bpmn20.xml");
        String bpmn = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:flowable=\"http://flowable.org/bpmn\" xmlns:allinone=\"https://allinone.local/workflow\"><process id=\"purchase-order-approval\" name=\"采购订单审批\"><userTask id=\"approve\" name=\"主管审批\" flowable:assignee=\"${approverUserId}\" allinone:assigneeType=\"SUPERVISOR_CONFIG\" allinone:assigneeValue=\"\"/></process></definitions>";
        when(repositoryService.getResourceAsStream("deployment-3", "processes/purchase-order-approval.bpmn20.xml"))
                .thenReturn(new ByteArrayInputStream(bpmn.getBytes(StandardCharsets.UTF_8)));
        when(runtimeService.startProcessInstanceById(eq("purchase-order-approval:3:9001"), eq("922337203685"), anyMap())).thenReturn(instance);

        PurchaseOrder order = new PurchaseOrder();
        order.setDocumentId(922337203685L);
        order.setNumber("PO-20260907-00001");
        order.setCreatorId(101L);
        service.startPurchaseOrder(order, 922337203686L, 922337203687L);

        verify(runtimeService).startProcessInstanceById(eq("purchase-order-approval:3:9001"), eq("922337203685"),
                org.mockito.ArgumentMatchers.argThat(variables -> variables != null
                        && "922337203685".equals(variables.get("documentId"))
                        && "922337203686".equals(variables.get("documentVersionId"))
                        && "101".equals(variables.get("startedBy"))
                        && "922337203687".equals(variables.get("approverUserId"))));
    }
}
