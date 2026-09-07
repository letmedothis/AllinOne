package com.allinone.supply.domain;

/** 付款单核销明细:一行 = 对一张已通过发票的核销金额。 */
public class PaymentLine {
    private Long id;
    private Long paymentId;
    private Long invoiceId;
    private Long allocatedCents;

    public Long getId() { return id; } public void setId(Long v) { id = v; }
    public Long getPaymentId() { return paymentId; } public void setPaymentId(Long v) { paymentId = v; }
    public Long getInvoiceId() { return invoiceId; } public void setInvoiceId(Long v) { invoiceId = v; }
    public Long getAllocatedCents() { return allocatedCents; } public void setAllocatedCents(Long v) { allocatedCents = v; }
}
