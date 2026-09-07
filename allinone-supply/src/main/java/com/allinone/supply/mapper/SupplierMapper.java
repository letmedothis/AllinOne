package com.allinone.supply.mapper;

import com.allinone.supply.domain.Supplier;
import com.allinone.supply.domain.WorkflowConfig;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface SupplierMapper {
    List<Supplier> selectSupplierList(Supplier supplier);
    Supplier selectSupplierById(@Param("documentId") Long documentId);
    int insertDocument(Supplier supplier);
    int insertSupplier(Supplier supplier);
    int updateSupplier(Supplier supplier);
    int updateDocument(Supplier supplier);
    int insertSequence(@Param("type") String type, @Param("businessDate") Date businessDate);
    int incrementSequence(@Param("type") String type, @Param("businessDate") Date businessDate);
    Integer selectSequence(@Param("type") String type, @Param("businessDate") Date businessDate);
    WorkflowConfig selectLatestConfig(@Param("flowType") String flowType);
    int selectEligibleSupervisorCount(@Param("userId") Long userId, @Param("excludeId") Long excludeId);
    int countAttachments(@Param("documentId") Long documentId);
    int insertVersion(@Param("id") Long id, @Param("documentId") Long documentId, @Param("versionNo") int versionNo,
                      @Param("snapshotJson") String snapshotJson, @Param("contentHash") String contentHash,
                      @Param("submittedBy") Long submittedBy, @Param("submittedAt") Date submittedAt);
    int insertVersionAttachments(@Param("versionId") Long versionId, @Param("documentId") Long documentId);
    int insertWorkflow(@Param("id") Long id, @Param("documentId") Long documentId, @Param("versionId") Long versionId,
                       @Param("configVersion") int configVersion, @Param("startedAt") Date startedAt);
    int insertTask(@Param("id") Long id, @Param("instanceId") Long instanceId, @Param("assigneeId") Long assigneeId);
    int insertAudit(@Param("id") Long id, @Param("documentId") Long documentId, @Param("versionId") Long versionId,
                    @Param("actorId") Long actorId, @Param("actorSnapshot") String actorSnapshot,
                    @Param("action") String action, @Param("reason") String reason, @Param("changesJson") String changesJson,
                    @Param("createdAt") Date createdAt);
}
