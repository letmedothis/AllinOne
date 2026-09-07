package com.allinone.supply.domain;

/** 采购订单 Flowable 待办动作。 */
public class WorkflowTaskDecision {
    private String action;
    private String comment;

    public String getAction() { return action; }
    public void setAction(String value) { action = value; }
    public String getComment() { return comment; }
    public void setComment(String value) { comment = value; }
}
