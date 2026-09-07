package com.allinone.supply.domain;

public class ParsedInvoiceLine {
    private String name; private String specification; private String unit; private String quantity; private String unitPrice; private String taxRate; private String amount; private String taxAmount;
    public String getName() { return name; } public void setName(String v) { name = v; }
    public String getSpecification() { return specification; } public void setSpecification(String v) { specification = v; }
    public String getUnit() { return unit; } public void setUnit(String v) { unit = v; }
    public String getQuantity() { return quantity; } public void setQuantity(String v) { quantity = v; }
    public String getUnitPrice() { return unitPrice; } public void setUnitPrice(String v) { unitPrice = v; }
    public String getTaxRate() { return taxRate; } public void setTaxRate(String v) { taxRate = v; }
    public String getAmount() { return amount; } public void setAmount(String v) { amount = v; }
    public String getTaxAmount() { return taxAmount; } public void setTaxAmount(String v) { taxAmount = v; }
}
