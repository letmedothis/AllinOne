package com.allinone.collect.service.impl;

import com.allinone.collect.domain.CollectDataCell;
import com.allinone.collect.mapper.CollectDataCellMapper;
import com.allinone.collect.service.ICollectDataCellService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CollectDataCellServiceImpl implements ICollectDataCellService {

    @Autowired
    private CollectDataCellMapper collectDataCellMapper;

    @Override
    public int batchUpsert(List<CollectDataCell> cells) {
        return collectDataCellMapper.batchUpsert(cells);
    }

    @Override
    public List<CollectDataCell> selectCollectDataCellByDataId(Long dataId) {
        return collectDataCellMapper.selectCollectDataCellByDataId(dataId);
    }

    @Override
    public int deleteCollectDataCellByDataId(Long dataId) {
        return collectDataCellMapper.deleteCollectDataCellByDataId(dataId);
    }
}
