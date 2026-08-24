package com.allinone.collect.domain;

import java.util.Date;

public class WorkReportSheetPermission
{
    private Long id;
    private String sheetId;
    private String permType;
    private Long permId;
    private Long grantedBy;
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSheetId() { return sheetId; }
    public void setSheetId(String sheetId) { this.sheetId = sheetId; }
    public String getPermType() { return permType; }
    public void setPermType(String permType) { this.permType = permType; }
    public Long getPermId() { return permId; }
    public void setPermId(Long permId) { this.permId = permId; }
    public Long getGrantedBy() { return grantedBy; }
    public void setGrantedBy(Long grantedBy) { this.grantedBy = grantedBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
