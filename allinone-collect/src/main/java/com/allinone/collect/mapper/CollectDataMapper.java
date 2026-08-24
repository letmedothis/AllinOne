package com.allinone.collect.mapper;

import com.allinone.collect.domain.CollectData;
import com.allinone.collect.domain.CollectDataCell;
import java.util.List;

public interface CollectDataMapper {
    List<CollectData> selectCollectDataList(CollectData data);
    CollectData selectCollectDataById(Long dataId);
    int insertCollectData(CollectData data);
    int updateCollectData(CollectData data);
    int updateCollectDataStatus(CollectData data);
    int deleteCollectDataById(Long dataId);
    int deleteCollectDataByIds(Long[] dataIds);
}

