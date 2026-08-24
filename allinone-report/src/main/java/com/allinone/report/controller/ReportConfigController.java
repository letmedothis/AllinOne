package com.allinone.report.controller;

import com.allinone.common.annotation.Log;
import com.allinone.common.core.controller.BaseController;
import com.allinone.common.core.domain.AjaxResult;
import com.allinone.common.core.page.TableDataInfo;
import com.allinone.common.enums.BusinessType;
import com.allinone.report.domain.ReportConfig;
import com.allinone.report.service.IReportConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/report/config")
public class ReportConfigController extends BaseController {

    @Autowired
    private IReportConfigService reportConfigService;

    @PreAuthorize("@ss.hasPermi('report:config:list')")
    @GetMapping("/list")
    public TableDataInfo list(ReportConfig config) {
        startPage();
        List<ReportConfig> list = reportConfigService.selectReportConfigList(config);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('report:config:query')")
    @GetMapping("/{reportId}")
    public AjaxResult getInfo(@PathVariable Long reportId) {
        return success(reportConfigService.selectReportConfigById(reportId));
    }

    @PreAuthorize("@ss.hasPermi('report:config:add')")
    @Log(title = "报表配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ReportConfig config) {
        return toAjax(reportConfigService.insertReportConfig(config));
    }

    @PreAuthorize("@ss.hasPermi('report:config:edit')")
    @Log(title = "报表配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ReportConfig config) {
        return toAjax(reportConfigService.updateReportConfig(config));
    }

    @PreAuthorize("@ss.hasPermi('report:config:remove')")
    @Log(title = "报表配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{reportIds}")
    public AjaxResult remove(@PathVariable Long[] reportIds) {
        return toAjax(reportConfigService.deleteReportConfigByIds(reportIds));
    }
}

