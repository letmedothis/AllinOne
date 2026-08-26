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

    /** 分类名称（列表展示用，联表查询） */
    private String categoryName;

    /** 访问URL（非表字段，服务层按类型计算：报表 /jmreport/view/{id}，大屏/仪表盘 /jimubi/view?pageId={id}） */
    private String url;

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
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    @Override
    public String toString() { return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
        .append("reportId", getReportId())
        .append("reportName", getReportName())
        .append("reportCode", getReportCode())
        .append("reportType", getReportType()).toString();
    }
}

