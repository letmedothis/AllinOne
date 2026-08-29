package com.allinone.collect.service;

import com.allinone.collect.domain.WorkReportSheet;
import java.util.Date;
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
    /** 乐观锁：仅当当前版本等于期望版本时递增并刷新更新时间，返回 0 表示已被他人修改 */
    int compareAndIncrementVersion(String id, Long expectedVersion, Date updateTime);
}
