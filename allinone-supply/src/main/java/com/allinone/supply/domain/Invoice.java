package com.allinone.supply.domain;

import com.allinone.common.core.domain.BaseEntity;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class Invoice extends BaseEntity {
    private static final long serialVersionUID = 1L;
    private Long documentId; private String number; private Long creatorId; private String createBy; private String creationKey; private Integer currentVersion; private String currentNode; private String approvalStatus; private Long orderId; private String invoiceNumber; private String invoiceType;
    private Date issueDate; private String sellerName; private String sellerTaxId; private String buyerName; private String buyerTaxId;
    private Long amountCents; private Long taxCents; private Long totalCents; private String differenceNote; private Boolean manualConfirmed; private String confirmedContentHash; private Integer revision; private Long confirmedBy; private Date confirmedAt;
    private List<InvoiceLine> lines;
    public Long getDocumentId(){return documentId;} public void setDocumentId(Long v){documentId=v;} public String getNumber(){return number;} public void setNumber(String v){number=v;}
    public Long getCreatorId(){return creatorId;} public void setCreatorId(Long v){creatorId=v;} public String getCreateBy(){return createBy;} public void setCreateBy(String v){createBy=v;} public String getCreationKey(){return creationKey;} public void setCreationKey(String v){creationKey=v;} public Integer getCurrentVersion(){return currentVersion;} public void setCurrentVersion(Integer v){currentVersion=v;}
    public Long getOrderId(){return orderId;} public void setOrderId(Long v){orderId=v;} public String getInvoiceNumber(){return invoiceNumber;} public void setInvoiceNumber(String v){invoiceNumber=v;}
    public String getInvoiceType(){return invoiceType;} public void setInvoiceType(String v){invoiceType=v;} public Date getIssueDate(){return issueDate;} public void setIssueDate(Date v){issueDate=v;}
    public String getSellerName(){return sellerName;} public void setSellerName(String v){sellerName=v;} public String getSellerTaxId(){return sellerTaxId;} public void setSellerTaxId(String v){sellerTaxId=v;}
    public String getBuyerName(){return buyerName;} public void setBuyerName(String v){buyerName=v;} public String getBuyerTaxId(){return buyerTaxId;} public void setBuyerTaxId(String v){buyerTaxId=v;}
    public Long getAmountCents(){return amountCents;} public void setAmountCents(Long v){amountCents=v;} public Long getTaxCents(){return taxCents;} public void setTaxCents(Long v){taxCents=v;}
    public Long getTotalCents(){return totalCents;} public void setTotalCents(Long v){totalCents=v;} public Integer getRevision(){return revision;} public void setRevision(Integer v){revision=v;}
    public String getDifferenceNote(){return differenceNote;} public void setDifferenceNote(String v){differenceNote=v;} public Boolean getManualConfirmed(){return manualConfirmed;} public void setManualConfirmed(Boolean v){manualConfirmed=v;} public String getConfirmedContentHash(){return confirmedContentHash;} public void setConfirmedContentHash(String v){confirmedContentHash=v;} public Long getConfirmedBy(){return confirmedBy;} public void setConfirmedBy(Long v){confirmedBy=v;} public Date getConfirmedAt(){return confirmedAt;} public void setConfirmedAt(Date v){confirmedAt=v;}
    public String getApprovalStatus(){return approvalStatus;} public void setApprovalStatus(String v){approvalStatus=v;} public String getCurrentNode(){return currentNode;} public void setCurrentNode(String v){currentNode=v;}
    public List<InvoiceLine> getLines(){return lines;} public void setLines(List<InvoiceLine> v){lines=v;}
}
