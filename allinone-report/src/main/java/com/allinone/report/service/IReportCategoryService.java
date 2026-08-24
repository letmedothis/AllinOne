package com.allinone.report.service;

import com.allinone.report.domain.ReportCategory;
import java.util.List;

public interface IReportCategoryService {
    List<ReportCategory> selectReportCategoryList(ReportCategory category);
    ReportCategory selectReportCategoryById(Long categoryId);
    int insertReportCategory(ReportCategory category);
    int updateReportCategory(ReportCategory category);
    int deleteReportCategoryByIds(Long[] categoryIds);
}

