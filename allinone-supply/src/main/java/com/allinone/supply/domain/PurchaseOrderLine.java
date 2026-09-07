package com.allinone.supply.domain;

import java.math.BigDecimal;

public class PurchaseOrderLine {
    private Long id;
    private Long orderId;
    private Integer lineNo;
    private String name;
    private String specification;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal rate;
    private Long amountCents;
    private Long taxCents;
    private Long totalCents;

    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long value) { orderId = value; }
    public Integer getLineNo() { return lineNo; }
    public void setLineNo(Integer value) { lineNo = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getSpecification() { return specification; }
    public void setSpecification(String value) { specification = value; }
    public String getUnit() { return unit; }
    public void setUnit(String value) { unit = value; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal value) { quantity = value; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal value) { price = value; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal value) { rate = value; }
    public Long getAmountCents() { return amountCents; }
    public void setAmountCents(Long value) { amountCents = value; }
    public Long getTaxCents() { return taxCents; }
    public void setTaxCents(Long value) { taxCents = value; }
    public Long getTotalCents() { return totalCents; }
    public void setTotalCents(Long value) { totalCents = value; }
}
