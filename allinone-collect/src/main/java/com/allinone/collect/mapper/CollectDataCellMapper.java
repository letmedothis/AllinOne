package com.allinone.collect.mapper;

import com.allinone.collect.domain.CollectDataCell;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface CollectDataCellMapper {
    int deleteCollectDataCellByDataId(Long dataId);
    int batchUpsert(List<CollectDataCell> cells);
    List<CollectDataCell> selectCollectDataCellByDataId(Long dataId);
    /** 按填报记录 ID 批量查询单元格快照，用于导出时一次取回并按 data_id 分组，避免逐条记录查询 */
    List<CollectDataCell> selectCollectDataCellByDataIds(@Param("dataIds") List<Long> dataIds);
}
