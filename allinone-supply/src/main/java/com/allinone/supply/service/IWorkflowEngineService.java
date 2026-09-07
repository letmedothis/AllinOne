package com.allinone.supply.service;

import com.allinone.supply.domain.PurchaseOrder;
import com.allinone.supply.domain.WorkflowDeploymentRequest;
import com.allinone.supply.domain.WorkflowCandidateOption;
import com.allinone.supply.domain.WorkflowDesign;
import com.allinone.supply.domain.WorkflowInstanceDetail;
import com.allinone.supply.domain.WorkflowProcessDefinition;
import com.allinone.supply.domain.WorkflowTask;
import com.allinone.supply.domain.WorkflowTaskDecision;
import java.util.List;

public interface IWorkflowEngineService {
    void startPurchaseOrder(PurchaseOrder order, Long versionId, Long approverUserId);
    List<WorkflowProcessDefinition> selectProcessDefinitions();
    List<WorkflowTask> selectMyPurchaseOrderTasks();
    List<WorkflowCandidateOption> selectCandidateOptions();
    WorkflowDesign selectDesign(String processKey);
    WorkflowDesign saveDraft(WorkflowDesign design);
    WorkflowProcessDefinition publish(WorkflowDesign design);
    String selectDefinitionXml(String definitionId);
    int changeDefinitionState(String definitionId, boolean suspended);
    List<WorkflowInstanceDetail> selectMyStartedInstances();
    WorkflowInstanceDetail selectInstanceDetail(String instanceId);
    WorkflowProcessDefinition deploy(WorkflowDeploymentRequest request);
    int completePurchaseOrderTask(String taskId, WorkflowTaskDecision decision);
}
