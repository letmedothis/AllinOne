package com.allinone.supply.domain;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
public class WorkflowTaskTransfer {
    @NotBlank private String taskId;
    @NotNull private Long assigneeId;
    public String getTaskId(){return taskId;} public void setTaskId(String v){taskId=v;}
    public Long getAssigneeId(){return assigneeId;} public void setAssigneeId(Long v){assigneeId=v;}
}
