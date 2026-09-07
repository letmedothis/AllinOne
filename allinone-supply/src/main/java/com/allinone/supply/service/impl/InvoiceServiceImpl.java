package com.allinone.supply.service.impl;

import com.allinone.common.exception.ServiceException;
import com.allinone.common.utils.DateUtils;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.StringUtils;
import com.allinone.common.utils.uuid.IdUtils;
import com.allinone.supply.domain.AllocationTarget;
import com.allinone.supply.domain.Invoice;
import com.allinone.supply.domain.InvoiceLine;
import com.allinone.supply.domain.PurchaseOrderLine;
import com.allinone.supply.domain.WorkflowConfig;
import com.allinone.supply.mapper.InvoiceMapper;
import com.allinone.supply.service.IInvoiceService;
import com.allinone.supply.support.SupplyDataScopeResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceServiceImpl implements IInvoiceService {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private InvoiceMapper mapper;
    @Autowired
    private SupplyDataScopeResolver scopeResolver;

    @Override
    public List<Invoice> list(String invoiceNumber, String approvalStatus) {
        SupplyDataScopeResolver.Scope scope = scopeResolver.current();
        return mapper.selectList(scope.mode(), scope.userId(), scope.deptId(), invoiceNumber, approvalStatus);
    }

    @Override
    @Transactional
    public int createDraft(Invoice invoice) {
        validate(invoice);
        validateOrderAndLines(invoice);
        if (mapper.selectIdentityCount(normalize(invoice.getSellerTaxId()), normalize(invoice.getInvoiceNumber())) > 0) {
            throw new ServiceException("销方税号与发票号码已登记");
        }
        calculate(invoice);
        Date now = DateUtils.getNowDate();
        Long userId = SecurityUtils.getUserId();
        invoice.setDocumentId(IdUtils.nextLongId());
        invoice.setNumber(String.format("INV-%tY%<tm%<td-%s", now, IdUtils.fastSimpleUUID().substring(0, 8)));
        invoice.setCreatorId(userId);
        invoice.setApprovalStatus("DRAFT");
        invoice.setRevision(0);
        invoice.setCurrentVersion(0);
        invoice.setCreateBy(SecurityUtils.getUsername());
        invoice.setCreateTime(now);
        mapper.insertDocument(invoice);
        mapper.insertInvoice(invoice);
        insertCurrentLines(invoice);
        mapper.insertIdentity(invoice);
        return 1;
    }

    @Override
    @Transactional
    public int updateDraft(Invoice invoice) {
        validate(invoice);
        Invoice saved = mapper.selectDraft(invoice.getDocumentId());
        requireOwner(saved, "发票不存在、不可编辑或无权操作");
        requireRevision(invoice.getRevision(), saved.getRevision());
        if (!saved.getOrderId().equals(invoice.getOrderId())
                || !saved.getInvoiceNumber().equals(normalize(invoice.getInvoiceNumber()))
                || !saved.getSellerTaxId().equalsIgnoreCase(normalize(invoice.getSellerTaxId()))) {
            throw new ServiceException("发票订单、号码和销方税号不可修改");
        }
        validateOrderAndLines(invoice);
        calculate(invoice);
        invoice.setDocumentId(saved.getDocumentId());
        invoice.setCreatorId(saved.getCreatorId());
        invoice.setRevision(saved.getRevision());
        int rows = mapper.updateInvoice(invoice);
        if (rows == 0) throw new ServiceException("发票已变化，请刷新后重试");
        mapper.archiveLines(invoice.getDocumentId());
        insertCurrentLines(invoice);
        return rows;
    }

    @Override
    @Transactional
    public int confirm(Invoice invoice) {
        if (invoice == null || invoice.getDocumentId() == null) throw new ServiceException("缺少发票ID");
        Invoice saved = mapper.selectDraft(invoice.getDocumentId());
        requireOwner(saved, "发票不存在、不可确认或无权操作");
        if (invoice.getRevision() != null) requireRevision(invoice.getRevision(), saved.getRevision());
        saved.setLines(mapper.selectLines(saved.getDocumentId()));
        String contentHash = contentHash(saved);
        int rows = mapper.confirmDraft(saved.getDocumentId(), saved.getRevision(), contentHash, SecurityUtils.getUserId());
        if (rows == 0) throw new ServiceException("发票已变化，请刷新后重试");
        return rows;
    }

    @Override
    @Transactional
    public int submit(Invoice invoice) {
        if (invoice == null || invoice.getDocumentId() == null) throw new ServiceException("缺少发票ID");
        Invoice saved = mapper.selectDraft(invoice.getDocumentId());
        requireOwner(saved, "发票不存在、已提交或无权操作");
        if (invoice.getRevision() != null) requireRevision(invoice.getRevision(), saved.getRevision());
        saved.setLines(mapper.selectLines(saved.getDocumentId()));
        validate(saved);
        validateOrderAndLines(saved);
        if (mapper.selectAttachmentCount(saved.getDocumentId()) == 0) {
            throw new ServiceException("提交发票前必须上传至少一个票据附件");
        }
        if (!Boolean.TRUE.equals(saved.getManualConfirmed())
                || !contentHash(saved).equals(saved.getConfirmedContentHash())) {
            throw new ServiceException("请先确认当前发票内容，修改后需重新确认");
        }
        WorkflowConfig config = mapper.selectLatestConfig("INVOICE_APPROVAL");
        Long purchase = mapper.selectOrderBuyerId(saved.getOrderId());
        Long finance = firstEligibleFinance(config == null ? null : config.getFinanceCandidates(), purchase);
        if (config == null || purchase == null || mapper.selectEligibleBuyerCount(purchase) == 0 || finance == null || purchase.equals(finance)
                || purchase.equals(SecurityUtils.getUserId()) || finance.equals(SecurityUtils.getUserId())) {
            throw new ServiceException("未配置合法的采购和财务审批人");
        }
        try {
            Date now = DateUtils.getNowDate();
            int versionNo = saved.getCurrentVersion() == null ? 1 : saved.getCurrentVersion() + 1;
            Long versionId = IdUtils.nextLongId();
            for (InvoiceLine line : saved.getLines()) allocate(saved, line, versionId);
            String snapshot = JSON.writeValueAsString(saved);
            mapper.insertVersion(versionId, saved.getDocumentId(), versionNo, snapshot, sha256(snapshot), SecurityUtils.getUserId(), now);
            mapper.insertVersionAttachments(versionId, saved.getDocumentId());
            Long workflowId = IdUtils.nextLongId();
            mapper.insertWorkflow(workflowId, saved.getDocumentId(), versionId, config.getVersion(), now);
            mapper.insertTask(IdUtils.nextLongId(), workflowId, "PURCHASE", 1, purchase);
            saved.setCurrentVersion(versionNo);
            saved.setApprovalStatus("IN_REVIEW");
            saved.setCurrentNode("PURCHASE");
            int result = mapper.updateDocument(saved);
            if (result == 0) throw new ServiceException("发票状态已变化，请刷新后重试");
            mapper.insertAudit(IdUtils.nextLongId(), saved.getDocumentId(), versionId, SecurityUtils.getUserId(),
                    SecurityUtils.getUsername(), "SUBMIT", now);
            return result;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("发票提交失败");
        }
    }

    private void validateOrderAndLines(Invoice invoice) {
        if (mapper.selectApprovedOrderCount(invoice.getOrderId()) == 0) {
            throw new ServiceException("订单未通过审批或已作废");
        }
        String supplierTax = mapper.selectSupplierTaxId(invoice.getOrderId());
        if (supplierTax == null || !normalize(invoice.getSellerTaxId()).equalsIgnoreCase(normalize(supplierTax))) {
            throw new ServiceException("销方税号与供应商不一致");
        }
        String companyTax = mapper.selectCompanyTaxId();
        if (companyTax != null && !normalize(invoice.getBuyerTaxId()).equalsIgnoreCase(normalize(companyTax))) {
            throw new ServiceException("购方税号与公司配置不一致");
        }
        boolean hasDifference = false;
        for (InvoiceLine line : invoice.getLines()) {
            if (mapper.selectOrderLineCount(invoice.getOrderId(), line.getOrderLineId()) == 0) {
                throw new ServiceException("发票行不属于关联订单");
            }
            PurchaseOrderLine orderLine = mapper.selectOrderLine(line.getOrderLineId());
            if (orderLine == null) throw new ServiceException("发票行关联的订单明细不存在");
            if (line.getUnit() == null || !line.getUnit().trim().equals(orderLine.getUnit())) {
                throw new ServiceException("发票单位与订单单位不一致");
            }
            if (line.getPrice().compareTo(orderLine.getPrice()) != 0 || line.getRate().compareTo(orderLine.getRate()) != 0) {
                hasDifference = true;
            }
        }
        if (hasDifference && (StringUtils.isEmpty(invoice.getDifferenceNote()) || invoice.getDifferenceNote().trim().isEmpty())) {
            throw new ServiceException("发票单价或税率与订单存在差异，必须填写差异说明");
        }
    }

    private void validate(Invoice invoice) {
        if (invoice == null || invoice.getOrderId() == null || StringUtils.isEmpty(invoice.getInvoiceNumber())
                || StringUtils.isEmpty(invoice.getInvoiceType()) || invoice.getIssueDate() == null
                || StringUtils.isEmpty(invoice.getSellerName()) || StringUtils.isEmpty(invoice.getSellerTaxId())
                || StringUtils.isEmpty(invoice.getBuyerName()) || StringUtils.isEmpty(invoice.getBuyerTaxId())
                || invoice.getLines() == null || invoice.getLines().isEmpty()) {
            throw new ServiceException("发票基本信息、开票日期、购销方信息和明细不能为空");
        }
        if (invoice.getInvoiceNumber().trim().length() > 32) throw new ServiceException("发票号码不能超过32个字符");
        for (InvoiceLine line : invoice.getLines()) {
            if (line.getOrderLineId() == null || StringUtils.isEmpty(line.getName()) || StringUtils.isEmpty(line.getUnit())
                    || line.getQuantity() == null || line.getQuantity().signum() <= 0 || line.getPrice() == null
                    || line.getPrice().signum() <= 0 || line.getRate() == null || line.getRate().signum() < 0
                    || line.getQuantity().compareTo(BigDecimal.valueOf(100_000_000L)) >= 0 || line.getPrice().compareTo(BigDecimal.valueOf(100_000_000L)) >= 0
                    || line.getRate().compareTo(BigDecimal.ONE) > 0 || line.getQuantity().scale() > 4 || line.getPrice().scale() > 6 || line.getRate().scale() > 4) {
                throw new ServiceException("发票明细数值必须合法：数量和单价小于1亿、税率为0到1且精度符合要求");
            }
        }
    }

    private void calculate(Invoice invoice) {
        Long suppliedAmount = invoice.getAmountCents();
        Long suppliedTax = invoice.getTaxCents();
        Long suppliedTotal = invoice.getTotalCents();
        long amount = 0;
        long tax = 0;
        for (InvoiceLine line : invoice.getLines()) {
            long lineAmount = line.getQuantity().multiply(line.getPrice()).movePointRight(2)
                    .setScale(0, RoundingMode.HALF_UP).longValueExact();
            long lineTax = BigDecimal.valueOf(lineAmount).multiply(line.getRate())
                    .setScale(0, RoundingMode.HALF_UP).longValueExact();
            line.setAmountCents(lineAmount);
            line.setTaxCents(lineTax);
            line.setTotalCents(lineAmount + lineTax);
            amount = Math.addExact(amount, lineAmount);
            tax = Math.addExact(tax, lineTax);
        }
        invoice.setAmountCents(amount);
        invoice.setTaxCents(tax);
        invoice.setTotalCents(Math.addExact(amount, tax));
        if ((suppliedAmount != null && Math.abs(suppliedAmount - amount) > 1)
                || (suppliedTax != null && Math.abs(suppliedTax - tax) > 1)
                || (suppliedTotal != null && Math.abs(suppliedTotal - (amount + tax)) > 1)) {
            throw new ServiceException("发票金额与明细合计不一致，允许的舍入差额不得超过0.01元");
        }
    }

    private void insertCurrentLines(Invoice invoice) {
        int lineNo = 1;
        for (InvoiceLine line : invoice.getLines()) {
            line.setId(IdUtils.nextLongId());
            line.setInvoiceId(invoice.getDocumentId());
            line.setLineNo(lineNo++);
            mapper.insertLine(line);
        }
    }

    private void allocate(Invoice invoice, InvoiceLine line, Long versionId) {
        BigDecimal left = line.getQuantity();
        for (AllocationTarget target : mapper.selectAllocationTargets(line.getOrderLineId())) {
            if (left.signum() <= 0) break;
            BigDecimal use = left.min(target.getRemainingQuantity());
            if (use.signum() > 0) {
                mapper.insertAllocation(IdUtils.nextLongId(), invoice.getDocumentId(), versionId, line.getId(),
                        target.getReceiptLineId(), use);
                left = left.subtract(use);
            }
        }
        if (left.signum() > 0) throw new ServiceException("发票明细「" + line.getName() + "」超过可开票数量");
    }

    private String contentHash(Invoice invoice) {
        try {
            Map<String, Object> content = new LinkedHashMap<>();
            content.put("orderId", invoice.getOrderId());
            content.put("invoiceNumber", normalize(invoice.getInvoiceNumber()));
            content.put("invoiceType", invoice.getInvoiceType());
            content.put("issueDate", invoice.getIssueDate());
            content.put("sellerName", invoice.getSellerName());
            content.put("sellerTaxId", normalize(invoice.getSellerTaxId()));
            content.put("buyerName", invoice.getBuyerName());
            content.put("buyerTaxId", normalize(invoice.getBuyerTaxId()));
            content.put("differenceNote", invoice.getDifferenceNote());
            List<Map<String, Object>> lines = new ArrayList<>();
            for (InvoiceLine line : invoice.getLines()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("orderLineId", line.getOrderLineId());
                item.put("name", line.getName());
                item.put("unit", line.getUnit());
                item.put("quantity", line.getQuantity());
                item.put("price", line.getPrice());
                item.put("rate", line.getRate());
                item.put("amountCents", line.getAmountCents());
                item.put("taxCents", line.getTaxCents());
                item.put("totalCents", line.getTotalCents());
                lines.add(item);
            }
            content.put("lines", lines);
            return sha256(JSON.writeValueAsString(content));
        } catch (Exception e) {
            throw new ServiceException("发票内容摘要计算失败");
        }
    }

    private Long first(String value) {
        try { return StringUtils.isEmpty(value) ? null : Long.valueOf(value.split(",")[0].trim()); }
        catch (Exception e) { return null; }
    }

    private Long firstEligibleFinance(String value, Long purchaseId) {
        if (StringUtils.isEmpty(value) || purchaseId == null) return null;
        for (String candidate : value.split(",")) {
            try { Long userId=Long.valueOf(candidate.trim()); if (mapper.selectEligibleFinanceCount(userId, SecurityUtils.getUserId(), purchaseId) > 0) return userId; }
            catch (NumberFormatException ignored) { }
        }
        return null;
    }

    private String normalize(String value) { return value == null ? null : value.trim(); }

    private void requireOwner(Invoice invoice, String message) {
        if (invoice == null || (!SecurityUtils.isAdmin() && !SecurityUtils.getUserId().equals(invoice.getCreatorId()))) {
            throw new ServiceException(message);
        }
    }

    private void requireRevision(Integer actual, Integer expected) {
        if (actual == null || expected == null || !actual.equals(expected)) {
            throw new ServiceException("发票已被其他用户修改，请刷新后重试");
        }
    }

    private String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte item : digest) result.append(String.format("%02x", item));
        return result.toString();
    }
}
