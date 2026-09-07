package com.allinone.supply.service.impl;

import com.allinone.common.exception.ServiceException;
import com.allinone.common.utils.DateUtils;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.StringUtils;
import com.allinone.common.utils.uuid.IdUtils;
import com.allinone.supply.domain.ApBalanceRow;
import com.allinone.supply.domain.Payment;
import com.allinone.supply.domain.PaymentDecision;
import com.allinone.supply.domain.PaymentLine;
import com.allinone.supply.mapper.PaymentMapper;
import com.allinone.supply.service.IPaymentService;
import com.allinone.supply.support.SupplyDataScopeResolver;
import static com.allinone.supply.support.SupplyDataScopeResolver.MODE_ALL;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 付款/应付核销(M4)。金额为人民币分;审批链:采购申请→财务复核(≤10万直通)→财务总监终审(>10万)。 */
@Service
public class PaymentServiceImpl implements IPaymentService {
    private static final String DOC_TYPE = "PAYMENT";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_IN_REVIEW = "IN_REVIEW";
    private static final String STATUS_RETURNED = "RETURNED";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_VOID = "VOID";
    private static final String NODE_REVIEW = "FINANCE_REVIEW";
    private static final String NODE_DIRECTOR = "FINANCE_DIRECTOR";
    /** 10 万元(分)。 */
    private static final long THRESHOLD_CENTS = 10_000_000L;

    @Autowired private PaymentMapper mapper;
    @Autowired private SupplyDataScopeResolver scopeResolver;

    @Override
    public List<Payment> list(Payment filter) {
        if (filter == null) filter = new Payment();
        scopeResolver.putInto(filter.getParams());
        if (isFinanceUser(SecurityUtils.getUserId())) {
            // 财务/总监在付款模块内可见全部,以便复核/终审
            filter.getParams().put("supplyScopeMode", MODE_ALL);
        }
        return mapper.selectList(filter);
    }

    @Override
    public Payment get(Long id) {
        Payment payment = load(id);
        requireVisible(payment);
        payment.setLines(mapper.selectLines(id));
        return payment;
    }

    @Override
    @Transactional
    public int create(Payment payment) {
        if (payment == null || payment.getSupplierId() == null) throw new ServiceException("付款单供应商不能为空");
        Date now = DateUtils.getNowDate();
        Long userId = SecurityUtils.getUserId();
        payment.setId(IdUtils.nextLongId());
        payment.setNumber(nextNumber(now));
        payment.setCreatorId(userId);
        payment.setCreationKey(StringUtils.isEmpty(payment.getCreationKey()) ? IdUtils.fastSimpleUUID() : payment.getCreationKey());
        payment.setStatus(STATUS_DRAFT); payment.setCurrentNode(null); payment.setRevision(0);
        payment.setAmountCents(sumLines(payment.getLines()));
        payment.setCreateTime(now); payment.setUpdateTime(now);
        fillLines(payment);
        if (mapper.insertPayment(payment) != 1) throw new ServiceException("付款单保存失败");
        insertLines(payment);
        return 1;
    }

    @Override
    @Transactional
    public int update(Payment payment) {
        Payment old = load(payment.getId());
        requireOwner(old);
        if (!(STATUS_DRAFT.equals(old.getStatus()) || STATUS_RETURNED.equals(old.getStatus()))) throw new ServiceException("当前状态不允许编辑付款单");
        payment.setSupplierId(payment.getSupplierId() != null ? payment.getSupplierId() : old.getSupplierId());
        payment.setAmountCents(sumLines(payment.getLines()));
        payment.setUpdateTime(DateUtils.getNowDate());
        if (mapper.updatePayment(payment) == 0) throw new ServiceException("付款单已变化，请刷新后重试");
        mapper.deleteLines(payment.getId()); fillLines(payment); insertLines(payment);
        return 1;
    }

