package com.allinone.collect.domain;

import com.allinone.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.allinone.common.annotation.Excel;

/**
 * 填报模板表 collect_template
 */
public class CollectTemplate extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long templateId;
    @Excel(name = "模板名称")
    private String templateName;
    @Excel(name = "模板编码")
    private String templateCode;
    private Long categoryId;
    @Excel(name = "分类名称")
    private String categoryName;
    private String templateType;
    private String templateJson;
    @Excel(name = "状态")
    private String status;
    private Integer version;

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getTemplateType() { return templateType; }
    public void setTemplateType(String templateType) { this.templateType = templateType; }
    public String getTemplateJson() { return templateJson; }
    public void setTemplateJson(String templateJson) { this.templateJson = templateJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    @Override
    public String toString() { return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
        .append("templateId", getTemplateId())
        .append("templateName", getTemplateName())
        .append("templateCode", getTemplateCode())
        .append("status", getStatus())
        .append("version", getVersion()).toString();
    }
}

