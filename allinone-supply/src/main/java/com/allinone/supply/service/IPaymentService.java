package com.allinone.supply.service;

import com.allinone.supply.domain.ApBalanceRow;
import com.allinone.supply.domain.Payment;
import com.allinone.supply.domain.PaymentDecision;
import com.allinone.supply.domain.SupplierOption;
import java.util.List;

public interface IPaymentService {
    List<SupplierOption> supplierOptions();
    List<Payment> list(Payment filter);
    Payment get(Long id);
    int create(Payment payment);
    int update(Payment payment);
    int submit(Long id);
    int review(Long id, PaymentDecision decision);
    int directorDecision(Long id, PaymentDecision decision);
    int voidPayment(Long id, String reason);
    int recordExecution(Long id, com.allinone.supply.domain.PaymentExecution execution);
    List<ApBalanceRow> apBalance(Long supplierId);
}
