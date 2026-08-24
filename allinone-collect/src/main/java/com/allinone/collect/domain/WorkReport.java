package com.allinone.collect.domain;

import com.allinone.common.annotation.Excel;
import com.allinone.common.core.domain.BaseEntity;

public class WorkReport extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private String id;
    @Excel(name = "报表名")
    private String reportName;
    @Excel(name = "报表简介")
    private String reportJianjie;
    @Excel(name = "备注")
    private String reportBeizhu;
    private Long userId;
    private Long deptId;
    private Long delStatus;
    private String sheetData;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }
    public String getReportJianjie() { return reportJianjie; }
    public void setReportJianjie(String reportJianjie) { this.reportJianjie = reportJianjie; }
    public String getReportBeizhu() { return reportBeizhu; }
    public void setReportBeizhu(String reportBeizhu) { this.reportBeizhu = reportBeizhu; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public Long getDelStatus() { return delStatus; }
    public void setDelStatus(Long delStatus) { this.delStatus = delStatus; }
    public String getSheetData() { return sheetData; }
    public void setSheetData(String sheetData) { this.sheetData = sheetData; }
}
