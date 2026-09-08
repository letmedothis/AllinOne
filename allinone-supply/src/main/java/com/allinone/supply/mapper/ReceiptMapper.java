package com.allinone.supply.mapper;

import com.allinone.supply.domain.Receipt;
import com.allinone.supply.domain.ReceiptLine;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ReceiptMapper {
    List<java.util.Map<String, Object>> selectOrderOptions();
    List<java.util.Map<String, Object>> selectWarehouseOptions();
    List<java.util.Map<String, Object>> selectHistory(@Param("orderId") Long orderId);
    int selectApprovedOrderCount(@Param("orderId") Long orderId);
    int selectEnabledWarehouseCount(@Param("warehouseId") Long warehouseId);
    List<ReceiptLine> selectOrderLineBalances(@Param("orderId") Long orderId);
    int insertSequence(@Param("businessDate") Date businessDate);
    int incrementSequence(@Param("businessDate") Date businessDate);
    Integer selectSequence(@Param("businessDate") Date businessDate);
    int insertDocument(Receipt receipt);
    int insertReceipt(Receipt receipt);
    int insertLine(@Param("id") Long id, @Param("receiptId") Long receiptId, @Param("orderLineId") Long orderLineId,
                   @Param("quantity") Object quantity, @Param("itemSnapshot") String itemSnapshot);
    int insertVersion(@Param("id") Long id, @Param("documentId") Long documentId, @Param("snapshotJson") String snapshotJson,
                      @Param("contentHash") String contentHash, @Param("submittedBy") Long submittedBy, @Param("submittedAt") Date submittedAt);
    int updateDocumentVersion(@Param("documentId") Long documentId);
    int insertAudit(@Param("id") Long id, @Param("documentId") Long documentId, @Param("actorId") Long actorId,
                    @Param("versionId") Long versionId, @Param("actorSnapshot") String actorSnapshot, @Param("action") String action, @Param("createdAt") Date createdAt);
}
