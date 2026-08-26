package com.allinone.collect.mapper;

import com.allinone.collect.domain.CollectTemplate;
import java.util.List;

public interface CollectTemplateMapper {
    List<CollectTemplate> selectCollectTemplateList(CollectTemplate template);
    CollectTemplate selectCollectTemplateById(Long templateId);
    CollectTemplate selectCollectTemplateByCode(String templateCode);
    int insertCollectTemplate(CollectTemplate template);
    int updateCollectTemplate(CollectTemplate template);
    int updateCollectTemplateStatus(CollectTemplate template);
    int deleteCollectTemplateById(Long templateId);
    int deleteCollectTemplateByIds(Long[] templateIds);
    int countSubmittedDataByTemplateIds(Long[] templateIds);
    int countFieldMappingByTemplateIds(Long[] templateIds);
}

