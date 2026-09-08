package com.allinone.supply.service;

import com.allinone.supply.domain.ApprovalDecision;
import com.allinone.supply.domain.ApprovalTask;
import com.allinone.supply.domain.ApprovalTransfer;
import java.util.List;

public interface IApprovalService {
    List<ApprovalTask> selectPendingTasks();
    int approve(ApprovalDecision decision);
    int reject(ApprovalDecision decision);
    int transfer(ApprovalTransfer request);
}
