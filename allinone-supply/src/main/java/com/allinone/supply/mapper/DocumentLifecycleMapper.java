package com.allinone.supply.mapper;

import com.allinone.supply.domain.DocumentLifecycle;
import org.apache.ibatis.annotations.Param;

public interface DocumentLifecycleMapper {
    DocumentLifecycle selectDocument(@Param("documentId") Long documentId);
    int countSupplierReferences(@Param("documentId") Long documentId);
    int countOrderReceipts(@Param("documentId") Long documentId);
    int countOrderInvoices(@Param("documentId") Long documentId);
    int cancelTasks(@Param("documentId") Long documentId);
    int releaseAllocations(@Param("documentId") Long documentId);
    int updateStatus(@Param("documentId") Long documentId, @Param("revision") Integer revision, @Param("status") String status);
    int markDeleted(@Param("documentId") Long documentId, @Param("revision") Integer revision);
    int updateWorkflow(@Param("documentId") Long documentId, @Param("status") String status);
    int insertAudit(@Param("id") Long id, @Param("documentId") Long documentId, @Param("actorId") Long actorId, @Param("actorSnapshot") String actorSnapshot, @Param("action") String action, @Param("reason") String reason);
}
