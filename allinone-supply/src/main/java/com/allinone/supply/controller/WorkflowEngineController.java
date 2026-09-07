package com.allinone.supply.controller;

import com.allinone.common.annotation.Log;
import com.allinone.common.annotation.RepeatSubmit;
import com.allinone.common.core.controller.BaseController;
import com.allinone.common.core.domain.AjaxResult;
import com.allinone.common.enums.BusinessType;
import com.allinone.supply.domain.WorkflowDeploymentRequest;
import com.allinone.supply.domain.WorkflowDefinitionStateRequest;
import com.allinone.supply.domain.WorkflowDesign;
import com.allinone.supply.domain.WorkflowTaskDecision;
import com.allinone.supply.service.IWorkflowEngineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Flowable 流程设计与待办入口；当前范围为采购订单审批。 */
@RestController
@RequestMapping("/supply/workflow")
public class WorkflowEngineController extends BaseController {
    @Autowired private IWorkflowEngineService workflowEngineService;

    @PreAuthorize("@ss.hasPermi('supply:workflow:list')")
    @GetMapping("/definitions")
    public AjaxResult definitions() { return success(workflowEngineService.selectProcessDefinitions()); }

    @PreAuthorize("@ss.hasPermi('supply:workflow:list')")
    @GetMapping("/definitions/{definitionId}/xml")
    public AjaxResult definitionXml(@PathVariable String definitionId) { return success(workflowEngineService.selectDefinitionXml(definitionId)); }

    @PreAuthorize("@ss.hasPermi('supply:workflow:deploy')")
    @Log(title = "流程定义状态", businessType = BusinessType.UPDATE)
    @PutMapping("/definitions/{definitionId}/state")
    public AjaxResult definitionState(@PathVariable String definitionId, @RequestBody WorkflowDefinitionStateRequest request) {
        if (request == null || request.getSuspended() == null) return error("流程状态不能为空");
        return toAjax(workflowEngineService.changeDefinitionState(definitionId, request.getSuspended()));
    }

    @PreAuthorize("@ss.hasPermi('supply:workflow:list')")
    @GetMapping("/candidates")
    public AjaxResult candidates() { return success(workflowEngineService.selectCandidateOptions()); }

    @PreAuthorize("@ss.hasPermi('supply:workflow:list')")
    @GetMapping("/designs/{processKey}")
    public AjaxResult design(@PathVariable String processKey) { return success(workflowEngineService.selectDesign(processKey)); }

    @PreAuthorize("@ss.hasPermi('supply:workflow:deploy')")
    @Log(title = "流程草稿", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/designs/save")
    public AjaxResult saveDraft(@RequestBody WorkflowDesign design) { return success(workflowEngineService.saveDraft(design)); }

    @PreAuthorize("@ss.hasPermi('supply:workflow:deploy')")
    @Log(title = "流程定义", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping("/designs/publish")
    public AjaxResult publish(@RequestBody WorkflowDesign design) { return success(workflowEngineService.publish(design)); }

    @PreAuthorize("@ss.hasPermi('supply:workflow:deploy')")
    @Log(title = "流程定义", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping("/definitions/deploy")
    public AjaxResult deploy(@RequestBody WorkflowDeploymentRequest request) { return success(workflowEngineService.deploy(request)); }

    @PreAuthorize("@ss.hasPermi('supply:workflow:task')")
    @GetMapping("/tasks/mine")
    public AjaxResult myTasks() { return success(workflowEngineService.selectMyPurchaseOrderTasks()); }

    @PreAuthorize("@ss.hasPermi('supply:workflow:task')")
    @GetMapping("/instances/mine")
    public AjaxResult myStartedInstances() { return success(workflowEngineService.selectMyStartedInstances()); }

    @PreAuthorize("@ss.hasPermi('supply:workflow:task')")
    @GetMapping("/instances/{instanceId}")
    public AjaxResult instanceDetail(@PathVariable String instanceId) { return success(workflowEngineService.selectInstanceDetail(instanceId)); }

    @PreAuthorize("@ss.hasPermi('supply:workflow:task')")
    @Log(title = "流程待办", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/tasks/{taskId}/complete")
    public AjaxResult complete(@PathVariable String taskId, @RequestBody WorkflowTaskDecision decision) {
        return toAjax(workflowEngineService.completePurchaseOrderTask(taskId, decision));
    }
}
