package com.allinone.supply.mapper;
import com.allinone.supply.domain.SupplyDashboard; import org.apache.ibatis.annotations.Param;
public interface SupplyDashboardMapper { SupplyDashboard selectSummary(@Param("mode") String mode,@Param("userId") Long userId,@Param("deptId") Long deptId); }
