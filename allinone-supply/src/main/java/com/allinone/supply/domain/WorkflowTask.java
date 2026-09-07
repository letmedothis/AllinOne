package com.allinone.supply.domain;

import java.util.Date;

/** 当前登录用户在 Flowable 中的采购订单待办。所有 ID 均以字符串返回，避免前端精度丢失。 */
public class WorkflowTask {
    private String id;
    private String name;
    private String taskDefinitionKey;
    private String processInstanceId;
    private String processDefinitionId;
    private String documentId;
    private String documentNumber;
    private Date createTime;

    public String getId() { return id; }
    public void setId(String value) { id = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getTaskDefinitionKey() { return taskDefinitionKey; }
    public void setTaskDefinitionKey(String value) { taskDefinitionKey = value; }
    public String getProcessInstanceId() { return processInstanceId; }
    public void setProcessInstanceId(String value) { processInstanceId = value; }
    public String getProcessDefinitionId() { return processDefinitionId; }
    public void setProcessDefinitionId(String value) { processDefinitionId = value; }
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String value) { documentId = value; }
    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String value) { documentNumber = value; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date value) { createTime = value; }
}
