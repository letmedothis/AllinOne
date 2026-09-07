package com.allinone.supply.mapper;
import com.allinone.supply.domain.SupplyDashboard; import org.apache.ibatis.annotations.Param;
public interface SupplyDashboardMapper { SupplyDashboard selectSummary(@Param("userId") Long userId,@Param("admin") boolean admin); }
