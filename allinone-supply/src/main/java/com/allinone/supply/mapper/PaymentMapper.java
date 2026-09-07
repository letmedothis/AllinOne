package com.allinone.supply.mapper;

import com.allinone.supply.domain.ApBalanceRow;
import com.allinone.supply.domain.Payment;
import com.allinone.supply.domain.PaymentLine;
import com.allinone.supply.domain.SupplierOption;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface PaymentMapper {
    List<SupplierOption> selectApprovedSupplierOptions();
    List<Payment> selectList(Payment payment);
    Payment selectById(@Param("id") Long id);
    List<PaymentLine> selectLines(@Param("paymentId") Long paymentId);
    int insertPayment(Payment payment);
    int insertPaymentLine(PaymentLine line);
    int deleteLines(@Param("paymentId") Long paymentId);
    int updatePayment(Payment payment);
    int updateState(@Param("id") Long id, @Param("revision") Integer revision, @Param("status") String status,
                    @Param("node") String node, @Param("reviewComment") String reviewComment,
                    @Param("directorComment") String directorComment, @Param("updateTime") Date updateTime);

    int insertSequence(@Param("type") String type, @Param("businessDate") Date businessDate);
    int incrementSequence(@Param("type") String type, @Param("businessDate") Date businessDate);
    Integer selectSequence(@Param("type") String type, @Param("businessDate") Date businessDate);

    int selectSupplierApproved(@Param("supplierId") Long supplierId);
    Long selectInvoiceSupplierId(@Param("invoiceId") Long invoiceId);
    Long lockApprovedInvoice(@Param("invoiceId") Long invoiceId);
    Long selectAllocatedForInvoice(@Param("invoiceId") Long invoiceId, @Param("excludePaymentId") Long excludePaymentId);
    int countReviewEligible(@Param("userId") Long userId, @Param("excludeId") Long excludeId);
    int countDirectorEligible(@Param("userId") Long userId, @Param("excludeId") Long excludeId);

    List<ApBalanceRow> selectApBalance(@Param("supplierId") Long supplierId);
}
