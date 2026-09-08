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
import com.allinone.supply.domain.PaymentEvent;
import com.allinone.supply.domain.PaymentExecution;
import com.allinone.supply.domain.SupplierOption;
import com.allinone.supply.mapper.PaymentMapper;
import com.allinone.supply.service.IPaymentService;
import com.allinone.supply.support.SupplyDataScopeResolver;
import static com.allinone.supply.support.SupplyDataScopeResolver.MODE_ALL;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Comparator;
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
    private static final String STATUS_READY = "READY_TO_PAY";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_VOID = "VOID";
    private static final String NODE_REVIEW = "FINANCE_REVIEW";
    private static final String NODE_DIRECTOR = "FINANCE_DIRECTOR";
    /** 10 万元(分)。 */
    private static final long THRESHOLD_CENTS = 10_000_000L;

    @Autowired private PaymentMapper mapper;
    @Autowired private SupplyDataScopeResolver scopeResolver;
    @Autowired private SupplierChangeService supplierChanges;
    @Autowired private com.allinone.supply.mapper.SupplierChangeMapper supplierChangeMapper;

    @Override
    public List<SupplierOption> supplierOptions() {
        return mapper.selectApprovedSupplierOptions();
    }

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
        payment.setEvents(mapper.selectEvents(id));
        payment.setAccountSnapshot(supplierChangeMapper.accountSnapshot(id));
        payment.setAccountChangePending(supplierChangeMapper.blocked(payment.getSupplierId())>0);
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
        payment.setThresholdExceeded(payment.getAmountCents() > THRESHOLD_CENTS ? "1" : "0");
        payment.setCreateTime(now); payment.setUpdateTime(now);
        fillLines(payment);
        if (mapper.insertPayment(payment) != 1) throw new ServiceException("付款单保存失败");
        insertLines(payment);
        recordEvent(payment, "CREATE", payment.getRemark(), payment);
        return 1;
    }

    @Override
    @Transactional
    public int update(Payment payment) {
        if (payment == null || payment.getId() == null || payment.getRevision() == null) throw new ServiceException("缺少付款单 ID 或版本号");
        Payment old = load(payment.getId());
        requireOwner(old);
        if (!(STATUS_DRAFT.equals(old.getStatus()) || STATUS_RETURNED.equals(old.getStatus()))) throw new ServiceException("当前状态不允许编辑付款单");
        payment.setSupplierId(payment.getSupplierId() != null ? payment.getSupplierId() : old.getSupplierId());
        payment.setAmountCents(sumLines(payment.getLines()));
        payment.setThresholdExceeded(payment.getAmountCents() > THRESHOLD_CENTS ? "1" : "0");
        payment.setUpdateTime(DateUtils.getNowDate());
        if (mapper.updatePayment(payment) == 0) throw new ServiceException("付款单已变化，请刷新后重试");
        mapper.deleteLines(payment.getId()); fillLines(payment); insertLines(payment);
        recordEvent(payment, "UPDATE", payment.getRemark(), payment);
        return 1;
    }

    @Override
    @Transactional
    public int submit(Long id) {
        Payment payment = load(id);
        requireOwner(payment);
        if (!(STATUS_DRAFT.equals(payment.getStatus()) || STATUS_RETURNED.equals(payment.getStatus()))) throw new ServiceException("当前状态不允许提交付款单");
        payment.setLines(mapper.selectLines(id));
        long amount = sumLines(payment.getLines());
        if (amount <= 0) throw new ServiceException("付款金额必须大于 0");
        if (mapper.selectSupplierApproved(payment.getSupplierId()) == 0) throw new ServiceException("收款供应商不存在或未准入");
        Set<Long> seen = new HashSet<>();
        // 同一组发票始终按 ID 顺序加锁，避免多张发票反向申请导致死锁。
        List<PaymentLine> orderedLines = new ArrayList<>(payment.getLines());
        orderedLines.sort(Comparator.comparing(PaymentLine::getInvoiceId));
        for (PaymentLine line : orderedLines) {
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
        Date now = DateUtils.getNowDate();
        payment.setAmountCents(amount);
        supplierChanges.captureAccount(payment);
        payment.setThresholdExceeded(amount > THRESHOLD_CENTS ? "1" : "0");
        payment.setUpdateTime(now);
        if (mapper.submitPayment(payment) == 0) throw new ServiceException("付款单已变化，请刷新后重试");
        recordEvent(payment, "SUBMIT", "提交财务复核", payment);
        return 1;
    }

    @Override
    @Transactional
    public int review(Long id, PaymentDecision decision) {
        requireDecision(decision);
        Payment payment = load(id);
        checkRevision(payment, decision.getRevision());
        Long userId = SecurityUtils.getUserId();
        if (!STATUS_IN_REVIEW.equals(payment.getStatus()) || !NODE_REVIEW.equals(payment.getCurrentNode())) throw new ServiceException("该付款单不在财务复核节点");
        if (userId.equals(payment.getCreatorId())) throw new ServiceException("申请人不能复核本人付款单");
        if (mapper.countReviewEligible(userId, payment.getCreatorId()) == 0) throw new ServiceException("当前用户不是有效财务复核人");
        Date now = DateUtils.getNowDate();
        String comment = StringUtils.defaultString(decision.getComment()).trim();
        if (comment.length() > 500) throw new ServiceException("复核意见不能超过 500 字");
        if (Boolean.TRUE.equals(decision.getApproved())) {
            supplierChanges.captureAccount(payment);
            if (payment.getAmountCents() > THRESHOLD_CENTS) {
                if (mapper.updateState(id, payment.getRevision(), STATUS_IN_REVIEW, NODE_DIRECTOR, comment, null, now) == 0) throw new ServiceException("付款单已变化，请刷新后重试");
            } else {
                if (mapper.updateState(id, payment.getRevision(), STATUS_READY, null, comment, null, now) == 0) throw new ServiceException("付款单已变化，请刷新后重试");
            }
        } else {
            if (mapper.updateState(id, payment.getRevision(), STATUS_RETURNED, null, comment, null, now) == 0) throw new ServiceException("付款单已变化，请刷新后重试");
        }
        recordEvent(payment, Boolean.TRUE.equals(decision.getApproved()) ? "REVIEW_APPROVE" : "REVIEW_RETURN", comment, payment);
        return 1;
    }

    @Override
    @Transactional
    public int directorDecision(Long id, PaymentDecision decision) {
        requireDecision(decision);
        Payment payment = load(id);
        checkRevision(payment, decision.getRevision());
        Long userId = SecurityUtils.getUserId();
        if (!STATUS_IN_REVIEW.equals(payment.getStatus()) || !NODE_DIRECTOR.equals(payment.getCurrentNode())) throw new ServiceException("该付款单不在财务总监终审节点");
        if (userId.equals(payment.getCreatorId())) throw new ServiceException("申请人不能终审本人付款单");
        if (mapper.countDirectorEligible(userId, payment.getCreatorId()) == 0) throw new ServiceException("当前用户不是财务总监(需 finance 角色且职级 EXEC)");
        String comment = StringUtils.defaultString(decision.getComment()).trim();
        if (comment.length() > 500) throw new ServiceException("终审意见不能超过 500 字");
        Date now = DateUtils.getNowDate();
        String status = Boolean.TRUE.equals(decision.getApproved()) ? STATUS_READY : STATUS_RETURNED;
        if (mapper.updateState(id, payment.getRevision(), status, null, null, comment, now) == 0) throw new ServiceException("付款单已变化，请刷新后重试");
        recordEvent(payment, Boolean.TRUE.equals(decision.getApproved()) ? "DIRECTOR_APPROVE" : "DIRECTOR_RETURN", comment, payment);
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
        recordEvent(payment, "VOID", reason.trim(), payment);
        return 1;
    }

    @Override
    public List<ApBalanceRow> apBalance(Long supplierId) {
        Payment filter = new Payment(); filter.setSupplierId(supplierId);
        applyScope(filter);
        return mapper.selectApBalance(filter);
    }

    @Override @Transactional
    public int recordExecution(Long id, PaymentExecution execution) {
        Payment payment = load(id);
        supplierChanges.requireExecutable(payment);
        if (!isFinanceUser(SecurityUtils.getUserId())) throw new ServiceException("仅财务人员可以记录实际付款");
        if (execution == null) throw new ServiceException("缺少支付凭据");
        checkRevision(payment, execution.getRevision());
        if (!STATUS_READY.equals(payment.getStatus())) throw new ServiceException("仅审批通过且待付款的单据可以记录支付");
        if (execution.getPaidAt() == null || execution.getPaidAt().after(DateUtils.getNowDate())
                || StringUtils.isBlank(execution.getReference()) || execution.getReference().length() > 120
                || StringUtils.isBlank(execution.getPayerAccount()) || execution.getPayerAccount().length() > 120)
            throw new ServiceException("请填写合法的支付日期、付款账户和支付凭据编号");
        if (mapper.updateState(id, payment.getRevision(), STATUS_PAID, null, null, null, DateUtils.getNowDate()) != 1)
            throw new ServiceException("付款单已变化，请刷新后重试");
        recordEvent(payment, "PAID", execution.getReference().trim(), execution);
        return 1;
    }

    private void applyScope(Payment filter) {
        scopeResolver.putInto(filter.getParams());
        if (isFinanceUser(SecurityUtils.getUserId())) filter.getParams().put("supplyScopeMode", MODE_ALL);
    }

    private void checkRevision(Payment payment, Integer revision) {
        if (revision == null || !revision.equals(payment.getRevision())) throw new ServiceException("付款单已变化，请刷新详情后重试");
    }

    private void recordEvent(Payment payment, String action, String comment, Object snapshot) {
        if (comment != null && comment.length() > 500) throw new ServiceException("说明不能超过 500 字");
        PaymentEvent event = new PaymentEvent(); event.setId(IdUtils.nextLongId()); event.setPaymentId(payment.getId());
        event.setActorId(SecurityUtils.getUserId()); event.setActorName(SecurityUtils.getUsername());
        event.setAction(action); event.setComment(comment); event.setCreatedAt(DateUtils.getNowDate());
        try { event.setSnapshot(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(snapshot)); }
        catch (Exception e) { throw new ServiceException("付款审计快照生成失败"); }
        if (mapper.insertEvent(event) != 1) throw new ServiceException("付款审计记录保存失败");
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
        Payment filter = new Payment(); applyScope(filter); filter.setId(payment.getId());
        if (!mapper.selectList(filter).isEmpty()) return;
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
        if (lines == null || lines.isEmpty()) throw new ServiceException("请至少填写一行核销明细");
        long sum = 0;
        Set<Long> invoices = new HashSet<>();
        for (PaymentLine line : lines) {
            if (line == null || line.getInvoiceId() == null || line.getAllocatedCents() == null || line.getAllocatedCents() <= 0)
                throw new ServiceException("核销发票不能为空且金额必须大于 0");
            if (!invoices.add(line.getInvoiceId())) throw new ServiceException("同一发票只能填写一行核销明细");
            try { sum = Math.addExact(sum, line.getAllocatedCents()); }
            catch (ArithmeticException e) { throw new ServiceException("付款金额超出允许范围"); }
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
        for (PaymentLine line : payment.getLines()) {
            if (mapper.insertPaymentLine(line) != 1) throw new ServiceException("付款核销明细保存失败");
        }
    }

    private String nextNumber(Date date) {
        java.util.Date business = java.util.Date.from(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
        mapper.insertSequence(DOC_TYPE, business);
        mapper.incrementSequence(DOC_TYPE, business);
        Integer next = mapper.selectSequence(DOC_TYPE, business);
        return String.format("PAY-%tY%<tm%<td-%05d", business, next - 1);
    }
}
