package com.allinone.collect.service.impl;

import java.util.List;

import com.allinone.common.annotation.DataScope;
import com.allinone.common.utils.DateUtils;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.uuid.IdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.allinone.common.exception.ServiceException;
import com.allinone.common.utils.spring.SpringUtils;
import com.allinone.collect.mapper.WorkReportMapper;
import com.allinone.collect.domain.WorkReport;
import com.allinone.collect.service.IWorkReportService;
import com.allinone.collect.service.IWorkReportSheetService;
import com.allinone.collect.service.IWorkReportCellService;

@Service
public class WorkReportServiceImpl implements IWorkReportService
{
    @Autowired
    private WorkReportMapper workReportMapper;

    @Autowired
    private IWorkReportSheetService workReportSheetService;

    @Autowired
    private IWorkReportCellService workReportCellService;

    @Override
    public WorkReport selectWorkReportById(String id)
    {
        WorkReport query = new WorkReport();
        query.setId(id);
        return SpringUtils.getAopProxy(this).selectAccessibleWorkReport(query);
    }

    @Override
    @DataScope(deptAlias = "wr", userAlias = "wr")
    public WorkReport selectAccessibleWorkReport(WorkReport workReport)
    {
        return workReportMapper.selectWorkReportById(workReport);
    }

    @Override
    @DataScope(deptAlias = "wr", userAlias = "wr")
    public List<WorkReport> selectWorkReportList(WorkReport workReport)
    {
        return workReportMapper.selectWorkReportList(workReport);
    }

    @Override
    public int insertWorkReport(WorkReport workReport)
    {
        workReport.setId(IdUtils.fastUUID());
        workReport.setUserId(SecurityUtils.getUserId());
        workReport.setDeptId(SecurityUtils.getDeptId());
        workReport.setDelStatus(0L);
        workReport.setCreateTime(DateUtils.getNowDate());
        return workReportMapper.insertWorkReport(workReport);
    }

    @Override
    public int updateWorkReport(WorkReport workReport)
    {
        requireAccessible(workReport.getId());
        workReport.setUserId(null);
        workReport.setDeptId(null);
        workReport.setDelStatus(null);
        workReport.setCreateTime(null);
        workReport.setUpdateTime(DateUtils.getNowDate());
        return workReportMapper.updateWorkReport(workReport);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWorkReportByIds(String[] ids)
    {
        for (String id : ids) {
            requireAccessible(id);
            workReportCellService.deleteCellsByReportId(id);
            workReportSheetService.deleteWorkReportSheetByReportId(id);
        }
        return workReportMapper.deleteWorkReportByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWorkReportById(String id)
    {
        requireAccessible(id);
        workReportCellService.deleteCellsByReportId(id);
        workReportSheetService.deleteWorkReportSheetByReportId(id);
        return workReportMapper.deleteWorkReportById(id);
    }

    private void requireAccessible(String id)
    {
        if (id == null || selectWorkReportById(id) == null)
        {
            throw new ServiceException("报表不存在或无权访问");
        }
    }
}
