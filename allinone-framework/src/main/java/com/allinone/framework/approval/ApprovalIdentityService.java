package com.allinone.framework.approval;

import com.allinone.common.approval.*;
import com.allinone.common.exception.ServiceException;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.uuid.IdUtils;
import com.allinone.framework.approval.mapper.ReviewMapper;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalIdentityService {
    @Autowired private ReviewMapper mapper;
    public static String prefix(String type) {
        return switch(type) {
            case "SUPPLIER_CHANGE" -> "supply:supplierChange";
            case "COLLECT_CORRECTION" -> "collect:correction";
            case "DELEGATION" -> "system:delegation";
            case "LEGACY" -> "supply:approval";
            case "FLOWABLE" -> "supply:workflow";
            case "PAYMENT" -> "supply:payment";
            default -> throw new ServiceException("不支持的业务类型");
        };
    }
    public boolean supervisor(Long user) {
        return mapper.role(user,"supervisor")>0 || mapper.role(user,"purchase_supervisor")>0 || mapper.role(user,"purchasing_supervisor")>0;
    }
    public boolean eligible(Long user,String type,String node) {
        if (user==null || mapper.user(user)==null) return false;
        String permission = "FLOWABLE".equals(type) ? "task" : "PAYMENT".equals(type)
                ? ("FINANCE_DIRECTOR".equals(node)?"director":"review") : "approve";
        if(mapper.permission(user,prefix(type)+":"+permission)==0) return false;
        if("FLOWABLE".equals(type) || "COLLECT_CORRECTION".equals(type)) return true; // 发布节点/模板配置另行核对
        if(node.startsWith("FINANCE")) return mapper.role(user,"finance")>0
                && (!node.equals("FINANCE_DIRECTOR") || "EXEC".equals(mapper.user(user).rank));
        if("PURCHASE".equals(node)) return mapper.role(user,"purchaser")>0;
        return supervisor(user);
    }
    public boolean valid(DelegationGrant g) {
        Date now=new Date();
        if(g==null || !"ACTIVE".equals(g.status) || now.before(g.startsAt) || !now.before(g.endsAt)) return false;
        ApprovalUser p=mapper.user(g.principalId), a=mapper.user(g.agentId);
        return p!=null && a!=null && Objects.equals(p.deptId,g.principalDeptId) && Objects.equals(a.deptId,g.agentDeptId)
                && eligible(g.principalId,g.type,g.node) && eligible(g.agentId,g.type,g.node);
    }
    public List<DelegationGrant> activeGrants() {
        return mapper.grants(SecurityUtils.getUserId()).stream().filter(this::valid).toList();
    }
    public boolean mayAct(Long principal,String type,String node,Long dept,Long cents) {
        Long current=SecurityUtils.getUserId();
        if(!eligible(current,type,node)) return false;
        if(Objects.equals(principal,current)) return true;
        return activeGrants().stream().anyMatch(g -> matches(g,principal,type,node,dept,cents));
    }
    private boolean matches(DelegationGrant g,Long principal,String type,String node,Long dept,Long cents) {
        return Objects.equals(g.principalId,principal) && g.type.equals(type) && g.node.equals(node) && Objects.equals(g.deptId,dept)
            && (g.maxCents==null || (cents!=null && cents<=g.maxCents));
    }
    public ApprovalActor resolve(Long principal,String type,String node,Long dept,Long cents,Long applicant) {
        Long actor=SecurityUtils.getUserId();
        if(Objects.equals(actor,applicant) || Objects.equals(principal,applicant)) throw new ServiceException("申请人不能审批自己的业务");
        if(!eligible(actor,type,node) || !eligible(principal,type,node)) throw new ServiceException("审批资格已失效");
        if(Objects.equals(actor,principal)) return new ApprovalActor(principal,actor,null);
        for(DelegationGrant g:activeGrants()) {
            if(matches(g,principal,type,node,dept,cents)) {
                DelegationGrant locked=mapper.lockGrant(g.id);
                if(valid(locked) && matches(locked,principal,type,node,dept,cents)) return new ApprovalActor(principal,actor,g.id);
            }
        }
        throw new ServiceException("代理未生效、已到期或不在授权范围内");
    }
    public void record(String type,String businessId,String round,String node,ApprovalActor actor,String action) {
        if(mapper.actorConflict(type,businessId,round,node,actor.actorId())>0
                || mapper.actorConflict(type,businessId,round,node,actor.responsibleId())>0) throw new ServiceException("本轮审批职责冲突，不能由同一人员办理不同节点");
        if(mapper.recordActor(IdUtils.nextLongId(),type,businessId,round,node,actor,action)!=1) throw new ServiceException("审批身份记录失败");
    }
    @Transactional
    public void revoke(Long id) {
        DelegationGrant g=mapper.lockGrant(id);
        if(g==null) throw new ServiceException("代理不存在");
        if(!Objects.equals(g.principalId,SecurityUtils.getUserId()) && !SecurityUtils.isAdmin()) throw new ServiceException("仅委托人或管理员可以撤销");
        mapper.grantStatus(id,"REVOKED");
    }
}
