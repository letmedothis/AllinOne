package com.allinone.collect.mapper;

import com.allinone.collect.domain.WorkReportSheet;
import java.util.List;

public interface WorkReportSheetMapper
{
    WorkReportSheet selectWorkReportSheetById(String id);
    List<WorkReportSheet> selectWorkReportSheetList(WorkReportSheet workReportSheet);
    List<WorkReportSheet> selectAccessibleSheets(WorkReportSheet workReportSheet);
    int insertWorkReportSheet(WorkReportSheet workReportSheet);
    int updateWorkReportSheet(WorkReportSheet workReportSheet);
    int deleteWorkReportSheetById(String id);
    int deleteWorkReportSheetByIds(String[] ids);
    int deleteWorkReportSheetByReportId(String reportId);
}
