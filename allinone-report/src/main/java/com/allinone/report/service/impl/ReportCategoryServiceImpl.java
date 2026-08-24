package com.allinone.report.service.impl;

import com.allinone.common.utils.DateUtils;
import com.allinone.report.domain.ReportCategory;
import com.allinone.report.mapper.ReportCategoryMapper;
import com.allinone.report.service.IReportCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ReportCategoryServiceImpl implements IReportCategoryService {

    @Autowired
    private ReportCategoryMapper reportCategoryMapper;

    @Override
    public List<ReportCategory> selectReportCategoryList(ReportCategory category) {
        return reportCategoryMapper.selectReportCategoryList(category);
    }

    @Override
    public ReportCategory selectReportCategoryById(Long categoryId) {
        return reportCategoryMapper.selectReportCategoryById(categoryId);
    }

    @Override
    public int insertReportCategory(ReportCategory category) {
        category.setCreateTime(DateUtils.getNowDate());
        return reportCategoryMapper.insertReportCategory(category);
    }

    @Override
    public int updateReportCategory(ReportCategory category) {
        category.setUpdateTime(DateUtils.getNowDate());
        return reportCategoryMapper.updateReportCategory(category);
    }

    @Override
    public int deleteReportCategoryByIds(Long[] categoryIds) {
        int count = 0;
        for (Long id : categoryIds) {
            count += reportCategoryMapper.deleteReportCategoryById(id);
        }
        return count;
    }
}

