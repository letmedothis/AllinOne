package com.allinone.collect.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.allinone.collect.mapper.WorkReportSheetPermissionMapper;
import com.allinone.collect.domain.WorkReportSheetPermission;
import com.allinone.collect.service.IWorkReportSheetPermissionService;

@Service
public class WorkReportSheetPermissionServiceImpl implements IWorkReportSheetPermissionService {

    @Autowired
    private WorkReportSheetPermissionMapper permissionMapper;

    @Override
    public void grant(WorkReportSheetPermission p) {
        p.setCreateTime(new java.util.Date());
        permissionMapper.insert(p);
    }

    @Override
    public void revoke(String sheetId, String permType, Long permId) {
        permissionMapper.deleteBySheetAndTarget(sheetId, permType, permId);
    }

    @Override
    public void revokeBySheet(String sheetId) {
        permissionMapper.deleteBySheetId(sheetId);
    }

    @Override
    public List<WorkReportSheetPermission> listBySheet(String sheetId) {
        return permissionMapper.selectBySheetId(sheetId);
    }
}
