package com.allinone.report.mapper;

import com.allinone.report.domain.ReportCategory;
import java.util.List;

public interface ReportCategoryMapper {
    List<ReportCategory> selectReportCategoryList(ReportCategory category);
    ReportCategory selectReportCategoryById(Long categoryId);
    int insertReportCategory(ReportCategory category);
    int updateReportCategory(ReportCategory category);
    int deleteReportCategoryById(Long categoryId);
    int countReportConfigByCategoryId(Long categoryId);

    /** 统计所选分类下仍存在的子分类数（删除保护） */
    int countCategoryChildByCategoryIds(Long[] categoryIds);
}

