package com.allinone.common.approval;

import java.util.Date;
public class DelegationGrant {
    public Long id;
    public Long principalId;
    public Long agentId;
    public Long deptId;
    public Long principalDeptId;
    public Long agentDeptId;
    public String type;
    public String node;
    public Long maxCents;
    public Date startsAt;
    public Date endsAt;
    public String status;
    public String principalName;
    public String agentName;
    public Long getId(){return id;} public void setId(Long v){id=v;} public Long getPrincipalId(){return principalId;} public void setPrincipalId(Long v){principalId=v;} public Long getAgentId(){return agentId;} public void setAgentId(Long v){agentId=v;}
    public Long getDeptId(){return deptId;} public void setDeptId(Long v){deptId=v;} public Long getPrincipalDeptId(){return principalDeptId;} public void setPrincipalDeptId(Long v){principalDeptId=v;} public Long getAgentDeptId(){return agentDeptId;} public void setAgentDeptId(Long v){agentDeptId=v;}
    public String getType(){return type;} public void setType(String v){type=v;} public String getNode(){return node;} public void setNode(String v){node=v;} public Long getMaxCents(){return maxCents;} public void setMaxCents(Long v){maxCents=v;}
    public Date getStartsAt(){return startsAt;} public void setStartsAt(Date v){startsAt=v;} public Date getEndsAt(){return endsAt;} public void setEndsAt(Date v){endsAt=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;}
}
