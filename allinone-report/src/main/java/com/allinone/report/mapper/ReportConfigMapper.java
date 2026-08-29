package com.allinone.report.mapper;

import com.allinone.report.domain.ReportConfig;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ReportConfigMapper {
    List<ReportConfig> selectReportConfigList(ReportConfig config);
    ReportConfig selectReportConfigById(Long reportId);
    /** 统计编码冲突数:唯一索引覆盖全部行(含软删),excludeId 用于更新时排除自身 */
    int countReportCodeConflict(@Param("reportCode") String reportCode, @Param("excludeId") Long excludeId);
    int insertReportConfig(ReportConfig config);
    int updateReportConfig(ReportConfig config);
    int deleteReportConfigById(Long reportId);
    int deleteReportConfigByIds(Long[] reportIds);
}

