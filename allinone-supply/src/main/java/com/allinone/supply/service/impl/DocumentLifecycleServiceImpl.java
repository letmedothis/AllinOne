package com.allinone.supply.service.impl;

import com.allinone.common.exception.ServiceException;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.StringUtils;
import com.allinone.common.utils.uuid.IdUtils;
import com.allinone.supply.domain.DocumentLifecycle;
import com.allinone.supply.mapper.DocumentLifecycleMapper;
import com.allinone.supply.service.IDocumentLifecycleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentLifecycleServiceImpl implements IDocumentLifecycleService {
    @Autowired private DocumentLifecycleMapper mapper;
    @Override @Transactional public int withdraw(Long id, String reason) { DocumentLifecycle d=load(id); requireReason(reason); requireOwner(d); if(!"IN_REVIEW".equals(d.getStatus()))throw new ServiceException("只有审批中的单据可以撤回"); mapper.cancelTasks(id); mapper.releaseAllocations(id); mapper.updateWorkflow(id,"WITHDRAWN"); return change(d,"RETURNED","WITHDRAW",reason); }
    @Override @Transactional public int voidDocument(Long id, String reason) { DocumentLifecycle d=load(id); requireReason(reason); if(!"SUPPLIER".equals(d.getType())&&!"PURCHASE_ORDER".equals(d.getType())&&!"INVOICE".equals(d.getType()))throw new ServiceException("入库单不支持作废，请按后续更正流程处理"); if("DRAFT".equals(d.getStatus())||"RETURNED".equals(d.getStatus())){requireOwner(d);}else if("APPROVED".equals(d.getStatus())){if("SUPPLIER".equals(d.getType())||"PURCHASE_ORDER".equals(d.getType())){if(!SecurityUtils.isAdmin()&&!hasRole("supervisor")&&!hasRole("purchasing_supervisor")&&!hasRole("purchase_supervisor"))throw new ServiceException("已通过供应商或订单仅采购主管或管理员可作废");}else if("INVOICE".equals(d.getType())){if(!SecurityUtils.isAdmin()&&!hasRole("finance"))throw new ServiceException("已通过发票仅财务或管理员可作废");if(!SecurityUtils.isAdmin()&&SecurityUtils.getUserId().equals(d.getCreatorId()))throw new ServiceException("发票提交人不能作废本人已通过发票");}if("SUPPLIER".equals(d.getType())&&mapper.countSupplierReferences(id)>0)throw new ServiceException("供应商已有有效订单，不能作废");if("PURCHASE_ORDER".equals(d.getType())&&(mapper.countOrderReceipts(id)>0||mapper.countOrderInvoices(id)>0))throw new ServiceException("订单已有下游业务，不能作废");}else throw new ServiceException("当前状态不允许作废"); mapper.cancelTasks(id); if("INVOICE".equals(d.getType()))mapper.releaseAllocations(id); return change(d,"VOID","VOID",reason); }
    @Override @Transactional public int restoreInvoice(Long id, String reason) { DocumentLifecycle d=load(id); requireReason(reason); if(!SecurityUtils.isAdmin()||!"INVOICE".equals(d.getType())||!"VOID".equals(d.getStatus()))throw new ServiceException("仅管理员可以恢复已作废发票"); return change(d,"RETURNED","RESTORE",reason); }
    @Override @Transactional public int deleteDraft(Long id) { DocumentLifecycle d=load(id); requireOwner(d); if(!"DRAFT".equals(d.getStatus())||d.getCurrentVersion()!=0)throw new ServiceException("只有未提交草稿允许删除");int rows=mapper.markDeleted(id,d.getRevision());if(rows==0)throw new ServiceException("单据已变化，请刷新后重试");mapper.insertAudit(IdUtils.nextLongId(),id,SecurityUtils.getUserId(),SecurityUtils.getUsername(),"DELETE",null);return rows; }
    private int change(DocumentLifecycle d,String status,String action,String reason){int rows=mapper.updateStatus(d.getDocumentId(),d.getRevision(),status);if(rows==0)throw new ServiceException("单据已变化，请刷新后重试");mapper.insertAudit(IdUtils.nextLongId(),d.getDocumentId(),SecurityUtils.getUserId(),SecurityUtils.getUsername(),action,reason);return rows;}
    private DocumentLifecycle load(Long id){if(id==null)throw new ServiceException("缺少单据ID");DocumentLifecycle d=mapper.selectDocument(id);if(d==null)throw new ServiceException("单据不存在");return d;}
    private void requireOwner(DocumentLifecycle d){if(!SecurityUtils.isAdmin()&&!SecurityUtils.getUserId().equals(d.getCreatorId()))throw new ServiceException("无权操作该单据");}
    private void requireReason(String reason){if(StringUtils.isEmpty(reason)||reason.trim().isEmpty())throw new ServiceException("原因不能为空");}
    private boolean hasRole(String roleKey){return SecurityUtils.getLoginUser()!=null&&SecurityUtils.getLoginUser().getUser().getRoles()!=null&&SecurityUtils.getLoginUser().getUser().getRoles().stream().anyMatch(role->roleKey.equals(role.getRoleKey()));}
}
