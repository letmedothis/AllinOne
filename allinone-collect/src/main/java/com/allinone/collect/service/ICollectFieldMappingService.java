package com.allinone.collect.service;

import com.allinone.collect.domain.CollectFieldMapping;
import java.util.List;

public interface ICollectFieldMappingService {
    List<CollectFieldMapping> selectCollectFieldMappingList(CollectFieldMapping mapping);
    CollectFieldMapping selectCollectFieldMappingById(Long mappingId);
    List<CollectFieldMapping> selectCollectFieldMappingByTemplate(Long templateId);
    int insertCollectFieldMapping(CollectFieldMapping mapping);
    int updateCollectFieldMapping(CollectFieldMapping mapping);
    int deleteCollectFieldMappingByIds(Long[] mappingIds);
}
