package com.allinone.collect.mapper;

import com.allinone.collect.domain.CollectDataCell;
import java.util.List;

public interface CollectDataCellMapper {
    int deleteCollectDataCellByDataId(Long dataId);
    int batchUpsert(List<CollectDataCell> cells);
    List<CollectDataCell> selectCollectDataCellByDataId(Long dataId);
}
