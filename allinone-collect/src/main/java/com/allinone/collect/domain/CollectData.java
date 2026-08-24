package com.allinone.collect.domain;

import com.allinone.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import java.util.Date;

/**
 * 填报数据表 collect_data
 */
public class CollectData extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long dataId;
    private Long templateId;
    private String formData;
    private String bizStatus;
    private Long deptId;
    private String flowInstanceId;
    private String dataCode;
    private Integer version;
    private String submitBy;
    private Date submitTime;

    public Long getDataId() { return dataId; }
    public void setDataId(Long dataId) { this.dataId = dataId; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getFormData() { return formData; }
    public void setFormData(String formData) { this.formData = formData; }
    public String getBizStatus() { return bizStatus; }
    public void setBizStatus(String bizStatus) { this.bizStatus = bizStatus; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getFlowInstanceId() { return flowInstanceId; }
    public void setFlowInstanceId(String flowInstanceId) { this.flowInstanceId = flowInstanceId; }
    public String getDataCode() { return dataCode; }
    public void setDataCode(String dataCode) { this.dataCode = dataCode; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getSubmitBy() { return submitBy; }
    public void setSubmitBy(String submitBy) { this.submitBy = submitBy; }
    public Date getSubmitTime() { return submitTime; }
    public void setSubmitTime(Date submitTime) { this.submitTime = submitTime; }

    @Override
    public String toString() { return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
        .append("dataId", getDataId())
        .append("templateId", getTemplateId())
        .append("bizStatus", getBizStatus())
        .append("submitBy", getSubmitBy()).toString();
    }
}

