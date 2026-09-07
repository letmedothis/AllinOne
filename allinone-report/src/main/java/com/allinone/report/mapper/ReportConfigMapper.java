package com.allinone.report.mapper;

import com.allinone.report.domain.ReportConfig;
import com.allinone.report.domain.JimuReportDefinition;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ReportConfigMapper {
    List<ReportConfig> selectReportConfigList(ReportConfig config);
    ReportConfig selectReportConfigById(Long reportId);
    /** 统计编码冲突数:唯一索引覆盖全部行(含软删),excludeId 用于更新时排除自身 */
    int countReportCodeConflict(@Param("reportCode") String reportCode, @Param("excludeId") Long excludeId);
    /** 读取引擎内仍可配置的 JimuReport 报表目录。 */
    List<JimuReportDefinition> selectJimuReportDefinitionList();
    /** 校验报表 ID 是否存在且未被引擎逻辑删除。 */
    int countJimuReportDefinitionById(String id);
    int insertReportConfig(ReportConfig config);
    int updateReportConfig(ReportConfig config);
    int deleteReportConfigById(Long reportId);
    int deleteReportConfigByIds(Long[] reportIds);
}

