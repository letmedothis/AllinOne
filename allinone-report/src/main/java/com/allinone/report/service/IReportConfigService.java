package com.allinone.report.service;

import com.allinone.report.domain.ReportConfig;
import java.util.List;

public interface IReportConfigService {
    List<ReportConfig> selectReportConfigList(ReportConfig config);
    ReportConfig selectReportConfigById(Long reportId);
    int insertReportConfig(ReportConfig config);
    int updateReportConfig(ReportConfig config);
    int deleteReportConfigByIds(Long[] reportIds);
}

