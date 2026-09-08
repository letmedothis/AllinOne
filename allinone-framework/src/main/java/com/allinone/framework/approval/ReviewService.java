package com.allinone.framework.approval;

import com.allinone.common.approval.*;
import com.allinone.common.exception.ServiceException;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.uuid.IdUtils;
import com.allinone.framework.approval.mapper.ReviewMapper;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {
    @Autowired private ReviewMapper mapper;
    @Autowired private ApprovalIdentityService identity;
    @Autowired private ObjectProvider<ReviewHandler> handlers;
    public void permission(String type,String action) {
        if(!SecurityUtils.hasPermi(ApprovalIdentityService.prefix(type)+":"+action)) throw new ServiceException("没有该业务操作权限");
    }
    private ReviewHandler handler(String type) { return handlers.orderedStream().filter(h->h.type().equals(type)).findFirst().orElseThrow(()->new ServiceException("业务未启用")); }
    private Long principal(ReviewCase c) { return "FINANCE".equals(c.node)?c.financeId:c.supervisorId; }
    public ReviewCase get(Long id) {
        ReviewCase c=mapper.get(id); if(c==null) throw new ServiceException("申请不存在");
        permission(c.type,"query");
        Long user=SecurityUtils.getUserId();
        c.canEdit=Objects.equals(c.ownerId,user) && Set.of("DRAFT","RETURNED").contains(c.status);
        c.canDecide="IN_REVIEW".equals(c.status) && !Objects.equals(c.ownerId,user) && identity.mayAct(principal(c),c.type,c.node,c.deptId,null);
        if(!SecurityUtils.isAdmin() && !Objects.equals(user,c.ownerId) && !Objects.equals(user,c.supervisorId)
                && !Objects.equals(user,c.financeId) && !c.canDecide) throw new ServiceException("无权访问该申请");
        return c;
    }
    public List<ReviewCase> list(String type) { permission(type,"query"); return mapper.list(type,SecurityUtils.getUserId(),SecurityUtils.isAdmin()); }
    @Transactional
    public ReviewCase save(ReviewCase input) {
        permission(input.type,"add");
        if(input.afterJson==null || input.afterJson.length()>5_000_000) throw new ServiceException("申请内容为空或超过大小限制");
        ReviewCase c;
        boolean creating=input.id==null;
        if(creating) {
            c=new ReviewCase(); c.id=IdUtils.nextLongId(); c.type=input.type; c.targetId=input.targetId;
            c.ownerId=SecurityUtils.getUserId(); c.deptId=SecurityUtils.getDeptId(); c.status="DRAFT";
            c.createdAt=new Date(); c.updatedAt=c.createdAt;
        } else {
            c=mapper.lock(input.id); requireOwner(c,input.revision);
            if(!c.type.equals(input.type) || !Set.of("DRAFT","RETURNED").contains(c.status)) throw new ServiceException("当前申请不能修改");
        }
        c.afterJson=input.afterJson; c.reason=input.reason;
        if(c.reason!=null && c.reason.length()>1000) throw new ServiceException("原因不能超过1000字");
        handler(c.type).prepare(c,creating);
        if(creating) { if(mapper.insert(c)!=1) throw new ServiceException("申请保存失败"); }
        else update(c);
        return c;
    }
    @Transactional
    public ReviewCase submit(Long id,int revision,String key) {
        ReviewCase c=mapper.lock(id); requireOwner(c,revision); permission(c.type,"add");
        if(!Set.of("DRAFT","RETURNED").contains(c.status) || c.reason==null || c.reason.isBlank()) throw new ServiceException("请填写原因并保存草稿后提交");
        handler(c.type).validateSubmission(c);
        long targetKey="COLLECT_CORRECTION".equals(c.type) ? configTarget(c) : 0L;
        ReviewConfig config=mapper.config(c.type,targetKey,c.deptId);
        if(config==null || !identity.eligible(config.supervisorId,c.type,"SUPERVISOR") || Objects.equals(config.supervisorId,c.ownerId)) throw new ServiceException("待配置：缺少合法审核人，请联系审批配置管理员");
        if(c.sensitive && (config.financeId==null || Objects.equals(config.financeId,c.ownerId) || Objects.equals(config.financeId,config.supervisorId)
                || !identity.eligible(config.financeId,c.type,"FINANCE"))) throw new ServiceException("待配置：缺少独立财务复核人");
        c.supervisorId=config.supervisorId; c.financeId=c.sensitive?config.financeId:null;
        if("DELEGATION".equals(c.type)) {
            DelegationGrant g;
            try { g=new com.fasterxml.jackson.databind.ObjectMapper().readValue(c.afterJson,DelegationGrant.class); }
            catch(Exception e){throw new ServiceException("授权内容无效");}
            if(Objects.equals(g.agentId,c.supervisorId)||Objects.equals(g.principalId,c.supervisorId)
               || Objects.equals(g.agentId,c.financeId)||Objects.equals(g.principalId,c.financeId)) throw new ServiceException("授权涉及人员不能审核自己的授权");
        }
        c.firstActorId=null; c.round++; c.node="SUPERVISOR"; c.status="IN_REVIEW";
        handler(c.type).submitted(c); update(c); event(c,"SUBMIT",self(),c.reason,key); return c;
    }
    private long configTarget(ReviewCase c) {
        try { return new com.fasterxml.jackson.databind.ObjectMapper().readTree(c.beforeJson).path("templateId").asLong(); }
        catch(Exception e) { throw new ServiceException("原始版本信息不完整"); }
    }
    @Transactional
    public ReviewCase decide(Long id,int revision,boolean approved,String comment,String key) {
        ReviewCase c=mapper.lock(id); if(c==null) throw new ServiceException("申请不存在"); permission(c.type,"approve");
        requireKey(key);
        if(mapper.hasEvent(id,key,SecurityUtils.getUserId())>0) return c;
        if(c.revision!=revision || !"IN_REVIEW".equals(c.status)) throw new ServiceException("申请状态已变化，请刷新");
        if(comment==null || comment.isBlank() || comment.length()>2000) throw new ServiceException("请填写审核依据或退回原因（最多2000字）");
        ApprovalActor actor=identity.resolve(principal(c),c.type,c.node,c.deptId,null,c.ownerId);
        if("DELEGATION".equals(c.type)) {
            DelegationGrant g=mapper.grant(c.id);
            if(Objects.equals(g.agentId,actor.actorId())||Objects.equals(g.principalId,actor.actorId()))throw new ServiceException("授权涉及人员不能代理审核自己的授权");
        }
        if(Objects.equals(c.firstActorId,actor.actorId()) || Objects.equals(c.firstActorId,actor.responsibleId())) throw new ServiceException("前后节点必须由不同人员办理");
        identity.record(c.type,String.valueOf(c.id),String.valueOf(c.round),c.node,actor,approved?"APPROVE":"RETURN");
        if(!approved) { c.status="RETURNED"; c.node=null; handler(c.type).cancelled(c); }
        else if(c.sensitive && "SUPERVISOR".equals(c.node)) { c.firstActorId=actor.actorId(); c.node="FINANCE"; }
        else { handler(c.type).apply(c); c.status="APPLIED"; c.node=null; }
        update(c); event(c,approved?"APPROVE":"RETURN",actor,comment,key); return c;
    }
    @Transactional
    public ReviewCase cancel(Long id,int revision,String reason,String key) {
        ReviewCase c=mapper.lock(id); requireOwner(c,revision); permission(c.type,"add");
        if(reason==null || reason.isBlank()) throw new ServiceException("请填写撤回/取消原因");
        if(!Set.of("DRAFT","RETURNED","IN_REVIEW").contains(c.status)) throw new ServiceException("已生效的申请不能撤回");
        boolean withdrawal="IN_REVIEW".equals(c.status);
        c.status=withdrawal?"DRAFT":"CANCELLED"; c.node=null; handler(c.type).cancelled(c); update(c);
        event(c,withdrawal?"WITHDRAW":"CANCEL",self(),reason,key); return c;
    }
    private void requireOwner(ReviewCase c,int revision) { if(c==null || !Objects.equals(c.ownerId,SecurityUtils.getUserId()) || c.revision!=revision) throw new ServiceException("不是申请负责人或版本已变化"); }
    private ApprovalActor self(){return new ApprovalActor(SecurityUtils.getUserId(),SecurityUtils.getUserId(),null);}
    @Transactional
    public void attach(Long id,int revision,String name,String path,String hash){
        ReviewCase c=mapper.lock(id);requireOwner(c,revision);permission(c.type,"add");
        if(!Set.of("DRAFT","RETURNED").contains(c.status))throw new ServiceException("当前申请不能上传材料");
        if(mapper.insertFile(IdUtils.nextLongId(),id,name,path,hash,SecurityUtils.getUserId())!=1)throw new ServiceException("材料保存失败");
    }
    @Transactional
    public void revokeDelegation(Long id,int revision,String reason,String key){
        ReviewCase c=mapper.lock(id);if(c==null||!"DELEGATION".equals(c.type)||!"APPLIED".equals(c.status)||c.revision!=revision)throw new ServiceException("授权状态已变化");
        if(reason==null||reason.isBlank())throw new ServiceException("请填写撤销原因");
        identity.revoke(id);c.status="REVOKED";update(c);event(c,"REVOKE",self(),reason,key);
    }
    private void update(ReviewCase c){if(mapper.update(c)!=1)throw new ServiceException("申请已被其他人员修改"); c.revision++;}
    private void requireKey(String key){if(key==null||key.isBlank()||key.length()>80)throw new ServiceException("缺少合法请求标识");}
    private void event(ReviewCase c,String action,ApprovalActor actor,String comment,String key){requireKey(key);if(mapper.event(IdUtils.nextLongId(),c,action,actor,comment,key)!=1)throw new ServiceException("审计写入失败");}
}
