package com.allinone.supply.domain;

import java.util.Date;

/** 付款每次动作的不可覆盖业务记录。 */
public class PaymentEvent {
    private Long id;
    private Long paymentId;
    private Long actorId;
    private String actorName;
    private String action;
    private String comment;
    private String snapshot;
    private Date createdAt;
    public Long getId() { return id; } public void setId(Long v) { id = v; }
    public Long getPaymentId() { return paymentId; } public void setPaymentId(Long v) { paymentId = v; }
    public Long getActorId() { return actorId; } public void setActorId(Long v) { actorId = v; }
    public String getActorName() { return actorName; } public void setActorName(String v) { actorName = v; }
    public String getAction() { return action; } public void setAction(String v) { action = v; }
    public String getComment() { return comment; } public void setComment(String v) { comment = v; }
    public String getSnapshot() { return snapshot; } public void setSnapshot(String v) { snapshot = v; }
    public Date getCreatedAt() { return createdAt; } public void setCreatedAt(Date v) { createdAt = v; }
}
