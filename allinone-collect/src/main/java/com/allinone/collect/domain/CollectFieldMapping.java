package com.allinone.collect.domain;

import com.allinone.common.core.domain.BaseEntity;

/**
 * 字段映射配置表 collect_field_mapping（三层架构 Tier 3）
 */
public class CollectFieldMapping extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long mappingId;
    private Long templateId;
    private String cellRef;
    private Integer sheetIndex;
    private Integer rowIndex;
    private Integer colIndex;
    private String targetTable;
    private String targetColumn;
    private String dataType;
    private Integer pkOrder;
    private String defaultValue;
    private String transformType;
    private String transformScript;
    private Integer orderNum;

    public Long getMappingId() { return mappingId; }
    public void setMappingId(Long mappingId) { this.mappingId = mappingId; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getCellRef() { return cellRef; }
    public void setCellRef(String cellRef) { this.cellRef = cellRef; }
    public Integer getSheetIndex() { return sheetIndex; }
    public void setSheetIndex(Integer sheetIndex) { this.sheetIndex = sheetIndex; }
    public Integer getRowIndex() { return rowIndex; }
    public void setRowIndex(Integer rowIndex) { this.rowIndex = rowIndex; }
    public Integer getColIndex() { return colIndex; }
    public void setColIndex(Integer colIndex) { this.colIndex = colIndex; }
    public String getTargetTable() { return targetTable; }
    public void setTargetTable(String targetTable) { this.targetTable = targetTable; }
    public String getTargetColumn() { return targetColumn; }
    public void setTargetColumn(String targetColumn) { this.targetColumn = targetColumn; }
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    public Integer getPkOrder() { return pkOrder; }
    public void setPkOrder(Integer pkOrder) { this.pkOrder = pkOrder; }
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    public String getTransformType() { return transformType; }
    public void setTransformType(String transformType) { this.transformType = transformType; }
    public String getTransformScript() { return transformScript; }
    public void setTransformScript(String transformScript) { this.transformScript = transformScript; }
    public Integer getOrderNum() { return orderNum; }
    public void setOrderNum(Integer orderNum) { this.orderNum = orderNum; }
}

