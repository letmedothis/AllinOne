package com.allinone.collect.mapper;

import com.allinone.collect.domain.CollectTemplate;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface CollectTemplateMapper {
    List<CollectTemplate> selectCollectTemplateList(CollectTemplate template);
    CollectTemplate selectCollectTemplateById(Long templateId);
    CollectTemplate selectCollectTemplateByCode(String templateCode);
    /** 统计编码冲突数:唯一索引覆盖全部行(含软删),excludeId 用于更新时排除自身 */
    int countTemplateCodeConflict(@Param("templateCode") String templateCode, @Param("excludeId") Long excludeId);
    int insertCollectTemplate(CollectTemplate template);
    int updateCollectTemplate(CollectTemplate template);
    int updateCollectTemplateStatus(CollectTemplate template);
    int deleteCollectTemplateById(Long templateId);
    int deleteCollectTemplateByIds(Long[] templateIds);
    int countSubmittedDataByTemplateIds(Long[] templateIds);
    int countFieldMappingByTemplateIds(Long[] templateIds);
    int insertTemplateVersion(@Param("templateId") Long templateId, @Param("version") Integer version,
                              @Param("templateJson") String templateJson, @Param("templateName") String templateName,
                              @Param("status") String status,
                              @Param("createdBy") String createdBy, @Param("createdAt") java.util.Date createdAt);
    CollectTemplate selectTemplateVersion(@Param("templateId") Long templateId, @Param("version") Integer version);
}

