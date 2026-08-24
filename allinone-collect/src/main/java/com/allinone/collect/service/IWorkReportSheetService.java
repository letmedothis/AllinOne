package com.allinone.collect.service;

import com.allinone.collect.domain.WorkReportSheet;
import java.util.List;

public interface IWorkReportSheetService
{
    WorkReportSheet selectWorkReportSheetById(String id);
    List<WorkReportSheet> selectWorkReportSheetList(WorkReportSheet workReportSheet);
    List<WorkReportSheet> selectAccessibleSheets(WorkReportSheet workReportSheet);
    WorkReportSheet selectAccessibleSheetById(String id);
    int insertWorkReportSheet(WorkReportSheet workReportSheet);
    int updateWorkReportSheet(WorkReportSheet workReportSheet);
    int deleteWorkReportSheetById(String id);
    int deleteWorkReportSheetByIds(String[] ids);
    int deleteWorkReportSheetByReportId(String reportId);
}
