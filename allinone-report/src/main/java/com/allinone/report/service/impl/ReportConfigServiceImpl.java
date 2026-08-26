package com.allinone.report.service.impl;

import com.allinone.common.utils.DateUtils;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.StringUtils;
import com.allinone.common.utils.uuid.IdUtils;
import com.allinone.report.domain.ReportConfig;
import com.allinone.report.mapper.ReportConfigMapper;
import com.allinone.report.service.IReportConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ReportConfigServiceImpl implements IReportConfigService {

    /** 报表类型：0报表 1大屏 2仪表盘 */
    private static final String TYPE_REPORT = "0";

    @Autowired
    private ReportConfigMapper reportConfigMapper;

    @Override
    public List<ReportConfig> selectReportConfigList(ReportConfig config) {
        List<ReportConfig> list = reportConfigMapper.selectReportConfigList(config);
        if (list != null) {
            list.forEach(this::fillUrl);
        }
        return list;
    }

    @Override
    public ReportConfig selectReportConfigById(Long reportId) {
        ReportConfig config = reportConfigMapper.selectReportConfigById(reportId);
        if (config != null) {
            fillUrl(config);
        }
        return config;
    }

    @Override
    public int insertReportConfig(ReportConfig config) {
        config.setReportId(IdUtils.nextLongId());
        config.setCreateBy(SecurityUtils.getUsername());
        config.setCreateTime(DateUtils.getNowDate());
        return reportConfigMapper.insertReportConfig(config);
    }

    @Override
    public int updateReportConfig(ReportConfig config) {
        config.setUpdateBy(SecurityUtils.getUsername());
        config.setUpdateTime(DateUtils.getNowDate());
        return reportConfigMapper.updateReportConfig(config);
    }

    @Override
    public int deleteReportConfigByIds(Long[] reportIds) {
        return reportConfigMapper.deleteReportConfigByIds(reportIds);
    }

    /**
     * 按报表类型计算访问 URL（iframe 地址，不含 token；token 由前端在加载时拼接）。
     * 报表：/jmreport/view/{jimu_report_id}
     * 大屏/仪表盘：/jimubi/view?pageId={jmbi_id}
     */
    private void fillUrl(ReportConfig config) {
        if (TYPE_REPORT.equals(config.getReportType())) {
            if (StringUtils.isNotEmpty(config.getJimuReportId())) {
                config.setUrl("/jmreport/view/" + config.getJimuReportId());
            }
        } else {
            if (StringUtils.isNotEmpty(config.getJmbiId())) {
                config.setUrl("/jimubi/view?pageId=" + config.getJmbiId());
            }
        }
    }
}
