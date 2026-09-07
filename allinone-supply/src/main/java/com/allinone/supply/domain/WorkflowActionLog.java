package com.allinone.supply.domain;

import java.util.Date;

/** Flowable 人工任务的不可变审批记录。 */
public class WorkflowActionLog {
    private String id;
    private String processInstanceId;
    private String taskId;
    private String taskDefinitionKey;
    private String action;
    private String comment;
    private String actorId;
    private String actorName;
    private Date createdAt;

    public String getId() { return id; }
    public void setId(String value) { id = value; }
    public String getProcessInstanceId() { return processInstanceId; }
    public void setProcessInstanceId(String value) { processInstanceId = value; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String value) { taskId = value; }
    public String getTaskDefinitionKey() { return taskDefinitionKey; }
    public void setTaskDefinitionKey(String value) { taskDefinitionKey = value; }
    public String getAction() { return action; }
    public void setAction(String value) { action = value; }
    public String getComment() { return comment; }
    public void setComment(String value) { comment = value; }
    public String getActorId() { return actorId; }
    public void setActorId(String value) { actorId = value; }
    public String getActorName() { return actorName; }
    public void setActorName(String value) { actorName = value; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date value) { createdAt = value; }
}
