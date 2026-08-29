package com.allinone.report.service.impl;

import com.allinone.common.exception.ServiceException;
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
        checkCodeConflict(config.getReportCode(), null);
        config.setReportId(IdUtils.nextLongId());
        config.setCreateBy(SecurityUtils.getUsername());
        config.setCreateTime(DateUtils.getNowDate());
        return reportConfigMapper.insertReportConfig(config);
    }

    @Override
    public int updateReportConfig(ReportConfig config) {
        checkCodeConflict(config.getReportCode(), config.getReportId());
        config.setUpdateBy(SecurityUtils.getUsername());
        config.setUpdateTime(DateUtils.getNowDate());
        return reportConfigMapper.updateReportConfig(config);
    }

    @Override
    public int deleteReportConfigByIds(Long[] reportIds) {
        return reportConfigMapper.deleteReportConfigByIds(reportIds);
    }

    /**
     * 报表编码前置查重:report_code 上的唯一索引覆盖全部行(含软删),
     * 不拦截会让重复插入以"系统未知错误"形式抛出原始 DuplicateKeyException
     */
    private void checkCodeConflict(String reportCode, Long excludeId) {
        if (StringUtils.isEmpty(reportCode)) {
            return;
        }
        if (reportConfigMapper.countReportCodeConflict(reportCode, excludeId) > 0) {
            throw new ServiceException("报表编码已存在(已删除报表的编码同样占用),请更换编码");
        }
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
