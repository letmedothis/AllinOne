package com.allinone.supply.domain;

import java.util.Date;

/** 审批中心待办摘要。 */
public class ApprovalTask {
    private Long taskId;
    private Long instanceId;
    private Long documentId;
    private Long versionId;
    private Long submitterId;
    private String documentType;
    private String documentNumber;
    private String node;
    private Long assigneeId;
    private String submitterName;
    private Integer versionNo;
    private String status;
    private Integer revision;
    private Integer documentRevision;
    private Date submittedAt;
    private String comment;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long value) { taskId = value; }
    public Long getInstanceId() { return instanceId; }
    public void setInstanceId(Long value) { instanceId = value; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long value) { documentId = value; }
    public Long getVersionId() { return versionId; }
    public void setVersionId(Long value) { versionId = value; }
    public Long getSubmitterId() { return submitterId; }
    public void setSubmitterId(Long value) { submitterId = value; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String value) { documentType = value; }
    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String value) { documentNumber = value; }
    public String getNode() { return node; }
    public void setNode(String value) { node = value; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long value) { assigneeId = value; }
    public String getSubmitterName() { return submitterName; }
    public void setSubmitterName(String value) { submitterName = value; }
    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer value) { versionNo = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public Integer getRevision() { return revision; }
    public void setRevision(Integer value) { revision = value; }
    public Integer getDocumentRevision() { return documentRevision; }
    public void setDocumentRevision(Integer value) { documentRevision = value; }
    public Date getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Date value) { submittedAt = value; }
    public String getComment() { return comment; }
    public void setComment(String value) { comment = value; }
}
