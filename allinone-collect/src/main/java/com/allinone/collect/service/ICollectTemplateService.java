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

    /**
     * 复制模板：克隆定义与 Luckysheet JSON 为未发布新模板（名称加“-副本”，编码重新生成，版本从 1 计数）
     *
     * @return 新建的模板记录
     */
    CollectTemplate copyTemplate(Long templateId);
}

