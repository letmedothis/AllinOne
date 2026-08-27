package com.allinone.collect.service;

import com.allinone.collect.domain.WorkReportSheetPermission;
import java.util.List;

public interface IWorkReportSheetPermissionService
{
    void grant(WorkReportSheetPermission permission);
    void revoke(String sheetId, String permType, Long permId);
    void revokeBySheet(String sheetId);
    List<WorkReportSheetPermission> listBySheet(String sheetId);
    int exists(String sheetId, String permType, Long permId);
}
