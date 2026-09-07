package com.allinone.supply.domain;

import com.allinone.common.core.domain.BaseEntity;
import java.math.BigDecimal;
import java.util.List;

/** 采购订单及其当前明细。金额统一由服务端按分计算。 */
public class PurchaseOrder extends BaseEntity {
    private static final long serialVersionUID = 1L;
    private Long documentId;
    private String number;
    private Long creatorId;
    private String creationKey;
    private String approvalStatus;
    private String currentNode;
    private Integer currentVersion;
    private Integer revision;
    private Long supplierId;
    private String supplierName;
    private Long buyerId;
    private java.util.Date expectedDate;
    private String currency;
    private Long amountCents;
    private Long taxCents;
    private Long totalCents;
    private List<PurchaseOrderLine> lines;

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long value) { documentId = value; }
    public String getNumber() { return number; }
    public void setNumber(String value) { number = value; }
    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long value) { creatorId = value; }
    public String getCreationKey() { return creationKey; }
    public void setCreationKey(String value) { creationKey = value; }
    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String value) { approvalStatus = value; }
    public String getCurrentNode() { return currentNode; }
    public void setCurrentNode(String value) { currentNode = value; }
    public Integer getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(Integer value) { currentVersion = value; }
    public Integer getRevision() { return revision; }
    public void setRevision(Integer value) { revision = value; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long value) { supplierId = value; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String value) { supplierName = value; }
    public Long getBuyerId() { return buyerId; }
    public void setBuyerId(Long value) { buyerId = value; }
    public java.util.Date getExpectedDate() { return expectedDate; }
    public void setExpectedDate(java.util.Date value) { expectedDate = value; }
    public String getCurrency() { return currency; }
    public void setCurrency(String value) { currency = value; }
    public Long getAmountCents() { return amountCents; }
    public void setAmountCents(Long value) { amountCents = value; }
    public Long getTaxCents() { return taxCents; }
    public void setTaxCents(Long value) { taxCents = value; }
    public Long getTotalCents() { return totalCents; }
    public void setTotalCents(Long value) { totalCents = value; }
    public List<PurchaseOrderLine> getLines() { return lines; }
    public void setLines(List<PurchaseOrderLine> value) { lines = value; }
}
