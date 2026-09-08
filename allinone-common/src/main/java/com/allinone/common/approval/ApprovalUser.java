package com.allinone.common.approval;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
public class ApprovalUser {
    @JsonSerialize(using=ToStringSerializer.class) public Long id;
    public String name;
    @JsonSerialize(using=ToStringSerializer.class) public Long deptId;
    public String rank;
    public Long getId(){return id;} public void setId(Long v){id=v;} public String getName(){return name;} public void setName(String v){name=v;} public Long getDeptId(){return deptId;} public void setDeptId(Long v){deptId=v;} public String getRank(){return rank;} public void setRank(String v){rank=v;}
}
