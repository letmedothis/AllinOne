package com.allinone.supply.domain;

public class ApprovalDecision {
    private Long taskId;
    private Long documentId;
    private Integer taskRevision;
    private Integer documentRevision;
    private String comment;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long value) { taskId = value; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long value) { documentId = value; }
    public Integer getTaskRevision() { return taskRevision; }
    public void setTaskRevision(Integer value) { taskRevision = value; }
    public Integer getDocumentRevision() { return documentRevision; }
    public void setDocumentRevision(Integer value) { documentRevision = value; }
    public String getComment() { return comment; }
    public void setComment(String value) { comment = value; }
}
