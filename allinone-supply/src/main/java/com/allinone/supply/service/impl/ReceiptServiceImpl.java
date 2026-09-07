package com.allinone.supply.service.impl;

import com.allinone.common.exception.ServiceException;
import com.allinone.common.utils.DateUtils;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.uuid.IdUtils;
import com.allinone.supply.domain.Receipt;
import com.allinone.supply.domain.ReceiptLine;
import com.allinone.supply.mapper.ReceiptMapper;
import com.allinone.supply.service.IReceiptService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceiptServiceImpl implements IReceiptService {
    private static final ObjectMapper JSON = new ObjectMapper();
    @Autowired private ReceiptMapper mapper;

    @Override public Receipt prepare(Long orderId) {
        if (orderId == null || mapper.selectApprovedOrderCount(orderId) == 0) throw new ServiceException("订单不存在、未通过审批或已作废");
        Receipt receipt = new Receipt(); receipt.setOrderId(orderId); receipt.setLines(mapper.selectOrderLineBalances(orderId)); return receipt;
    }

    @Override @Transactional public int confirm(Receipt receipt) {
        if (!isWarehouseOperator()) throw new ServiceException("仅仓管员或管理员可以确认入库");
        if (receipt == null || receipt.getOrderId() == null || receipt.getWarehouseId() == null || receipt.getLines() == null || receipt.getLines().isEmpty()) throw new ServiceException("订单、仓库和入库明细不能为空");
        if (mapper.selectApprovedOrderCount(receipt.getOrderId()) == 0) throw new ServiceException("订单不存在、未通过审批或已作废");
        if (mapper.selectEnabledWarehouseCount(receipt.getWarehouseId()) == 0) throw new ServiceException("仓库不存在或已停用");
        Date now = DateUtils.getNowDate(); LocalDate today = now.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        if (receipt.getBusinessDate() == null || receipt.getBusinessDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().isAfter(today)) throw new ServiceException("入库业务日期不能晚于当天");
        Map<Long, ReceiptLine> balances = new HashMap<>();
        for (ReceiptLine line : mapper.selectOrderLineBalances(receipt.getOrderId())) balances.put(line.getOrderLineId(), line);
        Map<Long, BigDecimal> requested = new HashMap<>();
        int valid = 0;
        for (ReceiptLine input : receipt.getLines()) {
            ReceiptLine balance = balances.get(input.getOrderLineId());
            if (balance == null || input.getQuantity() == null || input.getQuantity().signum() <= 0) throw new ServiceException("入库明细不合法或不属于当前订单");
            if (requested.put(input.getOrderLineId(), input.getQuantity()) != null) throw new ServiceException("同一入库单中同一订单行只能出现一次");
            if (input.getQuantity().compareTo(balance.getRemainingQuantity()) > 0) throw new ServiceException("商品「" + balance.getItemSnapshot() + "」当前最多可入库 " + balance.getRemainingQuantity());
            input.setItemSnapshot(balance.getItemSnapshot()); valid++;
        }
        if (valid == 0) throw new ServiceException("至少需要一条正数入库明细");
        receipt.setDocumentId(IdUtils.nextLongId()); receipt.setNumber(nextNumber(now)); receipt.setCreateBy(SecurityUtils.getUsername()); receipt.setCreateTime(now);
        receipt.setConfirmedBy(SecurityUtils.getUserId()); receipt.setConfirmedAt(now);
        mapper.insertDocument(receipt); mapper.insertReceipt(receipt);
        for (ReceiptLine line : receipt.getLines()) mapper.insertLine(IdUtils.nextLongId(), receipt.getDocumentId(), line.getOrderLineId(), line.getQuantity(), line.getItemSnapshot());
        try {
            Long versionId = IdUtils.nextLongId();
            String snapshot = JSON.writeValueAsString(receipt);
            mapper.insertVersion(versionId, receipt.getDocumentId(), snapshot, sha256(snapshot), SecurityUtils.getUserId(), now);
            if (mapper.updateDocumentVersion(receipt.getDocumentId()) == 0) throw new ServiceException("入库单状态已变化，请刷新后重试");
            mapper.insertAudit(IdUtils.nextLongId(), receipt.getDocumentId(), SecurityUtils.getUserId(), versionId, SecurityUtils.getUsername(), "RECEIPT_CONFIRMED", now);
        } catch (ServiceException e) { throw e; } catch (Exception e) { throw new ServiceException("入库快照保存失败"); }
        return 1;
    }
    private String nextNumber(Date date) { mapper.insertSequence(date); mapper.incrementSequence(date); return String.format("GR-%tY%<tm%<td-%05d", date, mapper.selectSequence(date) - 1); }
    private boolean isWarehouseOperator() { if (SecurityUtils.isAdmin()) return true; return SecurityUtils.getLoginUser().getUser().getRoles().stream().anyMatch(role -> "warehouse".equals(role.getRoleKey())); }
    private String sha256(String value) throws Exception { byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); StringBuilder result = new StringBuilder(); for (byte item : digest) result.append(String.format("%02x", item)); return result.toString(); }
}
