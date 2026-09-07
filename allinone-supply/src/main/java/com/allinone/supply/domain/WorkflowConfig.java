package com.allinone.supply.domain;

/** 固定审批流候选人配置。候选人按逗号分隔的用户 ID 顺序保存。 */
public class WorkflowConfig {
    private Long id;
    private String flowType;
    private Integer version;
    private String supervisorCandidates;
    private String financeCandidates;
    private Long updatedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFlowType() { return flowType; }
    public void setFlowType(String flowType) { this.flowType = flowType; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getSupervisorCandidates() { return supervisorCandidates; }
    public void setSupervisorCandidates(String supervisorCandidates) { this.supervisorCandidates = supervisorCandidates; }
    public String getFinanceCandidates() { return financeCandidates; }
    public void setFinanceCandidates(String financeCandidates) { this.financeCandidates = financeCandidates; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
}
