package com.allinone.supply.domain;

/** 管理员从 BPMN 设计器发布流程定义的请求。 */
public class WorkflowDeploymentRequest {
    private String processKey;
    private String deploymentName;
    private String bpmnXml;

    public String getProcessKey() { return processKey; }
    public void setProcessKey(String value) { processKey = value; }
    public String getDeploymentName() { return deploymentName; }
    public void setDeploymentName(String value) { deploymentName = value; }
    public String getBpmnXml() { return bpmnXml; }
    public void setBpmnXml(String value) { bpmnXml = value; }
}
