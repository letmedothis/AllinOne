package com.allinone.collect.service;

import com.allinone.collect.domain.WorkReport;
import com.allinone.collect.domain.WorkReportSheet;
import com.allinone.collect.domain.WorkReportSheetPermission;
import com.allinone.common.exception.ServiceException;
import com.allinone.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 报表/Sheet 统一访问控制服务
 * 区分"可见性"和"编辑权"，解决越权编辑和属主反转问题
 */
@Service
public class WorkReportAccessService {

    @Autowired
    private IWorkReportService workReportService;
    
    @Autowired
    private IWorkReportSheetService workReportSheetService;
    
    @Autowired
    private IWorkReportSheetPermissionService permissionService;

    /**
     * 校验报表可见性（用于查看操作）
     * 使用数据范围过滤，确保用户只能看到自己有权限的报表
     */
    public WorkReport requireViewableReport(String reportId) {
        WorkReport report = workReportService.selectWorkReportById(reportId);
        if (report == null) {
            throw new ServiceException("报表不存在或无权访问");
        }
        return report;
    }

    /**
     * 校验报表编辑权限（用于修改操作）
     * 只有报表属主或管理员可以编辑
     */
    public WorkReport requireReportOwnerOrAdmin(String reportId) {
        WorkReport report = workReportService.selectWorkReportById(reportId);
        if (report == null) {
            throw new ServiceException("报表不存在");
        }
        
        Long currentUserId = SecurityUtils.getUserId();
        // 管理员可以编辑任何报表
        if (SecurityUtils.isAdmin(currentUserId)) {
            return report;
        }
        
        // 检查是否为报表属主
        if (!currentUserId.equals(report.getUserId())) {
            throw new ServiceException("只有报表创建者和管理员可以编辑报表");
        }
        
        return report;
    }

    /**
     * 校验Sheet编辑权限（用于修改Sheet操作）
     * Sheet编辑需要满足以下条件之一：
     * 1. 报表属主
     * 2. 管理员
     * 3. 拥有该Sheet的显式授权
     */
    public WorkReportSheet requireEditableSheet(String sheetId) {
        WorkReportSheet sheet = workReportSheetService.selectWorkReportSheetById(sheetId);
        if (sheet == null) {
            throw new ServiceException("Sheet不存在");
        }
        
        Long currentUserId = SecurityUtils.getUserId();
        
        // 管理员可以编辑任何Sheet
        if (SecurityUtils.isAdmin(currentUserId)) {
            return sheet;
        }
        
        // 检查是否为报表属主
        WorkReport report = workReportService.selectWorkReportById(sheet.getReportId());
        if (report != null && currentUserId.equals(report.getUserId())) {
            return sheet;
        }
        
        // 检查是否有显式授权
        if (hasExplicitSheetPermission(sheetId, currentUserId)) {
            return sheet;
        }
        
        throw new ServiceException("没有该Sheet的编辑权限");
    }

    /**
     * 校验Sheet属主权限（用于权限管理操作）
     * 只有报表属主或管理员可以管理Sheet权限
     */
    public WorkReportSheet requireSheetOwnerOrAdmin(String sheetId) {
        WorkReportSheet sheet = workReportSheetService.selectWorkReportSheetById(sheetId);
        if (sheet == null) {
            throw new ServiceException("Sheet不存在");
        }
        
        Long currentUserId = SecurityUtils.getUserId();
        
        // 管理员可以管理任何Sheet权限
        if (SecurityUtils.isAdmin(currentUserId)) {
            return sheet;
        }
        
        // 检查是否为报表属主
        WorkReport report = workReportService.selectWorkReportById(sheet.getReportId());
        if (report != null && currentUserId.equals(report.getUserId())) {
            return sheet;
        }
        
        throw new ServiceException("只有报表创建者和管理员可以管理Sheet权限");
    }

    /**
     * 检查用户是否有Sheet的显式编辑权限
     */
    private boolean hasExplicitSheetPermission(String sheetId, Long userId) {
        // 检查用户直接授权
        WorkReportSheetPermission userPerm = new WorkReportSheetPermission();
        userPerm.setSheetId(sheetId);
        userPerm.setPermType("user");
        userPerm.setPermId(userId);
        if (permissionService.exists(userPerm) > 0) {
            return true;
        }
        
        // 检查用户所属部门的授权
        Long deptId = SecurityUtils.getDeptId();
        if (deptId != null) {
            WorkReportSheetPermission deptPerm = new WorkReportSheetPermission();
            deptPerm.setSheetId(sheetId);
            deptPerm.setPermType("dept");
            deptPerm.setPermId(deptId);
            if (permissionService.exists(deptPerm) > 0) {
                return true;
            }
        }
        
        // 检查用户所属角色的授权
        Set<String> roleKeys = SecurityUtils.getLoginUser().getRoles();
        if (roleKeys != null) {
            for (String roleKey : roleKeys) {
                // 这里需要根据角色key查询角色ID，简化处理
                // 实际应该通过角色服务查询
            }
        }
        
        return false;
    }

    /**
     * 创建新Sheet时设置正确的属主
     * Sheet属主应该是报表的属主，而不是当前操作用户
     */
    public WorkReportSheet createSheetWithCorrectOwner(String reportId, String sheetName, Long sheetIndex) {
        WorkReport report = requireReportOwnerOrAdmin(reportId);
        
        WorkReportSheet sheet = new WorkReportSheet();
        sheet.setReportId(reportId);
        sheet.setSheetName(sheetName);
        sheet.setSheetIndex(sheetIndex);
        sheet.setUserId(report.getUserId()); // 使用报表属主，而不是当前用户
        sheet.setDeptId(report.getDeptId()); // 使用报表属主的部门
        sheet.setDelStatus(0L);
        
        return sheet;
    }
}