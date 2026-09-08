package com.allinone.supply.mapper;

import com.allinone.supply.domain.OpeningPayable;
import java.util.Map;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface OpeningPayableMapper {
    Map<String, Object> company();
    int insert(OpeningPayable row);
    OpeningPayable select(@Param("id") Long id);
    List<OpeningPayable> list(@Param("userId") Long userId);
    int update(OpeningPayable row);
}
