package com.allinone.supply.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import java.util.Date;

/** 期初未付余额，不生成采购订单或入库事实。 */
public class OpeningPayable {
    private Long documentId;
    private String number;
    private String supplierName;
    private String approvalStatus;
    private String currentNode;
    private Integer revision;
    @NotNull private Long supplierId;
    @NotBlank @Size(max=32) private String invoiceNumber;
    @NotNull @PastOrPresent @JsonFormat(pattern="yyyy-MM-dd") private Date issueDate;
    @NotNull @PastOrPresent @JsonFormat(pattern="yyyy-MM-dd") private Date openingDate;
    @NotNull @Positive private Long balanceCents;
    @NotBlank @Size(max=500) private String reason;
    public Long getDocumentId(){return documentId;} public void setDocumentId(Long v){documentId=v;}
    public String getNumber(){return number;} public void setNumber(String v){number=v;}
    public String getSupplierName(){return supplierName;} public void setSupplierName(String v){supplierName=v;}
    public String getApprovalStatus(){return approvalStatus;} public void setApprovalStatus(String v){approvalStatus=v;}
    public String getCurrentNode(){return currentNode;} public void setCurrentNode(String v){currentNode=v;}
    public Integer getRevision(){return revision;} public void setRevision(Integer v){revision=v;}
    public Long getSupplierId(){return supplierId;} public void setSupplierId(Long v){supplierId=v;}
    public String getInvoiceNumber(){return invoiceNumber;} public void setInvoiceNumber(String v){invoiceNumber=v;}
    public Date getIssueDate(){return issueDate;} public void setIssueDate(Date v){issueDate=v;}
    public Date getOpeningDate(){return openingDate;} public void setOpeningDate(Date v){openingDate=v;}
    public Long getBalanceCents(){return balanceCents;} public void setBalanceCents(Long v){balanceCents=v;}
    public String getReason(){return reason;} public void setReason(String v){reason=v;}
}
