package com.allinone.supply.domain;

import java.util.Date;
import java.util.List;

/** 流程实例摘要和详情。 */
public class WorkflowInstanceDetail {
    private String id;
    private String processDefinitionId;
    private String processDefinitionName;
    private Integer processDefinitionVersion;
    private String documentId;
    private String documentNumber;
    private String startedBy;
    private String startedByName;
    private String status;
    private Date startTime;
    private Date endTime;
    private List<WorkflowTimelineItem> timeline;

    public String getId() { return id; }
    public void setId(String value) { id = value; }
    public String getProcessDefinitionId() { return processDefinitionId; }
    public void setProcessDefinitionId(String value) { processDefinitionId = value; }
    public String getProcessDefinitionName() { return processDefinitionName; }
    public void setProcessDefinitionName(String value) { processDefinitionName = value; }
    public Integer getProcessDefinitionVersion() { return processDefinitionVersion; }
    public void setProcessDefinitionVersion(Integer value) { processDefinitionVersion = value; }
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String value) { documentId = value; }
    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String value) { documentNumber = value; }
    public String getStartedBy() { return startedBy; }
    public void setStartedBy(String value) { startedBy = value; }
    public String getStartedByName() { return startedByName; }
    public void setStartedByName(String value) { startedByName = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date value) { startTime = value; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date value) { endTime = value; }
    public List<WorkflowTimelineItem> getTimeline() { return timeline; }
    public void setTimeline(List<WorkflowTimelineItem> value) { timeline = value; }
}
