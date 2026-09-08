package com.allinone.supply.service.impl;

import com.allinone.common.approval.*;
import com.allinone.common.exception.ServiceException;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.framework.approval.mapper.ReviewMapper;
import com.allinone.supply.domain.Supplier;
import com.allinone.supply.domain.Payment;
import com.allinone.supply.mapper.SupplierChangeMapper;
import com.allinone.supply.mapper.SupplierMapper;
import com.allinone.supply.service.ISupplierService;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SupplierChangeService implements ReviewHandler {
    @Autowired private SupplierChangeMapper mapper;
    @Autowired private SupplierMapper supplierMapper;
    @Autowired private ISupplierService supplierService;
    @Autowired private ReviewMapper review;
    private final ObjectMapper json=new ObjectMapper();
    public String type(){return "SUPPLIER_CHANGE";}
    public void lock(Long id){if(mapper.lockSupplier(id)==null)throw new ServiceException("供应商未准入、已作废或不存在");}
    public String snapshot(Supplier s){
        Map<String,Object> p=new LinkedHashMap<>();p.put("name",s.getName());p.put("taxId",s.getTaxId());p.put("contact",s.getContact());p.put("phone",s.getPhone());p.put("address",s.getAddress());p.put("bankName",s.getBankName());p.put("accountName",s.getAccountName());p.put("bankAccount",s.getBankAccount());return write(p);
    }
    public void prepare(ReviewCase c,boolean creating){
        if(c.targetId==null)throw new ServiceException("请选择供应商");
        Supplier s=supplierService.selectSupplierById(c.targetId);
        if(!Objects.equals(s.getCreatorId(),c.ownerId)&&!SecurityUtils.isAdmin())throw new ServiceException("请由供应商负责人发起变更，人员变动先办理交接");
        lock(c.targetId);s=supplierMapper.selectSupplierById(c.targetId);
        if(creating){
            int version=mapper.profileVersion(c.targetId);
            if(version==0){mapper.insertVersion(c.targetId,1,snapshot(s),null);version=1;}
            c.baseVersion=version;c.beforeJson=snapshot(s);c.title="供应商变更："+s.getName();
            ApprovalUser owner=review.user(s.getCreatorId());if(owner==null||owner.deptId==null)throw new ServiceException("供应商负责人部门未配置");c.deptId=owner.deptId;
        }
        Supplier changed=read(c.afterJson);
        if(!Objects.equals(s.getTaxId(),changed.getTaxId()))throw new ServiceException("税号变化须新供应商准入，不能更换原主体");
        for(String value:List.of(Objects.toString(changed.getName(),""),Objects.toString(changed.getContact(),""),Objects.toString(changed.getPhone(),""),Objects.toString(changed.getAddress(),""),Objects.toString(changed.getBankName(),""),Objects.toString(changed.getAccountName(),""),Objects.toString(changed.getBankAccount(),"")))if(value.isBlank()||value.length()>200)throw new ServiceException("供应商字段不能为空且不能超过200字");
        c.afterJson=snapshot(changed);
        Supplier old=read(c.beforeJson);
        c.sensitive=!Objects.equals(old.getName(),changed.getName())||!Objects.equals(old.getBankName(),changed.getBankName())||!Objects.equals(old.getAccountName(),changed.getAccountName())||!Objects.equals(old.getBankAccount(),changed.getBankAccount());
    }
    public void validateSubmission(ReviewCase c){
        lock(c.targetId);
        if(mapper.profileVersion(c.targetId)!=c.baseVersion)throw new ServiceException("原资料版本已变化，请重新核对");
        if(c.beforeJson.equals(c.afterJson))throw new ServiceException("没有资料变更");
        if(c.sensitive&&review.files(c.id).isEmpty())throw new ServiceException("账户或名称变更必须上传证明材料");
    }
    public void apply(ReviewCase c){
        validateSubmission(c);Supplier s=read(c.afterJson);s.setDocumentId(c.targetId);
        if(mapper.applySupplier(s)!=1||mapper.bump(c.targetId)!=1)throw new ServiceException("供应商资料生效失败");
        mapper.insertVersion(c.targetId,c.baseVersion+1,c.afterJson,c.id);
        if(c.sensitive){mapper.invalidateAccounts(c.targetId);mapper.requireReview(c.targetId);}
    }
    public void submitted(ReviewCase c){lock(c.targetId);}
    public void captureAccount(Payment p){
        lock(p.getSupplierId());Supplier s=supplierMapper.selectSupplierById(p.getSupplierId());
        int version=mapper.profileVersion(p.getSupplierId());
        if(version==0){mapper.insertVersion(p.getSupplierId(),1,snapshot(s),null);version=1;}
        mapper.account(p.getId(),p.getSupplierId(),version,snapshot(s));
    }
    public void requireExecutable(Payment p){
        lock(p.getSupplierId());
        if(mapper.blocked(p.getSupplierId())>0)throw new ServiceException("供应商账户/名称变更审核中，暂不能执行付款");
        Map<String,Object> a=mapper.accountSnapshot(p.getId());
        if(a==null||!Boolean.TRUE.equals(a.get("confirmed")))throw new ServiceException("收款账户尚未确认，请重新进行财务复核");
    }
    private Supplier read(String text){try{return json.readValue(text,Supplier.class);}catch(Exception e){throw new ServiceException("供应商资料格式错误");}}
    private String write(Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw new ServiceException("资料快照失败");}}
}
