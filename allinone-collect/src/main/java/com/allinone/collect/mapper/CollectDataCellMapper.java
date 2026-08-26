package com.allinone.collect.mapper;

import com.allinone.collect.domain.CollectDataCell;
import java.util.List;

public interface CollectDataCellMapper {
    int batchUpsert(List<CollectDataCell> cells);
    List<CollectDataCell> selectCollectDataCellByDataId(Long dataId);
}

