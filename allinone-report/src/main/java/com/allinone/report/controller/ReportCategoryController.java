package com.allinone.report.controller;

import com.allinone.common.annotation.Log;
import com.allinone.common.core.controller.BaseController;
import com.allinone.common.core.domain.AjaxResult;
import com.allinone.common.enums.BusinessType;
import com.allinone.report.domain.ReportCategory;
import com.allinone.report.service.IReportCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 报表分类管理
 */
@RestController
@RequestMapping("/report/category")
public class ReportCategoryController extends BaseController {

    @Autowired
    private IReportCategoryService reportCategoryService;

    @PreAuthorize("@ss.hasPermi('report:category:list')")
    @GetMapping("/list")
    public AjaxResult list(ReportCategory category) {
        List<ReportCategory> list = reportCategoryService.selectReportCategoryList(category);
        return success(list);
    }

    @PreAuthorize("@ss.hasPermi('report:category:add')")
    @Log(title = "报表分类", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ReportCategory category) {
        int rows = reportCategoryService.insertReportCategory(category);
        return rows > 0 ? success(category) : error("新增报表分类失败");
    }

    @PreAuthorize("@ss.hasPermi('report:category:edit')")
    @Log(title = "报表分类", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ReportCategory category) {
        return toAjax(reportCategoryService.updateReportCategory(category));
    }

    @PreAuthorize("@ss.hasPermi('report:category:remove')")
    @Log(title = "报表分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{categoryIds}")
    public AjaxResult remove(@PathVariable Long[] categoryIds) {
        return toAjax(reportCategoryService.deleteReportCategoryByIds(categoryIds));
    }
}
