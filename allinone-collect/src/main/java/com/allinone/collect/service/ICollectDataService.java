package com.allinone.collect.service;

import com.allinone.collect.domain.CollectData;
import java.util.List;

public interface ICollectDataService {
    List<CollectData> selectCollectDataList(CollectData data);
    CollectData selectCollectDataById(Long dataId);
    int insertCollectData(CollectData data);
    int updateCollectData(CollectData data);
    int submitData(Long dataId);
    int deleteCollectDataByIds(Long[] dataIds);
}

