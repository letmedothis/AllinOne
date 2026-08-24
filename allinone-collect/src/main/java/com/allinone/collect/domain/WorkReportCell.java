package com.allinone.collect.domain;

import java.util.Date;

public class WorkReportCell
{
    private Long id;
    private String sheetId;
    private Integer rowIndex;
    private Integer colIndex;
    private String cellValue;
    private String cellFormula;
    private String cellType;
    private String cellStyle;
    private Date createTime;
    private Date updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSheetId() { return sheetId; }
    public void setSheetId(String sheetId) { this.sheetId = sheetId; }
    public Integer getRowIndex() { return rowIndex; }
    public void setRowIndex(Integer rowIndex) { this.rowIndex = rowIndex; }
    public Integer getColIndex() { return colIndex; }
    public void setColIndex(Integer colIndex) { this.colIndex = colIndex; }
    public String getCellValue() { return cellValue; }
    public void setCellValue(String cellValue) { this.cellValue = cellValue; }
    public String getCellFormula() { return cellFormula; }
    public void setCellFormula(String cellFormula) { this.cellFormula = cellFormula; }
    public String getCellType() { return cellType; }
    public void setCellType(String cellType) { this.cellType = cellType; }
    public String getCellStyle() { return cellStyle; }
    public void setCellStyle(String cellStyle) { this.cellStyle = cellStyle; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
