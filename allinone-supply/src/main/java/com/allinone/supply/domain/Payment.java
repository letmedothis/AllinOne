package com.allinone.supply.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 付款单(M4)。金额统一为人民币分。 */
public class Payment {
    private Long id;
    private String number;
    private Long supplierId;
    private String supplierName;
    private Long amountCents;
    private String thresholdExceeded; // 0/1,服务端按 >10 万元计算
    private String status;            // DRAFT/IN_REVIEW/RETURNED/APPROVED/VOID
    private String currentNode;       // FINANCE_REVIEW/FINANCE_DIRECTOR
    private Long creatorId;
    private String creationKey;
    private Integer revision;
    private String remark;
    private String reviewComment;
    private String directorComment;
    private java.util.Date createTime;
    private java.util.Date updateTime;
    private String deleted;
    private List<PaymentLine> lines = new ArrayList<>();
    private Map<String, Object> params = new HashMap<>();

    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params == null ? new HashMap<>() : params; }

    public Long getId() { return id; } public void setId(Long v) { id = v; }
    public String getNumber() { return number; } public void setNumber(String v) { number = v; }
    public Long getSupplierId() { return supplierId; } public void setSupplierId(Long v) { supplierId = v; }
    public String getSupplierName() { return supplierName; } public void setSupplierName(String v) { supplierName = v; }
    public Long getAmountCents() { return amountCents; } public void setAmountCents(Long v) { amountCents = v; }
    public String getThresholdExceeded() { return thresholdExceeded; } public void setThresholdExceeded(String v) { thresholdExceeded = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public String getCurrentNode() { return currentNode; } public void setCurrentNode(String v) { currentNode = v; }
    public Long getCreatorId() { return creatorId; } public void setCreatorId(Long v) { creatorId = v; }
    public String getCreationKey() { return creationKey; } public void setCreationKey(String v) { creationKey = v; }
    public Integer getRevision() { return revision; } public void setRevision(Integer v) { revision = v; }
    public String getRemark() { return remark; } public void setRemark(String v) { remark = v; }
    public String getReviewComment() { return reviewComment; } public void setReviewComment(String v) { reviewComment = v; }
    public String getDirectorComment() { return directorComment; } public void setDirectorComment(String v) { directorComment = v; }
    public java.util.Date getCreateTime() { return createTime; } public void setCreateTime(java.util.Date v) { createTime = v; }
    public java.util.Date getUpdateTime() { return updateTime; } public void setUpdateTime(java.util.Date v) { updateTime = v; }
    public String getDeleted() { return deleted; } public void setDeleted(String v) { deleted = v; }
    public List<PaymentLine> getLines() { return lines; } public void setLines(List<PaymentLine> v) { lines = v == null ? new ArrayList<>() : v; }
}
