package com.allinone.supply.domain;

import java.util.Date;

/** 流程详情中的节点轨迹。 */
public class WorkflowTimelineItem {
    private String taskId;
    private String nodeId;
    private String nodeName;
    private String assigneeId;
    private String assigneeName;
    private String status;
    private String action;
    private String comment;
    private Date startTime;
    private Date endTime;

    public String getTaskId() { return taskId; }
    public void setTaskId(String value) { taskId = value; }
    public String getNodeId() { return nodeId; }
    public void setNodeId(String value) { nodeId = value; }
    public String getNodeName() { return nodeName; }
    public void setNodeName(String value) { nodeName = value; }
    public String getAssigneeId() { return assigneeId; }
    public void setAssigneeId(String value) { assigneeId = value; }
    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String value) { assigneeName = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getAction() { return action; }
    public void setAction(String value) { action = value; }
    public String getComment() { return comment; }
    public void setComment(String value) { comment = value; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date value) { startTime = value; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date value) { endTime = value; }
}
