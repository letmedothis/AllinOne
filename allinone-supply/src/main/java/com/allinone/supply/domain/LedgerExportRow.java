package com.allinone.supply.domain;

import com.allinone.common.annotation.Excel;
import java.math.BigDecimal;

public class LedgerExportRow {
    @Excel(name = "订单号") private String orderNumber;
    @Excel(name = "供应商") private String supplierName;
    @Excel(name = "订单金额（元）") private BigDecimal total;
    @Excel(name = "已入库数量") private Long receivedQuantity;
    @Excel(name = "已通过发票金额（元）") private BigDecimal approvedInvoice;
    @Excel(name = "待审发票金额（元）") private BigDecimal pendingInvoice;
    @Excel(name = "订单状态") private String approvalStatus;
    public LedgerExportRow() { }
    public LedgerExportRow(LedgerRow row) { orderNumber=row.getOrderNumber(); supplierName=row.getSupplierName(); total=cents(row.getTotalCents()); receivedQuantity=row.getReceivedQuantity(); approvedInvoice=cents(row.getApprovedInvoiceCents()); pendingInvoice=cents(row.getPendingInvoiceCents()); approvalStatus=row.getApprovalStatus(); }
    private BigDecimal cents(Long value) { return BigDecimal.valueOf(value == null ? 0 : value).movePointLeft(2); }
    public String getOrderNumber(){return orderNumber;} public void setOrderNumber(String v){orderNumber=v;} public String getSupplierName(){return supplierName;} public void setSupplierName(String v){supplierName=v;} public BigDecimal getTotal(){return total;} public void setTotal(BigDecimal v){total=v;} public Long getReceivedQuantity(){return receivedQuantity;} public void setReceivedQuantity(Long v){receivedQuantity=v;} public BigDecimal getApprovedInvoice(){return approvedInvoice;} public void setApprovedInvoice(BigDecimal v){approvedInvoice=v;} public BigDecimal getPendingInvoice(){return pendingInvoice;} public void setPendingInvoice(BigDecimal v){pendingInvoice=v;} public String getApprovalStatus(){return approvalStatus;} public void setApprovalStatus(String v){approvalStatus=v;}
}
