package com.allinone.supply.domain;

/** Flowable 中已部署的流程定义摘要，供流程设计页展示。 */
public class WorkflowProcessDefinition {
    private String id;
    private String key;
    private String name;
    private Integer version;
    private String deploymentId;
    private String resourceName;
    private Boolean suspended;

    public String getId() { return id; }
    public void setId(String value) { id = value; }
    public String getKey() { return key; }
    public void setKey(String value) { key = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer value) { version = value; }
    public String getDeploymentId() { return deploymentId; }
    public void setDeploymentId(String value) { deploymentId = value; }
    public String getResourceName() { return resourceName; }
    public void setResourceName(String value) { resourceName = value; }
    public Boolean getSuspended() { return suspended; }
    public void setSuspended(Boolean value) { suspended = value; }
}
