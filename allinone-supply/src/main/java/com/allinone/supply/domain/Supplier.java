package com.allinone.supply.domain;

import com.allinone.common.core.domain.BaseEntity;
import java.util.Date;

/** 供应商准入单，业务字段与公共单据字段组合返回。 */
public class Supplier extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long documentId;
    private String number;
    private Long creatorId;
    private String creationKey;
    private String approvalStatus;
    private String currentNode;
    private Integer currentVersion;
    private Integer revision;
    private String name;
    private String taxId;
    private String dedupKey;
    private String contact;
    private String phone;
    private String address;
    private String bankName;
    private String accountName;
    private String bankAccount;
    private String attachmentPaths;

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }
    public String getCreationKey() { return creationKey; }
    public void setCreationKey(String creationKey) { this.creationKey = creationKey; }
    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }
    public String getCurrentNode() { return currentNode; }
    public void setCurrentNode(String currentNode) { this.currentNode = currentNode; }
    public Integer getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(Integer currentVersion) { this.currentVersion = currentVersion; }
    public Integer getRevision() { return revision; }
    public void setRevision(Integer revision) { this.revision = revision; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
    public String getDedupKey() { return dedupKey; }
    public void setDedupKey(String dedupKey) { this.dedupKey = dedupKey; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }
    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }
    public String getAttachmentPaths() { return attachmentPaths; }
    public void setAttachmentPaths(String attachmentPaths) { this.attachmentPaths = attachmentPaths; }
}
