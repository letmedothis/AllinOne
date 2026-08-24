package com.allinone.collect.service.impl;

import com.allinone.collect.domain.CollectFieldMapping;
import com.allinone.collect.mapper.CollectFieldMappingMapper;
import com.allinone.collect.service.ICollectFieldMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CollectFieldMappingServiceImpl implements ICollectFieldMappingService {

    @Autowired
    private CollectFieldMappingMapper collectFieldMappingMapper;

    @Override
    public List<CollectFieldMapping> selectCollectFieldMappingList(CollectFieldMapping mapping) {
        return collectFieldMappingMapper.selectCollectFieldMappingList(mapping);
    }

    @Override
    public CollectFieldMapping selectCollectFieldMappingById(Long mappingId) {
        return collectFieldMappingMapper.selectCollectFieldMappingById(mappingId);
    }

    @Override
    public List<CollectFieldMapping> selectCollectFieldMappingByTemplate(Long templateId) {
        return collectFieldMappingMapper.selectCollectFieldMappingByTemplate(templateId);
    }

    @Override
    public int insertCollectFieldMapping(CollectFieldMapping mapping) {
        return collectFieldMappingMapper.insertCollectFieldMapping(mapping);
    }

    @Override
    public int updateCollectFieldMapping(CollectFieldMapping mapping) {
        return collectFieldMappingMapper.updateCollectFieldMapping(mapping);
    }

    @Override
    public int deleteCollectFieldMappingByIds(Long[] mappingIds) {
        return collectFieldMappingMapper.deleteCollectFieldMappingByIds(mappingIds);
    }
}
