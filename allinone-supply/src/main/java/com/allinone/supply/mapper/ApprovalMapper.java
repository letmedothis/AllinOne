package com.allinone.supply.mapper;

import com.allinone.supply.domain.ApprovalTask;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ApprovalMapper {
    List<ApprovalTask> selectPendingTasks(@Param("assigneeId") Long assigneeId);
    ApprovalTask selectTask(@Param("taskId") Long taskId);
    ApprovalTask selectTaskForUpdate(@Param("taskId") Long taskId);
    int completeTask(@Param("taskId") Long taskId, @Param("revision") Integer revision, @Param("decision") String decision,
                     @Param("comment") String comment, @Param("actorId") Long actorId, @Param("actedAt") Date actedAt);
    int updateDocument(@Param("documentId") Long documentId, @Param("revision") Integer revision, @Param("status") String status,
                       @Param("currentNode") String currentNode, @Param("expectedNode") String expectedNode, @Param("updateTime") Date updateTime);
    int updateWorkflow(@Param("instanceId") Long instanceId, @Param("status") String status, @Param("finishedAt") Date finishedAt);
    int cancelPendingTasks(@Param("instanceId") Long instanceId);
    int insertTask(@Param("id") Long id, @Param("instanceId") Long instanceId, @Param("node") String node, @Param("sequenceNo") Integer sequenceNo, @Param("assigneeId") Long assigneeId);
    String selectFinanceCandidates(@Param("instanceId") Long instanceId);
    int selectEligibleFinanceCount(@Param("userId") Long userId, @Param("submitterId") Long submitterId, @Param("purchaseId") Long purchaseId);
    int confirmInvoiceAllocations(@Param("documentId") Long documentId);
    int releaseInvoiceAllocations(@Param("documentId") Long documentId);
    int transferTask(@Param("taskId") Long taskId, @Param("assigneeId") Long assigneeId, @Param("fromAssigneeId") Long fromAssigneeId);
    int countActiveUser(@Param("userId") Long userId);
    int insertAudit(@Param("id") Long id, @Param("documentId") Long documentId, @Param("versionId") Long versionId,
                    @Param("actorId") Long actorId, @Param("actorSnapshot") String actorSnapshot, @Param("action") String action,
                    @Param("reason") String reason, @Param("createdAt") Date createdAt);
}
