package com.allinone.collect.service.impl;

import com.allinone.collect.domain.CollectFieldMapping;
import com.allinone.collect.mapper.CollectFieldMappingMapper;
import com.allinone.collect.service.ICollectFieldMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.allinone.common.utils.DateUtils;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.uuid.IdUtils;
import java.util.List;

@Service
public class CollectFieldMappingServiceImpl implements ICollectFieldMappingService {

    private static final WriteBackValueConverter VALUE_CONVERTER = new WriteBackValueConverter();

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
        mapping.setMappingId(IdUtils.nextLongId());
        if (mapping.getSheetIndex() == null) mapping.setSheetIndex(0);
        mapping.setDataType(VALUE_CONVERTER.normalizeType(mapping.getDataType()));
        mapping.setCreateBy(SecurityUtils.getUsername());
        mapping.setCreateTime(DateUtils.getNowDate());
        return collectFieldMappingMapper.insertCollectFieldMapping(mapping);
    }

    @Override
    public int updateCollectFieldMapping(CollectFieldMapping mapping) {
        mapping.setDataType(VALUE_CONVERTER.normalizeType(mapping.getDataType()));
        mapping.setUpdateBy(SecurityUtils.getUsername());
        mapping.setUpdateTime(DateUtils.getNowDate());
        return collectFieldMappingMapper.updateCollectFieldMapping(mapping);
    }

    @Override
    public int deleteCollectFieldMappingByIds(Long[] mappingIds) {
        return collectFieldMappingMapper.deleteCollectFieldMappingByIds(mappingIds);
    }
}
