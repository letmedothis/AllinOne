package com.allinone.supply.domain;

/** 流程节点审批人可选项。 */
public class WorkflowCandidateOption {
    private String type;
    private String value;
    private String label;
    private String description;

    public String getType() { return type; }
    public void setType(String value) { type = value; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getLabel() { return label; }
    public void setLabel(String value) { label = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
}
