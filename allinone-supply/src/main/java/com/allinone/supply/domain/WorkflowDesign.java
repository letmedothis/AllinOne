package com.allinone.supply.domain;

import java.util.Date;

/** 可编辑的流程草稿。ID 使用字符串传给浏览器，避免 bigint 精度丢失。 */
public class WorkflowDesign {
    private String id;
    private String processKey;
    private String name;
    private String bpmnXml;
    private String status;
    private Integer revision;
    private String publishedDefinitionId;
    private Integer publishedVersion;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
    private Date publishedAt;

    public String getId() { return id; }
    public void setId(String value) { id = value; }
    public String getProcessKey() { return processKey; }
    public void setProcessKey(String value) { processKey = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getBpmnXml() { return bpmnXml; }
    public void setBpmnXml(String value) { bpmnXml = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public Integer getRevision() { return revision; }
    public void setRevision(Integer value) { revision = value; }
    public String getPublishedDefinitionId() { return publishedDefinitionId; }
    public void setPublishedDefinitionId(String value) { publishedDefinitionId = value; }
    public Integer getPublishedVersion() { return publishedVersion; }
    public void setPublishedVersion(Integer value) { publishedVersion = value; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String value) { createBy = value; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date value) { createTime = value; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String value) { updateBy = value; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date value) { updateTime = value; }
    public Date getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Date value) { publishedAt = value; }
}
