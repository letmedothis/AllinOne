package com.allinone.supply.domain;

import java.math.BigDecimal;

public class ReceiptLine {
    private Long id;
    private Long orderLineId;
    private String itemSnapshot;
    private BigDecimal orderQuantity;
    private BigDecimal receivedQuantity;
    private BigDecimal remainingQuantity;
    private BigDecimal quantity;
    public Long getId() { return id; } public void setId(Long v) { id = v; }
    public Long getOrderLineId() { return orderLineId; } public void setOrderLineId(Long v) { orderLineId = v; }
    public String getItemSnapshot() { return itemSnapshot; } public void setItemSnapshot(String v) { itemSnapshot = v; }
    public BigDecimal getOrderQuantity() { return orderQuantity; } public void setOrderQuantity(BigDecimal v) { orderQuantity = v; }
    public BigDecimal getReceivedQuantity() { return receivedQuantity; } public void setReceivedQuantity(BigDecimal v) { receivedQuantity = v; }
    public BigDecimal getRemainingQuantity() { return remainingQuantity; } public void setRemainingQuantity(BigDecimal v) { remainingQuantity = v; }
    public BigDecimal getQuantity() { return quantity; } public void setQuantity(BigDecimal v) { quantity = v; }
}
