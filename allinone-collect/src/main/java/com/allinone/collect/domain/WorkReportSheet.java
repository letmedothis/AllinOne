package com.allinone.collect.domain;

import com.allinone.common.annotation.Excel;
import com.allinone.common.core.domain.BaseEntity;

public class WorkReportSheet extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private String id;
    private String reportId;
    private Long sheetIndex;
    @Excel(name = "Sheet名称")
    private String sheetName;
    private String sheetData;
    private Long userId;
    private Long deptId;
    private Long delStatus;
    /** 乐观锁版本号：单元格保存时按客户端持有版本 CAS 递增，防止多用户并发互相覆盖 */
    private Long version;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public Long getSheetIndex() { return sheetIndex; }
    public void setSheetIndex(Long sheetIndex) { this.sheetIndex = sheetIndex; }
    public String getSheetName() { return sheetName; }
    public void setSheetName(String sheetName) { this.sheetName = sheetName; }
    public String getSheetData() { return sheetData; }
    public void setSheetData(String sheetData) { this.sheetData = sheetData; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public Long getDelStatus() { return delStatus; }
    public void setDelStatus(Long delStatus) { this.delStatus = delStatus; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
