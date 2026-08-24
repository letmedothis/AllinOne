package com.allinone.report.service.impl;

import com.allinone.common.utils.DateUtils;
import com.allinone.report.domain.ReportConfig;
import com.allinone.report.mapper.ReportConfigMapper;
import com.allinone.report.service.IReportConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ReportConfigServiceImpl implements IReportConfigService {

    @Autowired
    private ReportConfigMapper reportConfigMapper;

    @Override
    public List<ReportConfig> selectReportConfigList(ReportConfig config) {
        return reportConfigMapper.selectReportConfigList(config);
    }

    @Override
    public ReportConfig selectReportConfigById(Long reportId) {
        return reportConfigMapper.selectReportConfigById(reportId);
    }

    @Override
    public int insertReportConfig(ReportConfig config) {
        config.setCreateTime(DateUtils.getNowDate());
        return reportConfigMapper.insertReportConfig(config);
    }

    @Override
    public int updateReportConfig(ReportConfig config) {
        config.setUpdateTime(DateUtils.getNowDate());
        return reportConfigMapper.updateReportConfig(config);
    }

    @Override
    public int deleteReportConfigByIds(Long[] reportIds) {
        return reportConfigMapper.deleteReportConfigByIds(reportIds);
    }
}

