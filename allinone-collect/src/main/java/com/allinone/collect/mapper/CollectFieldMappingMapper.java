package com.allinone.collect.mapper;

import com.allinone.collect.domain.CollectFieldMapping;
import java.util.List;

public interface CollectFieldMappingMapper {
    List<CollectFieldMapping> selectCollectFieldMappingList(CollectFieldMapping mapping);
    CollectFieldMapping selectCollectFieldMappingById(Long mappingId);
    List<CollectFieldMapping> selectCollectFieldMappingByTemplate(Long templateId);
    int insertCollectFieldMapping(CollectFieldMapping mapping);
    int updateCollectFieldMapping(CollectFieldMapping mapping);
    int deleteCollectFieldMappingByIds(Long[] mappingIds);
}
