package com.allinone.supply.domain;

import java.util.ArrayList;
import java.util.List;

/** 约定 DemoInvoice XML v1 的解析结果，解析结果仍需人工确认。 */
public class ParsedInvoice {
    private String invoiceNumber;
    private String invoiceType;
    private String issueDate;
    private String sellerName;
    private String sellerTaxId;
    private String buyerName;
    private String buyerTaxId;
    private String amount;
    private String taxAmount;
    private String totalAmount;
    private List<ParsedInvoiceLine> lines = new ArrayList<>();
    public String getInvoiceNumber() { return invoiceNumber; } public void setInvoiceNumber(String v) { invoiceNumber = v; }
    public String getInvoiceType() { return invoiceType; } public void setInvoiceType(String v) { invoiceType = v; }
    public String getIssueDate() { return issueDate; } public void setIssueDate(String v) { issueDate = v; }
    public String getSellerName() { return sellerName; } public void setSellerName(String v) { sellerName = v; }
    public String getSellerTaxId() { return sellerTaxId; } public void setSellerTaxId(String v) { sellerTaxId = v; }
    public String getBuyerName() { return buyerName; } public void setBuyerName(String v) { buyerName = v; }
    public String getBuyerTaxId() { return buyerTaxId; } public void setBuyerTaxId(String v) { buyerTaxId = v; }
    public String getAmount() { return amount; } public void setAmount(String v) { amount = v; }
    public String getTaxAmount() { return taxAmount; } public void setTaxAmount(String v) { taxAmount = v; }
    public String getTotalAmount() { return totalAmount; } public void setTotalAmount(String v) { totalAmount = v; }
    public List<ParsedInvoiceLine> getLines() { return lines; } public void setLines(List<ParsedInvoiceLine> v) { lines = v; }
}
