package com.allinone.collect.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import com.allinone.common.annotation.DataScope;
import com.allinone.common.constant.Constants;
import com.allinone.common.constant.UserConstants;
import com.allinone.common.core.domain.entity.SysRole;
import com.allinone.common.core.domain.entity.SysUser;
import com.allinone.common.core.domain.model.LoginUser;
import com.allinone.common.utils.DateUtils;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.StringUtils;
import com.allinone.common.utils.uuid.IdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.allinone.collect.mapper.WorkReportSheetMapper;
import com.allinone.collect.domain.WorkReportSheet;
import com.allinone.collect.service.IWorkReportSheetService;
import com.allinone.collect.service.IWorkReportSheetPermissionService;

@Service
public class WorkReportSheetServiceImpl implements IWorkReportSheetService
{
    @Autowired
    private WorkReportSheetMapper workReportSheetMapper;

    @Autowired
    private IWorkReportSheetPermissionService permissionService;

    @Override
    public WorkReportSheet selectWorkReportSheetById(String id)
    {
        return workReportSheetMapper.selectWorkReportSheetById(id);
    }

    @Override
    @DataScope(deptAlias = "wrs", userAlias = "wrs")
    public List<WorkReportSheet> selectWorkReportSheetList(WorkReportSheet workReportSheet)
    {
        return workReportSheetMapper.selectWorkReportSheetList(workReportSheet);
    }

    /**
     * D3: 双层权限查询
     * 组合 @DataScope 规则 + 自己创建�?sheet + 显式分配的权�?     */
    @Override
    public List<WorkReportSheet> selectAccessibleSheets(WorkReportSheet workReportSheet)
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) return workReportSheetMapper.selectWorkReportSheetList(workReportSheet);

        SysUser currentUser = loginUser.getUser();
        boolean isAdmin = currentUser.isAdmin();
        Long userId = currentUser.getUserId();
        Long deptId = currentUser.getDeptId();

        String roleIdList = currentUser.getRoles().stream()
                .map(r -> String.valueOf(r.getRoleId()))
                .collect(Collectors.joining(","));

        // 手动构建 @DataScope 条件
        String dataScopeCondition = buildDataScopeCondition(currentUser);

        workReportSheet.getParams().put("isAdmin", isAdmin ? 1 : 0);
        workReportSheet.getParams().put("currentUserId", userId);
        workReportSheet.getParams().put("currentDeptId", deptId);
        workReportSheet.getParams().put("roleIdList", roleIdList);
        workReportSheet.getParams().put("dataScopeCondition", dataScopeCondition);

        return workReportSheetMapper.selectAccessibleSheets(workReportSheet);
    }

    /**
     * 手动构建 @DataScope 条件片段（对�?DataScopeAspect.dataScopeFilter 逻辑�?     * 输出格式�?wrs.dept_id = 100 ...)，可直接嵌入 selectAccessibleSheets �?OR 表达�?     */
    private String buildDataScopeCondition(SysUser user) {
        if (user.isAdmin()) return "";

        StringBuilder sql = new StringBuilder();
        for (SysRole role : user.getRoles()) {
            if (UserConstants.ROLE_DISABLE.equals(role.getStatus())) continue;

            String ds = role.getDataScope();
            if (Constants.Dept.DATA_SCOPE_ALL.equals(ds)) {
                return "";
            } else if (Constants.Dept.DATA_SCOPE_CUSTOM.equals(ds)) {
                sql.append(" OR wrs.dept_id IN (SELECT dept_id FROM sys_role_dept WHERE role_id = ")
                   .append(role.getRoleId()).append(")");
            } else if (Constants.Dept.DATA_SCOPE_DEPT.equals(ds)) {
                sql.append(" OR wrs.dept_id = ").append(user.getDeptId());
            } else if (Constants.Dept.DATA_SCOPE_DEPT_AND_CHILD.equals(ds)) {
                sql.append(" OR wrs.dept_id IN (SELECT dept_id FROM sys_dept WHERE dept_id = ")
                   .append(user.getDeptId()).append(" OR find_in_set(")
                   .append(user.getDeptId()).append(", ancestors))");
            } else if (Constants.Dept.DATA_SCOPE_SELF.equals(ds)) {
                sql.append(" OR wrs.user_id = ").append(user.getUserId());
            }
        }
        if (sql.length() == 0) return "1=0";
        return sql.substring(4); // 去掉前导 " OR "
    }

    @Override
    public int insertWorkReportSheet(WorkReportSheet workReportSheet)
    {
        workReportSheet.setId(IdUtils.fastUUID());
        workReportSheet.setUserId(SecurityUtils.getUserId());
        workReportSheet.setDeptId(SecurityUtils.getDeptId());
        workReportSheet.setDelStatus(0L);
        workReportSheet.setCreateTime(DateUtils.getNowDate());
        return workReportSheetMapper.insertWorkReportSheet(workReportSheet);
    }

    @Override
    public int updateWorkReportSheet(WorkReportSheet workReportSheet)
    {
        workReportSheet.setUpdateTime(DateUtils.getNowDate());
        return workReportSheetMapper.updateWorkReportSheet(workReportSheet);
    }

    @Override
    public int deleteWorkReportSheetByIds(String[] ids)
    {
        for (String id : ids) {
            permissionService.revokeBySheet(id);
        }
        return workReportSheetMapper.deleteWorkReportSheetByIds(ids);
    }

    @Override
    public int deleteWorkReportSheetById(String id)
    {
        permissionService.revokeBySheet(id);
        return workReportSheetMapper.deleteWorkReportSheetById(id);
    }

    @Override
    public int deleteWorkReportSheetByReportId(String reportId)
    {
        // 删除 sheet 时级联清理权�?
        WorkReportSheet q = new WorkReportSheet();
        q.setReportId(reportId);
        List<WorkReportSheet> sheets = workReportSheetMapper.selectWorkReportSheetList(q);
        for (WorkReportSheet s : sheets) {
            permissionService.revokeBySheet(s.getId());
        }
        return workReportSheetMapper.deleteWorkReportSheetByReportId(reportId);
    }
}
