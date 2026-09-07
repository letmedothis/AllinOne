package com.allinone.supply.service.impl;

import com.allinone.common.exception.ServiceException;
import com.allinone.common.utils.DateUtils;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.StringUtils;
import com.allinone.common.utils.uuid.IdUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.allinone.supply.domain.Supplier;
import com.allinone.supply.domain.WorkflowConfig;
import com.allinone.supply.mapper.SupplierMapper;
import com.allinone.supply.service.ISupplierService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierServiceImpl implements ISupplierService {
    private static final String DOCUMENT_TYPE = "SUPPLIER";
    private static final String FLOW_TYPE = "SUPPLIER_ONBOARDING";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private SupplierMapper supplierMapper;

    @Override
    public List<Supplier> selectSupplierList(Supplier supplier) {
        if (!SecurityUtils.isAdmin()) {
            supplier.getParams().put("currentUserId", SecurityUtils.getUserId());
        }
        supplier.getParams().put("supplyGlobal", SecurityUtils.isAdmin() || hasAnyRole("supervisor", "purchasing_supervisor", "purchase_supervisor", "finance"));
        return supplierMapper.selectSupplierList(supplier);
    }

    @Override
    public Supplier selectSupplierById(Long documentId) {
        Supplier supplier = supplierMapper.selectSupplierById(documentId);
        requireVisible(supplier);
        return supplier;
    }

    @Override
    @Transactional
    public int insertSupplier(Supplier supplier) {
        supplier.setTaxId(normalize(supplier.getTaxId()));
        validateFields(supplier);
        Date now = DateUtils.getNowDate();
        Long userId = SecurityUtils.getUserId();
        LocalDate businessDate = now.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        Date sequenceDate = Date.from(businessDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        supplier.setDocumentId(IdUtils.nextLongId());
        supplier.setCreatorId(userId);
        supplier.setCreationKey(StringUtils.isEmpty(supplier.getCreationKey()) ? IdUtils.fastSimpleUUID() : supplier.getCreationKey());
        supplier.setNumber(nextNumber(sequenceDate));
        supplier.setCreateBy(SecurityUtils.getUsername());
        supplier.setCreateTime(now);
        supplier.setUpdateTime(now);
        supplier.setApprovalStatus("DRAFT");
        supplier.setCurrentVersion(0);
        supplier.setRevision(0);
        supplier.setDedupKey(normalize(supplier.getTaxId()));
        supplierMapper.insertDocument(supplier);
        return supplierMapper.insertSupplier(supplier);
    }

    @Override
    @Transactional
    public int updateSupplier(Supplier supplier) {
        if (supplier.getDocumentId() == null || supplier.getRevision() == null) {
            throw new ServiceException("缺少供应商单据ID或版本号");
        }
        Supplier existing = supplierMapper.selectSupplierById(supplier.getDocumentId());
        requireOwner(existing);
        if (!("DRAFT".equals(existing.getApprovalStatus()) || "RETURNED".equals(existing.getApprovalStatus()))) {
            throw new ServiceException("当前状态不允许编辑供应商");
        }
        supplier.setTaxId(normalize(supplier.getTaxId()));
        validateFields(supplier);
        supplier.setDedupKey(normalize(supplier.getTaxId()));
        supplier.setUpdateBy(SecurityUtils.getUsername());
        supplier.setUpdateTime(DateUtils.getNowDate());
        int rows = supplierMapper.updateSupplier(supplier);
        if (rows == 0) {
            throw new ServiceException("供应商已被其他用户修改，请刷新后重试");
        }
        return rows;
    }

    @Override
    @Transactional
    public int submitSupplier(Long documentId) {
        Supplier supplier = supplierMapper.selectSupplierById(documentId);
        requireOwner(supplier);
        if (supplierMapper.countAttachments(documentId) == 0) throw new ServiceException("提交供应商前至少上传一个资质附件");
        if (!("DRAFT".equals(supplier.getApprovalStatus()) || "RETURNED".equals(supplier.getApprovalStatus()))) {
            throw new ServiceException("当前状态不允许提交供应商");
        }
        WorkflowConfig config = supplierMapper.selectLatestConfig(FLOW_TYPE);
        Long assigneeId = firstEligibleAssignee(config == null ? null : config.getSupervisorCandidates());
        if (assigneeId == null) {
            throw new ServiceException("未配置合法的采购主管审批人，请先在系统设置中配置");
        }
        try {
            int versionNo = supplier.getCurrentVersion() + 1;
            Long versionId = IdUtils.nextLongId();
            Date now = DateUtils.getNowDate();
            String snapshot = MAPPER.writeValueAsString(supplier);
            supplierMapper.insertVersion(versionId, documentId, versionNo, snapshot, sha256(snapshot), SecurityUtils.getUserId(), now);
            supplierMapper.insertVersionAttachments(versionId, documentId);
            Long workflowId = IdUtils.nextLongId();
            supplierMapper.insertWorkflow(workflowId, documentId, versionId, config.getVersion(), now);
            supplierMapper.insertTask(IdUtils.nextLongId(), workflowId, assigneeId);
            supplier.setCurrentVersion(versionNo);
            supplier.setCurrentNode("SUPERVISOR");
            supplier.setApprovalStatus("IN_REVIEW");
            supplier.setUpdateBy(SecurityUtils.getUsername());
            supplier.setUpdateTime(now);
            if (supplierMapper.updateDocument(supplier) == 0) {
                throw new ServiceException("供应商状态已变化，请刷新后重试");
            }
            supplierMapper.insertAudit(IdUtils.nextLongId(), documentId, versionId, SecurityUtils.getUserId(),
                    SecurityUtils.getUsername(), "SUBMIT", null, null, now);
            return 1;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("供应商提交失败");
        }
    }

    private String nextNumber(Date businessDate) {
        supplierMapper.insertSequence(DOCUMENT_TYPE, businessDate);
        supplierMapper.incrementSequence(DOCUMENT_TYPE, businessDate);
        Integer next = supplierMapper.selectSequence(DOCUMENT_TYPE, businessDate);
        return String.format("SUP-%tY%<tm%<td-%05d", businessDate, next - 1);
    }

    private void validateFields(Supplier supplier) {
        if (StringUtils.isEmpty(supplier.getName()) || StringUtils.isEmpty(supplier.getTaxId())
                || StringUtils.isEmpty(supplier.getContact()) || StringUtils.isEmpty(supplier.getPhone())
                || StringUtils.isEmpty(supplier.getAddress()) || StringUtils.isEmpty(supplier.getBankName())
                || StringUtils.isEmpty(supplier.getAccountName()) || StringUtils.isEmpty(supplier.getBankAccount())) {
            throw new ServiceException("供应商名称、税号、联系人、电话、地址和开户信息均不能为空");
        }
        if (supplier.getName().trim().length() < 2 || supplier.getName().trim().length() > 200) {
            throw new ServiceException("供应商名称长度必须为2到200个字符");
        }
        if (!supplier.getTaxId().trim().matches("[0-9A-Z]{18}")) {
            throw new ServiceException("统一社会信用代码/销方税号必须为18位字母或数字");
        }
    }

    private Long firstEligibleAssignee(String candidates) {
        if (StringUtils.isEmpty(candidates)) return null;
        for (String value : candidates.split(",")) {
            try { Long userId = Long.valueOf(value.trim()); if (supplierMapper.selectEligibleSupervisorCount(userId, SecurityUtils.getUserId()) > 0) return userId; } catch (NumberFormatException ignored) { }
        }
        return null;
    }

    private void requireVisible(Supplier supplier) {
        if (supplier == null) throw new ServiceException("供应商不存在");
        if (SecurityUtils.isAdmin() || hasAnyRole("supervisor", "purchasing_supervisor", "purchase_supervisor", "finance")) return;
        if (!SecurityUtils.isAdmin() && !SecurityUtils.getUserId().equals(supplier.getCreatorId())) {
            throw new ServiceException("无权访问该供应商");
        }
    }

    private void requireOwner(Supplier supplier) {
        requireVisible(supplier);
        if (!SecurityUtils.isAdmin() && !SecurityUtils.getUserId().equals(supplier.getCreatorId())) {
            throw new ServiceException("仅供应商创建人可以操作");
        }
    }

    private String normalize(String value) { return value == null ? null : value.trim().toUpperCase(); }

    private boolean hasAnyRole(String... roleKeys) {
        if (SecurityUtils.getLoginUser() == null || SecurityUtils.getLoginUser().getUser().getRoles() == null) return false;
        return SecurityUtils.getLoginUser().getUser().getRoles().stream().anyMatch(role -> java.util.Arrays.asList(roleKeys).contains(role.getRoleKey()));
    }

    private String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte item : digest) result.append(String.format("%02x", item));
        return result.toString();
    }
}
