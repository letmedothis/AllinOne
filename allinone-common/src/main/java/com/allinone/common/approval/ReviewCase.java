package com.allinone.common.approval;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.Date;

/** 新业务审批申请。内容版本与并发 revision 分开保存。 */
public class ReviewCase {
    @JsonSerialize(using=ToStringSerializer.class) public Long id;
    public String type;
    @JsonSerialize(using=ToStringSerializer.class) public Long targetId;
    @JsonSerialize(using=ToStringSerializer.class) public Long ownerId;
    @JsonSerialize(using=ToStringSerializer.class) public Long deptId;
    public String title;
    public String status;
    public String node;
    public int revision;
    public int round;
    public int baseVersion;
    public String beforeJson;
    public String afterJson;
    public String reason;
    public boolean sensitive;
    @JsonSerialize(using=ToStringSerializer.class) public Long supervisorId;
    @JsonSerialize(using=ToStringSerializer.class) public Long financeId;
    @JsonSerialize(using=ToStringSerializer.class) public Long firstActorId;
    public Date createdAt;
    public Date updatedAt;
    public String ownerName;
    public boolean canEdit;
    public boolean canDecide;
    public Long getId(){return id;} public void setId(Long v){id=v;} public String getType(){return type;} public void setType(String v){type=v;}
    public Long getTargetId(){return targetId;} public void setTargetId(Long v){targetId=v;} public Long getOwnerId(){return ownerId;} public void setOwnerId(Long v){ownerId=v;}
    public Long getDeptId(){return deptId;} public void setDeptId(Long v){deptId=v;} public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;} public String getNode(){return node;} public void setNode(String v){node=v;}
    public int getRevision(){return revision;} public void setRevision(int v){revision=v;} public int getRound(){return round;} public void setRound(int v){round=v;}
    public int getBaseVersion(){return baseVersion;} public void setBaseVersion(int v){baseVersion=v;} public String getBeforeJson(){return beforeJson;} public void setBeforeJson(String v){beforeJson=v;}
    public String getAfterJson(){return afterJson;} public void setAfterJson(String v){afterJson=v;} public String getReason(){return reason;} public void setReason(String v){reason=v;}
    public boolean isSensitive(){return sensitive;} public void setSensitive(boolean v){sensitive=v;} public Long getSupervisorId(){return supervisorId;} public void setSupervisorId(Long v){supervisorId=v;}
    public Long getFinanceId(){return financeId;} public void setFinanceId(Long v){financeId=v;} public Long getFirstActorId(){return firstActorId;} public void setFirstActorId(Long v){firstActorId=v;}
    public Date getCreatedAt(){return createdAt;} public void setCreatedAt(Date v){createdAt=v;} public Date getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Date v){updatedAt=v;}
}
