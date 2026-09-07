package com.allinone.report.service;

import com.allinone.report.domain.ReportConfig;
import com.allinone.report.domain.JimuReportDefinition;
import java.util.List;

public interface IReportConfigService {
    List<ReportConfig> selectReportConfigList(ReportConfig config);
    ReportConfig selectReportConfigById(Long reportId);
    List<JimuReportDefinition> selectJimuReportDefinitionList();
    int insertReportConfig(ReportConfig config);
    int updateReportConfig(ReportConfig config);
    int deleteReportConfigByIds(Long[] reportIds);
}