    @Override
    @Transactional
    public int submit(Long id) {
        Payment payment = load(id);
        requireOwner(payment);
        if (!(STATUS_DRAFT.equals(payment.getStatus()) || STATUS_RETURNED.equals(payment.getStatus()))) throw new ServiceException("当前状态不允许提交付款单");
        long amount = sumLines(payment.getLines());
        if (amount <= 0) throw new ServiceException("付款金额必须大于 0");
        if (mapper.selectSupplierApproved(payment.getSupplierId()) == 0) throw new ServiceException("收款供应商不存在或未准入");
        Set<Long> seen = new HashSet<>();
        for (PaymentLine line : payment.getLines()) {
            if (line == null || line.getInvoiceId() == null || line.getAllocatedCents() == null || line.getAllocatedCents() <= 0) throw new ServiceException("核销明细不完整：发票与金额不能为空且须为正");
            if (!seen.add(line.getInvoiceId())) throw new ServiceException("同一发票只能核销一次");
            Long invoiceSupplier = mapper.selectInvoiceSupplierId(line.getInvoiceId());
            if (invoiceSupplier == null || !invoiceSupplier.equals(payment.getSupplierId())) throw new ServiceException("发票与收款供应商不匹配");
            Long invoiceTotal = mapper.lockApprovedInvoice(line.getInvoiceId());
            if (invoiceTotal == null) throw new ServiceException("核销发票必须已通过且未作废");
            long allocated = mapper.selectAllocatedForInvoice(line.getInvoiceId(), id);
            long residual = invoiceTotal - allocated;
            if (residual < line.getAllocatedCents()) throw new ServiceException("发票应付余额不足(可核销 " + residual / 100.0 + " 元)");
        }
        String exceeded = amount > THRESHOLD_CENTS ? "1" : "0";
        Date now = DateUtils.getNowDate();
        if (mapper.updateState(id, payment.getRevision(), STATUS_IN_REVIEW, NODE_REVIEW, null, null, now) == 0) throw new ServiceException("付款单已变化，请刷新后重试");
        return 1;
    }

    @Override
    @Transactional
    public int review(Long id, PaymentDecision decision) {
        requireDecision(decision);
        Payment payment = load(id);
        Long userId = SecurityUtils.getUserId();
        if (!STATUS_IN_REVIEW.equals(payment.getStatus()) || !NODE_REVIEW.equals(payment.getCurrentNode())) throw new ServiceException("该付款单不在财务复核节点");
        if (userId.equals(payment.getCreatorId())) throw new ServiceException("申请人不能复核本人付款单");
        if (mapper.countReviewEligible(userId, payment.getCreatorId()) == 0) throw new ServiceException("当前用户不是有效财务复核人");
        Date now = DateUtils.getNowDate();
        String comment = StringUtils.defaultString(decision.getComment()).trim();
        if (comment.length() > 500) throw new ServiceException("复核意见不能超过 500 字");
        if (Boolean.TRUE.equals(decision.getApproved())) {
            if ("1".equals(payment.getThresholdExceeded())) {
                if (mapper.updateState(id, payment.getRevision(), STATUS_IN_REVIEW, NODE_DIRECTOR, comment, null, now) == 0) throw new ServiceException("付款单已变化，请刷新后重试");
            } else {
                if (mapper.updateState(id, payment.getRevision(), STATUS_APPROVED, null, comment, null, now) == 0) throw new ServiceException("付款单已变化，请刷新后重试");
            }
        } else {
            if (mapper.updateState(id, payment.getRevision(), STATUS_RETURNED, null, comment, null, now) == 0) throw new ServiceException("付款单已变化，请刷新后重试");
        }
        return 1;
    }

    @Override
    @Transactional
    public int directorDecision(Long id, PaymentDecision decision) {
        requireDecision(decision);
        Payment payment = load(id);
        Long userId = SecurityUtils.getUserId();
        if (!STATUS_IN_REVIEW.equals(payment.getStatus()) || !NODE_DIRECTOR.equals(payment.getCurrentNode())) throw new ServiceException("该付款单不在财务总监终审节点");
        if (userId.equals(payment.getCreatorId())) throw new ServiceException("申请人不能终审本人付款单");
        if (mapper.countDirectorEligible(userId, payment.getCreatorId()) == 0) throw new ServiceException("当前用户不是财务总监(需 finance 角色且职级 EXEC)");
        String comment = StringUtils.defaultString(decision.getComment()).trim();
        if (comment.length() > 500) throw new ServiceException("终审意见不能超过 500 字");
        Date now = DateUtils.getNowDate();
        String status = Boolean.TRUE.equals(decision.getApproved()) ? STATUS_APPROVED : STATUS_RETURNED;
        if (mapper.updateState(id, payment.getRevision(), status, null, null, comment, now) == 0) throw new ServiceException("付款单已变化，请刷新后重试");
        return 1;
    }

