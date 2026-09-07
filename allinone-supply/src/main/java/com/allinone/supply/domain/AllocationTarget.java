package com.allinone.supply.domain;
import java.math.BigDecimal;
public class AllocationTarget {
    private Long receiptLineId; private BigDecimal remainingQuantity; private String itemSnapshot;
    public Long getReceiptLineId(){return receiptLineId;} public void setReceiptLineId(Long v){receiptLineId=v;} public BigDecimal getRemainingQuantity(){return remainingQuantity;} public void setRemainingQuantity(BigDecimal v){remainingQuantity=v;} public String getItemSnapshot(){return itemSnapshot;} public void setItemSnapshot(String v){itemSnapshot=v;}
}
