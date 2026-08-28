package com.allinone.framework.jimureport;

import com.allinone.framework.web.service.TokenService;
import org.jeecg.modules.jmreport.api.JmReportTokenServiceI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import com.allinone.common.core.domain.model.LoginUser;
import com.allinone.common.core.domain.entity.SysRole;
import com.allinone.common.utils.StringUtils;

/**
 * JimuReport 鉴权集成
 * 实现 JmReportTokenServiceI 接口，对接 RuoYi 的 TokenService
 * 官方文档: https://help.jimureport.com/config/token.html
 */
@Component
public class JimuReportTokenService implements JmReportTokenServiceI {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private JimuTicketService jimuTicketService;

    /**
     * 从 HttpServletRequest 中提取 Token 令牌
     * 支持从URL参数中获取ticket并验证
     */
    @Override
    public String getToken(HttpServletRequest request) {
        // 首先检查是否是票据验证请求
        String ticket = request.getParameter("ticket");
        if (StringUtils.isNotEmpty(ticket)) {
            // 验证并消费票据
            JimuTicketService.TicketInfo ticketInfo = jimuTicketService.consumeTicket(ticket);
            if (ticketInfo != null) {
                // 票据有效，返回票据中绑定的原始JWT令牌，
                // 使后续 verifyToken/getUsername/getUserInfo 走既有 JWT 校验逻辑
                return ticketInfo.getToken();
            }
            // 票据无效，返回null
            return null;
        }
        
        // 否则使用原有的token获取逻辑
        return tokenService.getJimuReportToken(request);
    }

    /**
     * 校验 Token 有效性
     */
    @Override
    public Boolean verifyToken(String token) {
        try {
            if (StringUtils.isEmpty(token)) {
                return false;
            }
            // 验证JWT令牌是否有效（包括过期时间）
            if (!tokenService.validateToken(token)) {
                return false;
            }
            return tokenService.getLoginUserFromToken(token) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 根据 Token 获取用户名
     */
    @Override
    public String getUsername(String token) {
        LoginUser loginUser = tokenService.getLoginUserFromToken(token);
        return loginUser == null ? null : loginUser.getUsername();
    }

    @Override
    public String[] getRoles(String token) {
        LoginUser loginUser = tokenService.getLoginUserFromToken(token);
        if (loginUser == null || loginUser.getUser() == null || loginUser.getUser().getRoles() == null)
            return new String[0];
        return loginUser.getUser().getRoles().stream().map(SysRole::getRoleKey).toArray(String[]::new);
    }

    @Override
    public String[] getPermissions(String token) {
        LoginUser loginUser = tokenService.getLoginUserFromToken(token);
        if (loginUser == null || loginUser.getPermissions() == null) return new String[0];
        Set<String> permissions = new java.util.HashSet<>(loginUser.getPermissions());
        // 超级管理员(拥有 *:*:* 或 *) 视为拥有 JimuReport/JimuBI 全部权限，
        // 解决引擎精确匹配 @RequiresPermissions 导致管理员设计器操作返回 403 的问题
        if (permissions.contains("*") || permissions.contains("*:*:*")) {
            permissions.add("*");
        }
        return permissions.toArray(new String[0]);
    }

    /**
     * 获取用户身份信息（包含部门、角色等上下文）
     * 用于报表 SQL 中的参数注入（如 #{dept_id}）
     */
    @Override
    public Map<String, Object> getUserInfo(String token) {
        Map<String, Object> info = new HashMap<>();
        LoginUser loginUser = tokenService.getLoginUserFromToken(token);
        if (loginUser == null || loginUser.getUser() == null) return info;
        String username = loginUser.getUsername();
        Object deptId = loginUser.getDeptId();
        java.util.Date now = new java.util.Date();
        info.put("sysUserCode", username);
        info.put("sysUserName", username);
        info.put("sysOrgCode", deptId);
        info.put("sysDate", new java.text.SimpleDateFormat("yyyy-MM-dd").format(now));
        info.put("sysDateTime", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(now));
        // 兼容保留既有键
        info.put("username", username);
        info.put("user_id", loginUser.getUserId());
        info.put("dept_id", deptId);
        info.put("roles", getRoles(token));
        return info;
    }
}
