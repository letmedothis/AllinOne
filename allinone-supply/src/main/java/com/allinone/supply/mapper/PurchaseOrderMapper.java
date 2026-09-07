package com.allinone.supply.mapper;

import com.allinone.supply.domain.PurchaseOrder;
import com.allinone.supply.domain.PurchaseOrderLine;
import com.allinone.supply.domain.WorkflowConfig;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface PurchaseOrderMapper {
    List<PurchaseOrder> selectPurchaseOrderList(PurchaseOrder order);
    PurchaseOrder selectPurchaseOrderById(@Param("documentId") Long documentId);
    List<PurchaseOrderLine> selectLinesByOrderId(@Param("orderId") Long orderId);
    int selectApprovedSupplierCount(@Param("supplierId") Long supplierId);
    int selectEligibleBuyerCount(@Param("buyerId") Long buyerId);
    int selectEligibleSupervisorCount(@Param("userId") Long userId, @Param("excludeId") Long excludeId);
    int insertDocument(PurchaseOrder order);
    int insertOrder(PurchaseOrder order);
    int insertLine(PurchaseOrderLine line);
    int deleteLines(Long orderId);
    int updateOrder(PurchaseOrder order);
    int updateDocument(PurchaseOrder order);
    int insertSequence(@Param("type") String type, @Param("businessDate") Date businessDate);
    int incrementSequence(@Param("type") String type, @Param("businessDate") Date businessDate);
    Integer selectSequence(@Param("type") String type, @Param("businessDate") Date businessDate);
    WorkflowConfig selectLatestConfig(String flowType);
    int insertVersion(@Param("id") Long id, @Param("documentId") Long documentId, @Param("versionNo") int versionNo,
                      @Param("snapshotJson") String snapshotJson, @Param("contentHash") String contentHash,
                      @Param("submittedBy") Long submittedBy, @Param("submittedAt") Date submittedAt);
    int insertVersionAttachments(@Param("versionId") Long versionId, @Param("documentId") Long documentId);
    int insertWorkflow(@Param("id") Long id, @Param("documentId") Long documentId, @Param("versionId") Long versionId,
                       @Param("configVersion") int configVersion, @Param("startedAt") Date startedAt);
    int insertTask(@Param("id") Long id, @Param("instanceId") Long instanceId, @Param("assigneeId") Long assigneeId);
    int insertAudit(@Param("id") Long id, @Param("documentId") Long documentId, @Param("versionId") Long versionId,
                    @Param("actorId") Long actorId, @Param("actorSnapshot") String actorSnapshot,
                    @Param("action") String action, @Param("createdAt") Date createdAt);
}
