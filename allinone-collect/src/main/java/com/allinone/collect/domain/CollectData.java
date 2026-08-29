package com.allinone.collect.domain;

import com.allinone.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import java.util.Date;
import com.allinone.common.annotation.Excel;

/**
 * 填报数据表 collect_data
 */
public class CollectData extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long dataId;
    private Long templateId;
    @Excel(name = "模板名称")
    private String templateName;
    @Excel(name = "模板编码")
    private String templateCode;
    private String formData;
    @Excel(name = "填报状态")
    private String bizStatus;
    private Long deptId;
    private String flowInstanceId;
    @Excel(name = "业务编码")
    private String dataCode;
    private Integer version;
    @Excel(name = "提交人")
    private String submitBy;
    @Excel(name = "提交时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date submitTime;
    /** 导出备注列：仅用于导出汇总表展示（如“内容过大未导出”），不映射数据库字段 */
    @Excel(name = "备注")
    private String exportNote;

    public Long getDataId() { return dataId; }
    public void setDataId(Long dataId) { this.dataId = dataId; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
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
    public String getExportNote() { return exportNote; }
    public void setExportNote(String exportNote) { this.exportNote = exportNote; }

    @Override
    public String toString() { return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
        .append("dataId", getDataId())
        .append("templateId", getTemplateId())
        .append("bizStatus", getBizStatus())
        .append("submitBy", getSubmitBy()).toString();
    }
}

