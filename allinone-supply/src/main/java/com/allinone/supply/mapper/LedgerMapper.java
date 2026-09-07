package com.allinone.supply.mapper;
import com.allinone.supply.domain.LedgerRow; import java.util.List; import org.apache.ibatis.annotations.Param;
public interface LedgerMapper { List<LedgerRow> selectCombined(@Param("mode") String mode,@Param("userId") Long userId,@Param("deptId") Long deptId,@Param("number") String number,@Param("supplierName") String supplierName,@Param("approvalStatus") String approvalStatus); }
