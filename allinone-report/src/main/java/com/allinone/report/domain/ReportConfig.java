package com.allinone.report.domain;

import com.allinone.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 报表配置表 report_config
 */
public class ReportConfig extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long reportId;
    private String reportName;
    private String reportCode;
    private String reportType;
    private String jimuReportId;
    private String jmbiId;
    private Long categoryId;
    private String icon;
    private Integer orderNum;
    private String status;

    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }
    public String getReportCode() { return reportCode; }
    public void setReportCode(String reportCode) { this.reportCode = reportCode; }
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public String getJimuReportId() { return jimuReportId; }
    public void setJimuReportId(String jimuReportId) { this.jimuReportId = jimuReportId; }
    public String getJmbiId() { return jmbiId; }
    public void setJmbiId(String jmbiId) { this.jmbiId = jmbiId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Integer getOrderNum() { return orderNum; }
    public void setOrderNum(Integer orderNum) { this.orderNum = orderNum; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() { return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
        .append("reportId", getReportId())
        .append("reportName", getReportName())
        .append("reportCode", getReportCode())
        .append("reportType", getReportType()).toString();
    }
}

