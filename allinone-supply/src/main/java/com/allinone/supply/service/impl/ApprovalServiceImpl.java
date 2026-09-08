package com.allinone.supply.service.impl;

import com.allinone.common.exception.ServiceException;
import com.allinone.common.utils.DateUtils;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.StringUtils;
import com.allinone.common.utils.uuid.IdUtils;
import com.allinone.supply.domain.ApprovalDecision;
import com.allinone.supply.domain.ApprovalTask;
import com.allinone.supply.domain.ApprovalTransfer;
import com.allinone.supply.mapper.ApprovalMapper;
import com.allinone.supply.service.IApprovalService;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalServiceImpl implements IApprovalService {
    @Autowired private ApprovalMapper mapper;
    @Autowired private com.allinone.supply.support.TaskTransferAuthorization transferAuthorization;

    @Override public List<ApprovalTask> selectPendingTasks() {
        return mapper.selectPendingTasks(SecurityUtils.getUserId());
    }

    @Override @Transactional public int approve(ApprovalDecision decision) {
        return decide(decision, "APPROVE", "APPROVED");
    }

    @Override @Transactional public int reject(ApprovalDecision decision) {
        if (decision == null || StringUtils.isEmpty(decision.getComment()) || decision.getComment().trim().isEmpty()) throw new ServiceException("退回原因不能为空");
        return decide(decision, "RETURN", "RETURNED");
    }

    @Override @Transactional public int transfer(ApprovalTransfer request) {
        if (request == null || request.getTaskId() == null || request.getAssigneeId() == null) {
            throw new ServiceException("接替人不存在或已停用");
        }
        ApprovalTask task = mapper.selectTask(request.getTaskId());
        if (task == null || !"PENDING".equals(task.getStatus())) throw new ServiceException("待办任务不存在或已处理");
        transferAuthorization.requireOperator(task.getDocumentId(), "supply:approval:transfer");
        transferAuthorization.lockDocument(task.getDocumentId());
        task = mapper.selectTaskForUpdate(request.getTaskId());
        if (task == null || !"PENDING".equals(task.getStatus())) throw new ServiceException("待办任务不存在或已处理");
        transferAuthorization.requireRecipient(task.getDocumentId(), request.getAssigneeId(), "supply:approval:approve");
        boolean eligible = "FINANCE".equals(task.getNode())
                ? transferAuthorization.hasRole(request.getAssigneeId(), "finance")
                : "PURCHASE".equals(task.getNode()) ? transferAuthorization.hasRole(request.getAssigneeId(), "purchaser")
                : "SUPERVISOR".equals(task.getNode()) && transferAuthorization.isSupervisor(request.getAssigneeId());
        if (!eligible) throw new ServiceException("接替人不具备当前审批节点的业务资格");
        if (request.getAssigneeId().equals(task.getAssigneeId())) throw new ServiceException("接替人不能与当前办理人相同");
        int rows = mapper.transferTask(request.getTaskId(), request.getAssigneeId(), task.getAssigneeId());
        if (rows == 0) throw new ServiceException("任务已变化，请刷新后重试");
        if (mapper.insertAudit(IdUtils.nextLongId(), task.getDocumentId(), task.getVersionId(), SecurityUtils.getUserId(), SecurityUtils.getUsername(), "TRANSFER", "原办理人：" + task.getAssigneeId() + "；接替人：" + request.getAssigneeId(), DateUtils.getNowDate()) != 1) {
            throw new ServiceException("转交审计保存失败");
        }
        return rows;
    }

    private int decide(ApprovalDecision decision, String taskDecision, String documentStatus) {
        if (decision == null || decision.getTaskId() == null || decision.getDocumentId() == null || decision.getTaskRevision() == null || decision.getDocumentRevision() == null) throw new ServiceException("缺少审批任务或单据版本号");
        if (decision.getComment() != null && decision.getComment().length() > 2000) throw new ServiceException("审批意见不能超过2000个字符");
        ApprovalTask task = mapper.selectTask(decision.getTaskId());
        if (task == null || !decision.getDocumentId().equals(task.getDocumentId())) throw new ServiceException("审批任务不存在");
        transferAuthorization.lockDocument(task.getDocumentId());
        task = mapper.selectTaskForUpdate(decision.getTaskId());
        if (task == null) throw new ServiceException("审批任务已变化，请刷新后重试");
        if (!SecurityUtils.getUserId().equals(task.getAssigneeId())) throw new ServiceException("无权处理该审批任务");
        if (!"PENDING".equals(task.getStatus())) throw new ServiceException("该审批任务已处理，请刷新后重试");
        Date now = DateUtils.getNowDate();
        if (mapper.completeTask(task.getTaskId(), decision.getTaskRevision(), taskDecision, decision.getComment(), SecurityUtils.getUserId(), now) == 0) throw new ServiceException("审批任务已变化，请刷新后重试");
        boolean invoicePurchasePass = "APPROVE".equals(taskDecision) && "INVOICE".equals(task.getDocumentType()) && "PURCHASE".equals(task.getNode());
        String nextNode = invoicePurchasePass ? "FINANCE" : null;
        String nextStatus = invoicePurchasePass ? "IN_REVIEW" : documentStatus;
        if (mapper.updateDocument(task.getDocumentId(), decision.getDocumentRevision(), nextStatus, nextNode, task.getNode(), now) == 0) throw new ServiceException("单据状态已变化，请刷新后重试");
        if (invoicePurchasePass) {
            Long financeId = firstEligibleFinance(mapper.selectFinanceCandidates(task.getInstanceId()), task);
            if (financeId == null) throw new ServiceException("未配置合法的财务审批人");
            mapper.insertTask(IdUtils.nextLongId(), task.getInstanceId(), "FINANCE", 2, financeId);
            mapper.insertAudit(IdUtils.nextLongId(), task.getDocumentId(), task.getVersionId(), SecurityUtils.getUserId(), SecurityUtils.getUsername(), "PURCHASE_APPROVED", decision.getComment(), now);
            return 1;
        }
        if ("INVOICE".equals(task.getDocumentType())) {
            if ("APPROVE".equals(taskDecision)) mapper.confirmInvoiceAllocations(task.getDocumentId());
            else mapper.releaseInvoiceAllocations(task.getDocumentId());
        }
        mapper.cancelPendingTasks(task.getInstanceId());
        mapper.updateWorkflow(task.getInstanceId(), "APPROVE".equals(taskDecision) ? "APPROVED" : "RETURNED", now);
        mapper.insertAudit(IdUtils.nextLongId(), task.getDocumentId(), task.getVersionId(), SecurityUtils.getUserId(), SecurityUtils.getUsername(), taskDecision, decision.getComment(), now);
        return 1;
    }

    private Long first(String value) { try { return StringUtils.isEmpty(value) ? null : Long.valueOf(value.split(",")[0].trim()); } catch (Exception e) { return null; } }
    private Long firstEligibleFinance(String value, ApprovalTask task) { if (StringUtils.isEmpty(value) || task.getSubmitterId() == null) return null; for (String candidate : value.split(",")) { try { Long userId=Long.valueOf(candidate.trim()); if (mapper.selectEligibleFinanceCount(userId, task.getSubmitterId(), task.getAssigneeId()) > 0) return userId; } catch (NumberFormatException ignored) { } } return null; }
}
