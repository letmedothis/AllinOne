package com.allinone.framework.approval;

import com.allinone.common.approval.*;
import com.allinone.common.exception.ServiceException;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.framework.approval.mapper.ReviewMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DelegationReviewHandler implements ReviewHandler {
    @Autowired private ReviewMapper mapper;
    @Autowired private ApprovalIdentityService identity;
    private final ObjectMapper json=new ObjectMapper();
    public String type(){return "DELEGATION";}
    public void prepare(ReviewCase c,boolean creating) {
        DelegationGrant g=parse(c);
        g.id=c.id; if(g.principalId==null)g.principalId=c.ownerId;
        if(!Objects.equals(g.principalId,c.ownerId) && !SecurityUtils.isAdmin()) throw new ServiceException("只能申请自己的代理");
        if(g.agentId==null || g.agentId.equals(g.principalId))throw new ServiceException("请选择不同的代理人");
        validate(g);
        c.targetId=c.id; c.deptId=g.deptId; c.title="代理授权："+mapper.user(g.principalId).name+" → "+mapper.user(g.agentId).name;
        c.sensitive=g.node.startsWith("FINANCE");
        g.principalDeptId=mapper.user(g.principalId).deptId; g.agentDeptId=mapper.user(g.agentId).deptId;
        g.status="PENDING";
        c.afterJson=write(g); if(creating)c.beforeJson="{}";
    }
    private void validate(DelegationGrant g) {
        if(g.type==null||g.node==null||g.deptId==null||g.startsAt==null||g.endsAt==null)throw new ServiceException("业务、节点、部门和代理期限不能为空");
        long duration=g.endsAt.getTime()-g.startsAt.getTime();
        if(duration<=0||duration>180L*24*60*60*1000||!g.endsAt.after(new Date()))throw new ServiceException("代理期限必须有效且不超过180天");
        if(g.maxCents!=null && (!"PAYMENT".equals(g.type)||g.maxCents<=0))throw new ServiceException("仅付款代理可设置正数金额上限");
        if(!identity.eligible(g.principalId,g.type,g.node)||!identity.eligible(g.agentId,g.type,g.node))throw new ServiceException("委托人或代理人缺少节点资格/权限");
        ApprovalUser principal=mapper.user(g.principalId);
        if(!Objects.equals(principal.deptId,g.deptId)&&!"EXEC".equals(principal.rank))throw new ServiceException("授权不能超出委托人部门范围");
        Set<String> nodes=switch(g.type){
            case "LEGACY" -> Set.of("SUPERVISOR","PURCHASE","FINANCE");
            case "PAYMENT" -> Set.of("FINANCE_REVIEW","FINANCE_DIRECTOR");
            case "SUPPLIER_CHANGE","DELEGATION" -> Set.of("SUPERVISOR","FINANCE");
            case "COLLECT_CORRECTION" -> Set.of("SUPERVISOR");
            case "FLOWABLE" -> Set.of(g.node);
            default -> Set.of();
        };
        if(!nodes.contains(g.node)||!g.node.matches("[A-Za-z][A-Za-z0-9_-]{0,79}"))throw new ServiceException("不支持该业务代理节点");
    }
    public void validateSubmission(ReviewCase c){
        DelegationGrant g=parse(c); validate(g);
        for(Long id:new TreeSet<>(List.of(g.principalId,g.agentId)))if(mapper.lockUser(id)==null)throw new ServiceException("授权涉及账号已停用");
        if(mapper.overlapping(g)>0||mapper.chain(g)>0)throw new ServiceException("存在重叠授权或代理链，请调整范围和期限");
    }
    public void submitted(ReviewCase c){
        DelegationGrant g=parse(c);g.id=c.id;g.status="PENDING";
        if(mapper.grant(c.id)==null)mapper.insertGrant(g);
        else {
            // 退回后重新提交使用保存的完整申请内容，避免沿用旧授权范围。
            mapper.replaceGrant(g);
        }
    }
    public void apply(ReviewCase c){validateSubmission(c);mapper.grantStatus(c.id,"ACTIVE");}
    public void cancelled(ReviewCase c){if(mapper.grant(c.id)!=null)mapper.grantStatus(c.id,"CANCELLED");}
    private DelegationGrant parse(ReviewCase c){try{return json.readValue(c.afterJson,DelegationGrant.class);}catch(Exception e){throw new ServiceException("代理申请格式不正确");}}
    private String write(Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw new ServiceException("代理快照生成失败");}}
}
