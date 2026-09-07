package com.allinone.supply.mapper;
import com.allinone.supply.domain.LedgerRow; import java.util.List; import org.apache.ibatis.annotations.Param;
public interface LedgerMapper { List<LedgerRow> selectCombined(@Param("userId") Long userId,@Param("admin") boolean admin,@Param("number") String number,@Param("supplierName") String supplierName,@Param("approvalStatus") String approvalStatus); }
