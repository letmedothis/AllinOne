package com.allinone.collect.mapper;

import com.allinone.collect.domain.WorkReport;
import java.util.List;

public interface WorkReportMapper
{
    WorkReport selectWorkReportById(String id);
    List<WorkReport> selectWorkReportList(WorkReport workReport);
    int insertWorkReport(WorkReport workReport);
    int updateWorkReport(WorkReport workReport);
    int deleteWorkReportById(String id);
    int deleteWorkReportByIds(String[] ids);
}
