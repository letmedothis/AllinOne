package com.allinone.supply.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.allinone.common.core.domain.entity.SysUser;
import com.allinone.common.core.domain.model.LoginUser;
import com.allinone.common.exception.ServiceException;
import com.allinone.supply.domain.*;
import com.allinone.supply.mapper.PaymentMapper;
import com.allinone.supply.support.SupplyDataScopeResolver;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {
    @Mock PaymentMapper mapper;
    @Mock SupplyDataScopeResolver scopeResolver;
    @Mock SupplierChangeService supplierChanges;
    @Mock com.allinone.supply.mapper.SupplierChangeMapper supplierChangeMapper;
    @InjectMocks PaymentServiceImpl service;

    @BeforeEach void login() {
        SysUser user = new SysUser(); user.setUserId(20L); user.setUserName("purchaser");
        user.setRoles(List.of());
        LoginUser login = new LoginUser(20L, 10L, user, Set.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login, null, List.of()));
        lenient().when(mapper.insertEvent(any())).thenReturn(1);
    }
    @AfterEach void logout() { SecurityContextHolder.clearContext(); }

    @Test void createsServerOwnedAmountsAndReturnsPersistentIdentity() {
        Payment payment = draft(); payment.setLines(List.of(line(500L)));
        payment.setAmountCents(1L); payment.setThresholdExceeded("1");
        when(mapper.selectSequence(anyString(), any())).thenReturn(2);
        when(mapper.insertPayment(any())).thenReturn(1);
        when(mapper.insertPaymentLine(any())).thenReturn(1);
        service.create(payment);
        assertThat(payment.getId()).isNotNull();
        assertThat(payment.getAmountCents()).isEqualTo(500L);
        assertThat(payment.getThresholdExceeded()).isEqualTo("0");
        assertThat(payment.getLines().get(0).getPaymentId()).isEqualTo(payment.getId());
    }

    @ParameterizedTest @CsvSource({"9999999,0", "10000000,0", "10000001,1"})
    void submitsSavedLinesAndRecomputesThreshold(long cents, String expected) {
        Payment payment = draft(); payment.setStatus("RETURNED");
        payment.setThresholdExceeded("incorrect-client-value");
        when(mapper.selectById(100L)).thenReturn(payment);
        when(mapper.selectLines(100L)).thenReturn(List.of(line(cents)));
        when(mapper.selectSupplierApproved(200L)).thenReturn(1);
        when(mapper.selectInvoiceSupplierId(300L)).thenReturn(200L);
        when(mapper.lockApprovedInvoice(300L)).thenReturn(cents);
        when(mapper.selectAllocatedForInvoice(300L, 100L)).thenReturn(0L);
        when(mapper.submitPayment(any())).thenReturn(1);
        service.submit(100L);
        verify(mapper).submitPayment(argThat(p -> p.getAmountCents() == cents && expected.equals(p.getThresholdExceeded())));
    }

    @Test void rejectsAllocationBeyondAvailableInvoiceBalance() {
        when(mapper.selectById(100L)).thenReturn(draft());
        when(mapper.selectLines(100L)).thenReturn(List.of(line(500L)));
        when(mapper.selectSupplierApproved(200L)).thenReturn(1);
        when(mapper.selectInvoiceSupplierId(300L)).thenReturn(200L);
        when(mapper.lockApprovedInvoice(300L)).thenReturn(600L);
        when(mapper.selectAllocatedForInvoice(300L, 100L)).thenReturn(200L);
        assertThatThrownBy(() -> service.submit(100L)).isInstanceOf(ServiceException.class).hasMessageContaining("余额不足");
        verify(mapper, never()).submitPayment(any());
    }

    @Test void largePaymentsCannotBypassDirectorWithStaleThresholdFlag() {
        Payment payment = draft(); payment.setCreatorId(21L); payment.setStatus("IN_REVIEW");
        payment.setCurrentNode("FINANCE_REVIEW"); payment.setAmountCents(10_000_001L); payment.setThresholdExceeded("0");
        when(mapper.selectById(100L)).thenReturn(payment);
        when(mapper.countReviewEligible(20L, 21L)).thenReturn(1);
        when(mapper.updateState(eq(100L), eq(0), eq("IN_REVIEW"), eq("FINANCE_DIRECTOR"), anyString(), isNull(), any())).thenReturn(1);
        PaymentDecision decision = new PaymentDecision(); decision.setApproved(true); decision.setRevision(0);
        service.review(100L, decision);
        verify(mapper).updateState(eq(100L), eq(0), eq("IN_REVIEW"), eq("FINANCE_DIRECTOR"), anyString(), isNull(), any());
    }

    @Test void rejectsNegativeAmountsEvenIfOverallSumIsPositive() {
        Payment payment = draft(); payment.setLines(List.of(line(-1L)));
        when(mapper.selectSequence(anyString(), any())).thenReturn(2);
        assertThatThrownBy(() -> service.create(payment)).isInstanceOf(ServiceException.class).hasMessageContaining("大于 0");
        verify(mapper, never()).insertPayment(any());
    }

    @Test void rejectsAmountOverflow() {
        PaymentLine second = line(1L); second.setInvoiceId(301L);
        Payment payment = draft(); payment.setLines(List.of(line(Long.MAX_VALUE), second));
        when(mapper.selectSequence(anyString(), any())).thenReturn(2);
        assertThatThrownBy(() -> service.create(payment)).isInstanceOf(ServiceException.class).hasMessageContaining("超出");
        verify(mapper, never()).insertPayment(any());
    }

    private Payment draft() {
        Payment p = new Payment(); p.setId(100L); p.setSupplierId(200L); p.setCreatorId(20L);
        p.setStatus("DRAFT"); p.setRevision(0); return p;
    }
    private PaymentLine line(long cents) {
        PaymentLine line = new PaymentLine(); line.setInvoiceId(300L); line.setAllocatedCents(cents); return line;
    }
}
