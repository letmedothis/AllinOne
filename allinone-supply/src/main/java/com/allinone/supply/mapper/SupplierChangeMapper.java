package com.allinone.supply.mapper;
import com.allinone.supply.domain.Supplier;
import java.util.*;
import org.apache.ibatis.annotations.Param;
public interface SupplierChangeMapper {
    Long lockSupplier(@Param("id") Long id);
    int profileVersion(@Param("id") Long id);
    int insertVersion(@Param("id") Long id,@Param("version") int version,@Param("snapshot") String snapshot,@Param("caseId") Long caseId);
    int applySupplier(Supplier supplier);
    int bump(@Param("id") Long id);
    int blocked(@Param("id") Long id);
    int invalidateAccounts(@Param("id") Long id);
    int requireReview(@Param("id") Long id);
    int account(@Param("paymentId") Long paymentId,@Param("supplierId") Long supplierId,@Param("version") int version,@Param("snapshot") String snapshot);
    Map<String,Object> accountSnapshot(@Param("paymentId") Long paymentId);
    List<Map<String,Object>> history(@Param("id") Long id);
    List<Map<String,Object>> affected(@Param("id") Long id);
}