    @Override
    @Transactional
    public int voidPayment(Long id, String reason) {
        if (StringUtils.isEmpty(reason) || reason.trim().isEmpty()) throw new ServiceException("作废原因不能为空");
        Payment payment = load(id);
        boolean draftOrReturned = STATUS_DRAFT.equals(payment.getStatus()) || STATUS_RETURNED.equals(payment.getStatus());
        if (!draftOrReturned && !STATUS_IN_REVIEW.equals(payment.getStatus())) throw new ServiceException("仅草稿、退回或审批中的付款单可作废");
        if (!SecurityUtils.isAdmin() && !SecurityUtils.getUserId().equals(payment.getCreatorId())) throw new ServiceException("仅创建人或管理员可以作废");
        if (mapper.updateState(id, payment.getRevision(), STATUS_VOID, null, null, null, DateUtils.getNowDate()) == 0) throw new ServiceException("付款单已变化，请刷新后重试");
        return 1;
    }

    @Override
    public List<ApBalanceRow> apBalance(Long supplierId) {
        return mapper.selectApBalance(supplierId);
    }

    // ---- helpers ----

    private Payment load(Long id) {
        if (id == null) throw new ServiceException("缺少付款单 ID");
        Payment payment = mapper.selectById(id);
        if (payment == null) throw new ServiceException("付款单不存在");
        return payment;
    }

    private void requireOwner(Payment payment) {
        if (!SecurityUtils.isAdmin() && !SecurityUtils.getUserId().equals(payment.getCreatorId())) throw new ServiceException("仅创建人可以操作该付款单");
    }

    private void requireVisible(Payment payment) {
        Long userId = SecurityUtils.getUserId();
        if (SecurityUtils.isAdmin() || userId.equals(payment.getCreatorId()) || isFinanceUser(userId)) return;
        if (MODE_ALL.equals(scopeResolver.current().mode())) return;
        throw new ServiceException("无权访问该付款单");
    }

    private void requireDecision(PaymentDecision decision) {
        if (decision == null || decision.getApproved() == null) throw new ServiceException("审批动作不能为空");
    }

    private boolean isFinanceUser(Long userId) {
        var login = SecurityUtils.getLoginUser();
        if (login == null || login.getUser() == null || login.getUser().getRoles() == null) return false;
        return login.getUser().getRoles().stream().anyMatch(role -> "finance".equals(role.getRoleKey()));
    }

    private long sumLines(List<PaymentLine> lines) {
        if (lines == null || lines.isEmpty()) return 0L;
        long sum = 0;
        for (PaymentLine line : lines) {
            if (line.getAllocatedCents() == null) throw new ServiceException("核销金额不能为空");
            sum += line.getAllocatedCents();
        }
        return sum;
    }

    private void fillLines(Payment payment) {
        List<PaymentLine> stored = new ArrayList<>();
        if (payment.getLines() != null) {
            for (PaymentLine line : payment.getLines()) {
                if (line == null || line.getInvoiceId() == null) continue;
                line.setId(IdUtils.nextLongId());
                line.setPaymentId(payment.getId());
                stored.add(line);
            }
        }
        payment.setLines(stored);
    }

    private void insertLines(Payment payment) {
        for (PaymentLine line : payment.getLines()) mapper.insertPaymentLine(line);
    }

    private String nextNumber(Date date) {
        java.util.Date business = java.util.Date.from(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
        mapper.insertSequence(DOC_TYPE, business);
        mapper.incrementSequence(DOC_TYPE, business);
        Integer next = mapper.selectSequence(DOC_TYPE, business);
        return String.format("PAY-%tY%<tm%<td-%05d", business, next - 1);
    }
}
