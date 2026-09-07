package com.allinone.supply.mapper;
import com.allinone.supply.domain.*; import java.util.*; import org.apache.ibatis.annotations.Param;
public interface InvoiceMapper {
    List<Invoice> selectList(@Param("mode") String mode,@Param("userId") Long userId,@Param("deptId") Long deptId,@Param("invoiceNumber") String invoiceNumber,@Param("approvalStatus") String approvalStatus);
    int selectApprovedOrderCount(@Param("orderId") Long orderId); int selectIdentityCount(@Param("sellerTaxId") String sellerTaxId,@Param("invoiceNumber") String invoiceNumber);
    String selectSupplierTaxId(@Param("orderId") Long orderId); String selectCompanyTaxId(); Long selectOrderBuyerId(@Param("orderId") Long orderId); int selectEligibleBuyerCount(@Param("buyerId") Long buyerId); int selectEligibleFinanceCount(@Param("userId") Long userId, @Param("submitterId") Long submitterId, @Param("purchaseId") Long purchaseId);
    int selectAttachmentCount(@Param("documentId") Long documentId);
    int selectOrderLineCount(@Param("orderId") Long orderId, @Param("orderLineId") Long orderLineId);
    PurchaseOrderLine selectOrderLine(@Param("orderLineId") Long orderLineId);
    int confirmDraft(@Param("documentId") Long documentId, @Param("revision") Integer revision, @Param("hash") String hash, @Param("userId") Long userId);
    int updateInvoice(Invoice invoice); int archiveLines(@Param("invoiceId") Long invoiceId); 
    Invoice selectDraft(@Param("documentId") Long documentId); List<InvoiceLine> selectLines(@Param("invoiceId") Long invoiceId);
    List<AllocationTarget> selectAllocationTargets(@Param("orderLineId") Long orderLineId); int insertDocument(Invoice invoice); int insertInvoice(Invoice invoice); int insertLine(InvoiceLine line); int insertIdentity(Invoice invoice);
    int insertAllocation(@Param("id") Long id,@Param("invoiceId") Long invoiceId,@Param("versionId") Long versionId,@Param("invoiceLineId") Long invoiceLineId,@Param("receiptLineId") Long receiptLineId,@Param("quantity") Object quantity); int updateDocument(Invoice invoice);
    int insertVersion(@Param("id") Long id,@Param("documentId") Long documentId,@Param("versionNo") int versionNo,@Param("snapshotJson") String snapshotJson,@Param("contentHash") String contentHash,@Param("submittedBy") Long submittedBy,@Param("submittedAt") Date submittedAt);
    int insertVersionAttachments(@Param("versionId") Long versionId, @Param("documentId") Long documentId);
    int insertWorkflow(@Param("id") Long id,@Param("documentId") Long documentId,@Param("versionId") Long versionId,@Param("configVersion") int configVersion,@Param("startedAt") Date startedAt);
    int insertTask(@Param("id") Long id,@Param("instanceId") Long instanceId,@Param("node") String node,@Param("sequenceNo") int sequenceNo,@Param("assigneeId") Long assigneeId);
    WorkflowConfig selectLatestConfig(@Param("flowType") String flowType); int insertAudit(@Param("id") Long id,@Param("documentId") Long documentId,@Param("versionId") Long versionId,@Param("actorId") Long actorId,@Param("actorSnapshot") String actorSnapshot,@Param("action") String action,@Param("createdAt") Date createdAt);
}
