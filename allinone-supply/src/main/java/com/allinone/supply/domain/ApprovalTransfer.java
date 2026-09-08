package com.allinone.supply.domain;

import jakarta.validation.constraints.NotNull;

public class ApprovalTransfer {
    @NotNull private Long taskId;
    @NotNull private Long assigneeId;
    public Long getTaskId(){return taskId;} public void setTaskId(Long v){taskId=v;}
    public Long getAssigneeId(){return assigneeId;} public void setAssigneeId(Long v){assigneeId=v;}
}
