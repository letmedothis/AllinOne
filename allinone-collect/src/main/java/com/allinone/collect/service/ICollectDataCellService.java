package com.allinone.collect.service;

import com.allinone.collect.domain.CollectDataCell;
import java.util.List;

public interface ICollectDataCellService {
    int batchUpsert(List<CollectDataCell> cells);
    List<CollectDataCell> selectCollectDataCellByDataId(Long dataId);
    int deleteCollectDataCellByDataId(Long dataId);
}
