package com.allinone.supply.service.impl;

import com.allinone.common.exception.ServiceException;
import com.allinone.common.utils.DateUtils;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.StringUtils;
import com.allinone.common.utils.uuid.IdUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.allinone.supply.domain.PurchaseOrder;
import com.allinone.supply.domain.PurchaseOrderLine;
import com.allinone.supply.domain.SupplierOption;
import com.allinone.supply.domain.WorkflowConfig;
import com.allinone.supply.mapper.PurchaseOrderMapper;
import com.allinone.supply.service.IPurchaseOrderService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseOrderServiceImpl implements IPurchaseOrderService {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DOCUMENT_TYPE = "PURCHASE_ORDER";
    private static final String FLOW_TYPE = "PURCHASE_ORDER_APPROVAL";

    @Autowired private PurchaseOrderMapper mapper;

    @Override public List<PurchaseOrder> selectPurchaseOrderList(PurchaseOrder order) {
        if (!SecurityUtils.isAdmin()) order.getParams().put("currentUserId", SecurityUtils.getUserId());
        order.getParams().put("supplyGlobal", SecurityUtils.isAdmin() || hasAnyRole("supervisor", "purchasing_supervisor", "purchase_supervisor", "finance"));
        return mapper.selectPurchaseOrderList(order);
    }
    @Override public List<SupplierOption> selectApprovedSupplierOptions() {
        return mapper.selectApprovedSupplierOptions();
    }
    @Override public PurchaseOrder selectPurchaseOrderById(Long id) {
        PurchaseOrder order = mapper.selectPurchaseOrderById(id);
        requireVisible(order);
        order.setLines(mapper.selectLinesByOrderId(id));
        return order;
    }
    @Override @Transactional public int insertPurchaseOrder(PurchaseOrder order) {
        validate(order);
        if (mapper.selectApprovedSupplierCount(order.getSupplierId()) == 0) throw new ServiceException("供应商尚未准入或已作废");
        if (mapper.selectEligibleBuyerCount(order.getBuyerId()) == 0) throw new ServiceException("指定采购员不存在、已停用或不是采购角色");
        Date now = DateUtils.getNowDate();
        order.setDocumentId(IdUtils.nextLongId()); order.setCreatorId(SecurityUtils.getUserId());
        order.setCreationKey(StringUtils.isEmpty(order.getCreationKey()) ? IdUtils.fastSimpleUUID() : order.getCreationKey());
        order.setNumber(nextNumber(now)); order.setCurrency("CNY"); order.setApprovalStatus("DRAFT");
        order.setCurrentVersion(0); order.setRevision(0); order.setCreateBy(SecurityUtils.getUsername()); order.setCreateTime(now); order.setUpdateTime(now);
        calculate(order);
        mapper.insertDocument(order); mapper.insertOrder(order); insertLines(order);
        return 1;
    }
    @Override @Transactional public int updatePurchaseOrder(PurchaseOrder order) {
        PurchaseOrder old = mapper.selectPurchaseOrderById(order.getDocumentId()); requireOwner(old);
        if (!("DRAFT".equals(old.getApprovalStatus()) || "RETURNED".equals(old.getApprovalStatus()))) throw new ServiceException("当前状态不允许编辑订单");
        validate(order); if (mapper.selectApprovedSupplierCount(order.getSupplierId()) == 0) throw new ServiceException("供应商尚未准入或已作废");
        if (mapper.selectEligibleBuyerCount(order.getBuyerId()) == 0) throw new ServiceException("指定采购员不存在、已停用或不是采购角色");
        calculate(order); order.setUpdateTime(DateUtils.getNowDate());
        if (mapper.updateOrder(order) == 0) throw new ServiceException("订单已被其他用户修改，请刷新后重试");
        mapper.deleteLines(order.getDocumentId()); insertLines(order); return 1;
    }
    @Override @Transactional public int submitPurchaseOrder(Long id) {
        PurchaseOrder order = selectPurchaseOrderById(id); requireOwner(order); validate(order);
        if (!("DRAFT".equals(order.getApprovalStatus()) || "RETURNED".equals(order.getApprovalStatus()))) throw new ServiceException("当前状态不允许提交订单");
        if (mapper.selectApprovedSupplierCount(order.getSupplierId()) == 0) throw new ServiceException("供应商尚未准入或已作废");
        if (mapper.selectEligibleBuyerCount(order.getBuyerId()) == 0) throw new ServiceException("指定采购员不存在、已停用或不是采购角色");
        WorkflowConfig config = mapper.selectLatestConfig(FLOW_TYPE); Long assignee = firstEligibleSupervisor(config == null ? null : config.getSupervisorCandidates());
        if (assignee == null) throw new ServiceException("未配置合法的采购主管审批人，请先在系统设置中配置");
        try {
            Date now = DateUtils.getNowDate(); int version = order.getCurrentVersion() + 1; Long versionId = IdUtils.nextLongId();
            String snapshot = MAPPER.writeValueAsString(order); mapper.insertVersion(versionId, id, version, snapshot, sha256(snapshot), SecurityUtils.getUserId(), now);
            mapper.insertVersionAttachments(versionId, id);
            Long workflowId = IdUtils.nextLongId(); mapper.insertWorkflow(workflowId, id, versionId, config.getVersion(), now); mapper.insertTask(IdUtils.nextLongId(), workflowId, assignee);
            order.setCurrentVersion(version); order.setCurrentNode("SUPERVISOR"); order.setApprovalStatus("IN_REVIEW"); order.setUpdateTime(now);
            if (mapper.updateDocument(order) == 0) throw new ServiceException("订单状态已变化，请刷新后重试");
            mapper.insertAudit(IdUtils.nextLongId(), id, versionId, SecurityUtils.getUserId(), SecurityUtils.getUsername(), "SUBMIT", now); return 1;
        } catch (ServiceException e) { throw e; } catch (Exception e) { throw new ServiceException("订单提交失败"); }
    }
    private void validate(PurchaseOrder order) {
        if (order == null || order.getSupplierId() == null || order.getBuyerId() == null || order.getLines() == null || order.getLines().isEmpty()) throw new ServiceException("供应商、采购员和订单明细不能为空");
        for (PurchaseOrderLine line : order.getLines()) if (StringUtils.isEmpty(line.getName()) || StringUtils.isEmpty(line.getUnit()) || line.getQuantity() == null || line.getPrice() == null || line.getRate() == null || line.getQuantity().signum() <= 0 || line.getPrice().signum() <= 0 || line.getRate().signum() < 0 || line.getQuantity().compareTo(BigDecimal.valueOf(100_000_000L)) >= 0 || line.getPrice().compareTo(BigDecimal.valueOf(100_000_000L)) >= 0 || line.getRate().compareTo(BigDecimal.ONE) > 0 || line.getQuantity().scale() > 4 || line.getPrice().scale() > 6 || line.getRate().scale() > 4) throw new ServiceException("订单明细数值必须合法：数量和单价小于1亿、税率为0到1且精度符合要求");
    }
    private void calculate(PurchaseOrder order) {
        long amount = 0, tax = 0; int no = 1;
        for (PurchaseOrderLine line : order.getLines()) {
            line.setOrderId(order.getDocumentId()); line.setId(IdUtils.nextLongId()); line.setLineNo(no++);
            long lineAmount = line.getQuantity().multiply(line.getPrice()).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
            long lineTax = BigDecimal.valueOf(lineAmount).multiply(line.getRate()).setScale(0, RoundingMode.HALF_UP).longValueExact();
            line.setAmountCents(lineAmount); line.setTaxCents(lineTax); line.setTotalCents(lineAmount + lineTax); amount += lineAmount; tax += lineTax;
        }
        order.setAmountCents(amount); order.setTaxCents(tax); order.setTotalCents(amount + tax);
    }
    private void insertLines(PurchaseOrder order) { for (PurchaseOrderLine line : order.getLines()) mapper.insertLine(line); }
    private String nextNumber(Date date) { mapper.insertSequence(DOCUMENT_TYPE, date); mapper.incrementSequence(DOCUMENT_TYPE, date); return String.format("PO-%tY%<tm%<td-%05d", date, mapper.selectSequence(DOCUMENT_TYPE, date) - 1); }
    private Long first(String value) { if (StringUtils.isEmpty(value)) return null; try { return Long.valueOf(value.split(",")[0].trim()); } catch (Exception e) { return null; } }
    private Long firstEligibleSupervisor(String value) { if (StringUtils.isEmpty(value)) return null; for (String candidate : value.split(",")) { try { Long userId=Long.valueOf(candidate.trim()); if (mapper.selectEligibleSupervisorCount(userId, SecurityUtils.getUserId()) > 0) return userId; } catch (NumberFormatException ignored) { } } return null; }
    private void requireOwner(PurchaseOrder order) { if (order == null) throw new ServiceException("采购订单不存在"); if (!SecurityUtils.isAdmin() && !SecurityUtils.getUserId().equals(order.getCreatorId())) throw new ServiceException("无权访问该采购订单"); }
    private void requireVisible(PurchaseOrder order) { if (order == null) throw new ServiceException("采购订单不存在"); if (SecurityUtils.isAdmin() || hasAnyRole("supervisor", "purchasing_supervisor", "purchase_supervisor", "finance")) return; if (!SecurityUtils.getUserId().equals(order.getCreatorId()) && !SecurityUtils.getUserId().equals(order.getBuyerId())) throw new ServiceException("无权访问该采购订单"); }
    private boolean hasAnyRole(String... roleKeys) { if (SecurityUtils.getLoginUser() == null || SecurityUtils.getLoginUser().getUser().getRoles() == null) return false; return SecurityUtils.getLoginUser().getUser().getRoles().stream().anyMatch(role -> java.util.Arrays.asList(roleKeys).contains(role.getRoleKey())); }
    private String sha256(String value) throws Exception { byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); StringBuilder s = new StringBuilder(); for (byte b : bytes) s.append(String.format("%02x", b)); return s.toString(); }
}
