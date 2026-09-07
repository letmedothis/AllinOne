package com.allinone.supply.domain;

/** 应付余额行(按发票)。 */
public class ApBalanceRow {
    private Long invoiceId;
    private String invoiceNumber;
    private String issueDate;
    private Long totalCents;
    private Long allocatedCents;
    private Long balanceCents;

    public Long getInvoiceId() { return invoiceId; } public void setInvoiceId(Long v) { invoiceId = v; }
    public String getInvoiceNumber() { return invoiceNumber; } public void setInvoiceNumber(String v) { invoiceNumber = v; }
    public String getIssueDate() { return issueDate; } public void setIssueDate(String v) { issueDate = v; }
    public Long getTotalCents() { return totalCents; } public void setTotalCents(Long v) { totalCents = v; }
    public Long getAllocatedCents() { return allocatedCents; } public void setAllocatedCents(Long v) { allocatedCents = v; }
    public Long getBalanceCents() { return balanceCents; } public void setBalanceCents(Long v) { balanceCents = v; }
}
