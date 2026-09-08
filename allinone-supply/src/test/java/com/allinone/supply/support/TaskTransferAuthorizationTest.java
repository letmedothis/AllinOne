package com.allinone.supply.support;

import com.allinone.common.exception.ServiceException;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.supply.mapper.TaskTransferMapper;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskTransferAuthorizationTest {
    private final TaskTransferMapper mapper = mock(TaskTransferMapper.class);
    private final TaskTransferAuthorization auth = new TaskTransferAuthorization();
    TaskTransferAuthorizationTest() { ReflectionTestUtils.setField(auth,"mapper",mapper); }

    @Test void rejectsSupervisorOutsideScopeAndAllowsScopedSupervisor() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getUserId).thenReturn(20L);
            when(mapper.countPermission(20L,"transfer")).thenReturn(1);
            when(mapper.countRole(20L,"supervisor")).thenReturn(1);
            assertThatThrownBy(() -> auth.requireOperator(100L,"transfer")).isInstanceOf(ServiceException.class);
            when(mapper.countScope(100L,20L)).thenReturn(1);
            assertThatCode(() -> auth.requireOperator(100L,"transfer")).doesNotThrowAnyException();
        }
    }

    @Test void adminCannotBypassRecipientPermissionOrConflict() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isAdmin).thenReturn(true);
            assertThatCode(() -> auth.requireOperator(100L,"transfer")).doesNotThrowAnyException();
            assertThatThrownBy(() -> auth.requireRecipient(100L,30L,"approve")).hasMessageContaining("办理权限");
            when(mapper.countPermission(30L,"approve")).thenReturn(1);
            when(mapper.countConflict(100L,30L)).thenReturn(1);
            assertThatThrownBy(() -> auth.requireRecipient(100L,30L,"approve")).hasMessageContaining("申请人");
        }
    }
}
