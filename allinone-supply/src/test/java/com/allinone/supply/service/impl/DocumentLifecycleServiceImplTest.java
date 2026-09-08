package com.allinone.supply.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.allinone.common.exception.ServiceException;
import com.allinone.supply.domain.DocumentLifecycle;
import com.allinone.supply.mapper.DocumentLifecycleMapper;
import org.flowable.engine.RuntimeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentLifecycleServiceImplTest {
    @Mock DocumentLifecycleMapper mapper;
    @Mock RuntimeService runtimeService;
    @InjectMocks DocumentLifecycleServiceImpl service;

    @Test void invoiceWithPaymentCannotBeVoidedOrReleaseReceiptAllocations() {
        DocumentLifecycle invoice = new DocumentLifecycle(); invoice.setDocumentId(100L);
        invoice.setType("INVOICE"); invoice.setStatus("APPROVED");
        when(mapper.selectDocument(100L)).thenReturn(invoice);
        when(mapper.countInvoicePayments(100L)).thenReturn(1);
        assertThatThrownBy(() -> service.voidDocument(100L, "录入有误"))
                .isInstanceOf(ServiceException.class).hasMessageContaining("付款");
        verify(mapper, never()).releaseAllocations(anyLong());
        verify(mapper, never()).updateStatus(anyLong(), any(), anyString());
    }
}
