package com.allinone.collect.mapper;

import com.allinone.collect.domain.CollectDataCell;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

public interface CollectDataCellMapper {
    int deleteCollectDataCellByDataId(Long dataId);
    int batchUpsert(List<CollectDataCell> cells);
    List<CollectDataCell> selectCollectDataCellByDataId(Long dataId);
    /**
     * 按填报记录 ID 统计单元格快照数（dataId/cellCount 两列），
     * 导出前先计数，避免把超额快照整体载入内存后才判断上限
     */
    List<Map<String, Object>> countCollectDataCellByDataIds(@Param("dataIds") List<Long> dataIds);
}
