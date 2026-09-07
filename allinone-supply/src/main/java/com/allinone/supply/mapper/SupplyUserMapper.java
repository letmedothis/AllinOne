package com.allinone.supply.mapper;

import org.apache.ibatis.annotations.Param;

public interface SupplyUserMapper {
    /** 读取用户职级(EXEC/LEADER/STAFF),未配置返回 null。 */
    String selectRankLevel(@Param("userId") Long userId);
}
