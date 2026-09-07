package com.allinone.supply.domain;

/** 付款审批动作(复核/总监共用)。 */
public class PaymentDecision {
    private Boolean approved;
    private String comment;

    public Boolean getApproved() { return approved; }
    public void setApproved(Boolean v) { approved = v; }
    public String getComment() { return comment; }
    public void setComment(String v) { comment = v; }
}
