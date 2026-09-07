package com.allinone.supply.domain;

import com.allinone.common.core.domain.BaseEntity;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class Receipt extends BaseEntity {
    private static final long serialVersionUID = 1L;
    private Long documentId;
    private String number;
    private Long orderId;
    private Long warehouseId;
    private Date businessDate;
    private Long confirmedBy;
    private Date confirmedAt;
    private String remark;
    private List<ReceiptLine> lines;
    public Long getDocumentId() { return documentId; } public void setDocumentId(Long v) { documentId = v; }
    public String getNumber() { return number; } public void setNumber(String v) { number = v; }
    public Long getOrderId() { return orderId; } public void setOrderId(Long v) { orderId = v; }
    public Long getWarehouseId() { return warehouseId; } public void setWarehouseId(Long v) { warehouseId = v; }
    public Date getBusinessDate() { return businessDate; } public void setBusinessDate(Date v) { businessDate = v; }
    public Long getConfirmedBy() { return confirmedBy; } public void setConfirmedBy(Long v) { confirmedBy = v; }
    public Date getConfirmedAt() { return confirmedAt; } public void setConfirmedAt(Date v) { confirmedAt = v; }
    public String getRemark() { return remark; } public void setRemark(String v) { remark = v; }
    public List<ReceiptLine> getLines() { return lines; } public void setLines(List<ReceiptLine> v) { lines = v; }
}
