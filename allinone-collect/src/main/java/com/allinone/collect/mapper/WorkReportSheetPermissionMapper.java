package com.allinone.collect.mapper;

import com.allinone.collect.domain.WorkReportSheetPermission;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface WorkReportSheetPermissionMapper
{
    int insert(WorkReportSheetPermission permission);
    int deleteBySheetAndTarget(@Param("sheetId") String sheetId, @Param("permType") String permType, @Param("permId") Long permId);
    int deleteBySheetId(@Param("sheetId") String sheetId);
    List<WorkReportSheetPermission> selectBySheetId(@Param("sheetId") String sheetId);
    List<WorkReportSheetPermission> selectByTarget(@Param("permType") String permType, @Param("permId") Long permId);
    int exists(@Param("sheetId") String sheetId, @Param("permType") String permType, @Param("permId") Long permId);
}
