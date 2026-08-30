package com.allinone.report.service.impl;

import com.allinone.common.utils.DateUtils;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.uuid.IdUtils;
import com.allinone.common.exception.ServiceException;
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
        category.setCategoryId(IdUtils.nextLongId());
        category.setCreateBy(SecurityUtils.getUsername());
        category.setCreateTime(DateUtils.getNowDate());
        return reportCategoryMapper.insertReportCategory(category);
    }

    @Override
    public int updateReportCategory(ReportCategory category) {
        category.setUpdateBy(SecurityUtils.getUsername());
        category.setUpdateTime(DateUtils.getNowDate());
        return reportCategoryMapper.updateReportCategory(category);
    }

    @Override
    public int deleteReportCategoryByIds(Long[] categoryIds) {
        // 删除保护：存在子分类时先删子分类，避免树断裂
        if (reportCategoryMapper.countCategoryChildByCategoryIds(categoryIds) > 0) {
            throw new ServiceException("所选分类下存在子分类，请先删除子分类");
        }
        int count = 0;
        for (Long id : categoryIds) {
            if (reportCategoryMapper.countReportConfigByCategoryId(id) > 0) {
                throw new ServiceException("该分类下存在报表配置，无法删除");
            }
            count += reportCategoryMapper.deleteReportCategoryById(id);
        }
        return count;
    }
}

