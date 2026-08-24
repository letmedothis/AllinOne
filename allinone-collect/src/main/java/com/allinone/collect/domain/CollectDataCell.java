package com.allinone.collect.domain;

import com.allinone.common.core.domain.BaseEntity;
import java.math.BigDecimal;

/**
 * 填报单元格数据表 collect_data_cell（三层架构 Tier 2）
 */
public class CollectDataCell extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long cellId;
    private Long dataId;
    private Long templateId;
    private Integer sheetIndex;
    private Integer rowIndex;
    private Integer colIndex;
    private String cellText;
    private String cellValue;
    private BigDecimal cellNumericValue;
    private String cellType;
    private String cellFormat;
    private String isFormula;
    private String formulaExpr;

    public Long getCellId() { return cellId; }
    public void setCellId(Long cellId) { this.cellId = cellId; }
    public Long getDataId() { return dataId; }
    public void setDataId(Long dataId) { this.dataId = dataId; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public Integer getSheetIndex() { return sheetIndex; }
    public void setSheetIndex(Integer sheetIndex) { this.sheetIndex = sheetIndex; }
    public Integer getRowIndex() { return rowIndex; }
    public void setRowIndex(Integer rowIndex) { this.rowIndex = rowIndex; }
    public Integer getColIndex() { return colIndex; }
    public void setColIndex(Integer colIndex) { this.colIndex = colIndex; }
    public String getCellText() { return cellText; }
    public void setCellText(String cellText) { this.cellText = cellText; }
    public String getCellValue() { return cellValue; }
    public void setCellValue(String cellValue) { this.cellValue = cellValue; }
    public BigDecimal getCellNumericValue() { return cellNumericValue; }
    public void setCellNumericValue(BigDecimal cellNumericValue) { this.cellNumericValue = cellNumericValue; }
    public String getCellType() { return cellType; }
    public void setCellType(String cellType) { this.cellType = cellType; }
    public String getCellFormat() { return cellFormat; }
    public void setCellFormat(String cellFormat) { this.cellFormat = cellFormat; }
    public String getIsFormula() { return isFormula; }
    public void setIsFormula(String isFormula) { this.isFormula = isFormula; }
    public String getFormulaExpr() { return formulaExpr; }
    public void setFormulaExpr(String formulaExpr) { this.formulaExpr = formulaExpr; }
}

