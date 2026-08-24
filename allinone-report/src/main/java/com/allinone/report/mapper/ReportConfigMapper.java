package com.allinone.report.mapper;

import com.allinone.report.domain.ReportConfig;
import java.util.List;

public interface ReportConfigMapper {
    List<ReportConfig> selectReportConfigList(ReportConfig config);
    ReportConfig selectReportConfigById(Long reportId);
    int insertReportConfig(ReportConfig config);
    int updateReportConfig(ReportConfig config);
    int deleteReportConfigById(Long reportId);
    int deleteReportConfigByIds(Long[] reportIds);
}

