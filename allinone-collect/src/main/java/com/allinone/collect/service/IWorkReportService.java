package com.allinone.collect.service;

import com.allinone.collect.domain.WorkReport;
import java.util.List;

public interface IWorkReportService
{
    WorkReport selectWorkReportById(String id);
    WorkReport selectAccessibleWorkReport(WorkReport workReport);
    List<WorkReport> selectWorkReportList(WorkReport workReport);
    int insertWorkReport(WorkReport workReport);
    int updateWorkReport(WorkReport workReport);
    int deleteWorkReportById(String id);
    int deleteWorkReportByIds(String[] ids);
}
