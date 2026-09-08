package com.allinone.supply.service.impl;

import com.allinone.supply.domain.*;
import com.allinone.supply.mapper.ApprovalMapper;
import com.allinone.supply.support.TaskTransferAuthorization;
import com.allinone.common.utils.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ApprovalTransferTest {
    private final ApprovalMapper mapper = mock(ApprovalMapper.class);
    private final TaskTransferAuthorization auth = mock(TaskTransferAuthorization.class);
    private final ApprovalServiceImpl service = new ApprovalServiceImpl();
    ApprovalTransferTest() {
        ReflectionTestUtils.setField(service,"mapper",mapper);
        ReflectionTestUtils.setField(service,"transferAuthorization",auth);
    }
    private ApprovalTransfer request() {
        ApprovalTransfer request = new ApprovalTransfer(); request.setTaskId(1L); request.setAssigneeId(30L);
        ApprovalTask task = new ApprovalTask(); task.setTaskId(1L); task.setDocumentId(100L); task.setVersionId(101L);
        task.setStatus("PENDING"); task.setNode("FINANCE"); task.setAssigneeId(20L);
        when(mapper.selectTask(1L)).thenReturn(task);
        when(mapper.selectTaskForUpdate(1L)).thenReturn(task);
        return request;
    }
    @Test void rejectsWrongNodeRoleWithoutMutation() {
        var request = request();
        assertThatThrownBy(() -> service.transfer(request)).hasMessageContaining("业务资格");
        verify(auth).requireOperator(100L,"supply:approval:transfer");
        verify(auth).requireRecipient(100L,30L,"supply:approval:approve");
        verify(mapper,never()).transferTask(any(),any(),any());
    }
    @Test void recordsBothAssigneesOnSuccessfulTransfer() {
        var request = request();
        when(auth.hasRole(30L,"finance")).thenReturn(true);
        when(mapper.transferTask(1L,30L,20L)).thenReturn(1);
        when(mapper.insertAudit(any(),any(),any(),any(),any(),any(),any(),any())).thenReturn(1);
        try (var security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getUserId).thenReturn(40L);
            security.when(SecurityUtils::getUsername).thenReturn("manager");
            assertThat(service.transfer(request)).isEqualTo(1);
        }
        verify(mapper).insertAudit(any(),eq(100L),eq(101L),eq(40L),eq("manager"),eq("TRANSFER"),argThat(s -> s.contains("20") && s.contains("30")),any());
    }
}
