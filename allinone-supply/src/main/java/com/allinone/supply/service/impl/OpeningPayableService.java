package com.allinone.supply.service.impl;

import com.allinone.common.exception.ServiceException;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.uuid.IdUtils;
import com.allinone.supply.domain.*;
import com.allinone.supply.mapper.*;
import com.allinone.supply.service.IInvoiceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpeningPayableService {
    @Autowired private OpeningPayableMapper opening;
    @Autowired private InvoiceMapper invoices;
    @Autowired private SupplierMapper suppliers;
    @Autowired private PaymentMapper payments;
    @Autowired private IInvoiceService invoiceService;

    public OpeningPayable get(Long id) { invoiceService.get(id); return opening.select(id); }
    public List<OpeningPayable> list() { return opening.list(SecurityUtils.getUserId()); }

    @Transactional public Invoice save(OpeningPayable request) {
        if (request.getOpeningDate().before(request.getIssueDate())) throw new ServiceException("期初日期不能早于原票日期");
        if (payments.selectSupplierApproved(request.getSupplierId()) != 1) throw new ServiceException("供应商尚未准入");
        Supplier supplier = suppliers.selectSupplierById(request.getSupplierId());
        var company = opening.company();
        if (company == null) throw new ServiceException("请先维护公司信息");
        Invoice invoice;
        Date now = new Date();
        boolean create = request.getDocumentId() == null;
        if (create) {
            if (invoices.selectIdentityCount(supplier.getTaxId(), request.getInvoiceNumber().trim()) > 0) throw new ServiceException("该供应商的原始票号已登记");
            invoice = new Invoice(); invoice.setDocumentId(IdUtils.nextLongId());
            invoice.setNumber("OPEN-" + IdUtils.fastSimpleUUID().substring(0,12));
            invoice.setCreatorId(SecurityUtils.getUserId()); invoice.setApprovalStatus("DRAFT"); invoice.setRevision(0); invoice.setCurrentVersion(0);
            invoice.setCreateTime(now); invoice.setCreateBy(SecurityUtils.getUsername());
        } else {
            invoice = invoices.selectDraft(request.getDocumentId());
            OpeningPayable old = opening.select(request.getDocumentId());
            requireOwner(invoice, request.getRevision());
            if (old == null || !old.getSupplierId().equals(request.getSupplierId()) || !old.getInvoiceNumber().equals(request.getInvoiceNumber().trim())) throw new ServiceException("期初来源的供应商和原始票号不可变更");
        }
        invoice.setInvoiceType("OPENING_AP"); invoice.setInvoiceNumber(request.getInvoiceNumber().trim()); invoice.setIssueDate(request.getIssueDate());
        invoice.setSellerName(supplier.getName()); invoice.setSellerTaxId(supplier.getTaxId());
        invoice.setBuyerName(String.valueOf(company.get("name"))); invoice.setBuyerTaxId(String.valueOf(company.get("taxId")));
        invoice.setAmountCents(request.getBalanceCents()); invoice.setTotalCents(request.getBalanceCents()); invoice.setTaxCents(0L);
        invoice.setDifferenceNote(request.getReason()); invoice.setLines(List.of());
        request.setDocumentId(invoice.getDocumentId());
        if (create) {
            if (invoices.insertDocument(invoice)!=1 || invoices.insertInvoice(invoice)!=1 || invoices.insertIdentity(invoice)!=1 || opening.insert(request)!=1) throw new ServiceException("期初应付保存失败");
        } else {
            if (invoices.updateInvoice(invoice)!=1 || opening.update(request)!=1) throw new ServiceException("期初应付已变化，请刷新重试");
        }
        return invoiceService.get(invoice.getDocumentId());
    }

    @Transactional public int submit(Long id, Integer revision) {
        Invoice invoice = invoices.selectDraft(id); requireOwner(invoice, revision);
        if (opening.select(id) == null) throw new ServiceException("该单据不是期初应付");
        if (invoices.selectAttachmentCount(id) == 0) throw new ServiceException("请上传原票或期初余额依据后再提交");
        WorkflowConfig config = invoices.selectLatestConfig("INVOICE_APPROVAL");
        Long reviewer = null;
        if (config != null && config.getFinanceCandidates()!=null) for (String value : config.getFinanceCandidates().split(",")) {
            try { Long candidate = Long.valueOf(value.trim()); if (invoices.selectEligibleFinanceCount(candidate, SecurityUtils.getUserId(), -1L)>0) { reviewer=candidate; break; } } catch (NumberFormatException ignored) { }
        }
        if (reviewer==null) throw new ServiceException("请配置与登记人不同的有效财务审批人");
        Date now = new Date(); Long versionId = IdUtils.nextLongId(); Long workflowId = IdUtils.nextLongId();
        int version = invoice.getCurrentVersion()+1;
        try {
            String snapshot = new ObjectMapper().writeValueAsString(invoice);
            String hash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(snapshot.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            invoices.insertVersion(versionId,id,version,snapshot,hash,SecurityUtils.getUserId(),now);
            invoices.insertVersionAttachments(versionId,id); invoices.insertWorkflow(workflowId,id,versionId,config.getVersion(),now);
            invoices.insertTask(IdUtils.nextLongId(),workflowId,"FINANCE",1,reviewer);
            invoice.setCurrentVersion(version); invoice.setApprovalStatus("IN_REVIEW"); invoice.setCurrentNode("FINANCE");
            if(invoices.updateDocument(invoice)!=1) throw new ServiceException("期初应付已变化，请刷新重试");
            invoices.insertAudit(IdUtils.nextLongId(),id,versionId,SecurityUtils.getUserId(),SecurityUtils.getUsername(),"OPENING_SUBMIT",now);
            return 1;
        } catch (ServiceException e) { throw e; } catch (Exception e) { throw new ServiceException("期初应付提交失败"); }
    }
    private void requireOwner(Invoice invoice,Integer revision) {
        if(invoice==null || !SecurityUtils.getUserId().equals(invoice.getCreatorId())) throw new ServiceException("仅登记人可以修改或提交期初应付草稿");
        if(revision==null || !revision.equals(invoice.getRevision())) throw new ServiceException("单据已变化，请刷新重试");
    }
}
