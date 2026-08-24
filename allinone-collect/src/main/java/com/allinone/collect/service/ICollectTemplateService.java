package com.allinone.collect.service;

import com.allinone.collect.domain.CollectTemplate;
import java.util.List;

public interface ICollectTemplateService {
    List<CollectTemplate> selectCollectTemplateList(CollectTemplate template);
    CollectTemplate selectCollectTemplateById(Long templateId);
    int insertCollectTemplate(CollectTemplate template);
    int updateCollectTemplate(CollectTemplate template);
    int updateStatus(Long templateId, String status);
    int deleteCollectTemplateByIds(Long[] templateIds);
}

