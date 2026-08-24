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

/**
 * JimuReport 鉴权集成
 * 实现 JmReportTokenServiceI 接口，对接 RuoYi 的 TokenService
 * 官方文档: https://help.jimureport.com/config/token.html
 */
@Component
public class JimuReportTokenService implements JmReportTokenServiceI {

    @Autowired
    private TokenService tokenService;

    /**
     * 从 HttpServletRequest 中提取 Token 令牌
     */
    @Override
    public String getToken(HttpServletRequest request) {
        return tokenService.getJimuReportToken(request);
    }

    /**
     * 校验 Token 有效性
     */
    @Override
    public Boolean verifyToken(String token) {
        try {
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
        Set<String> permissions = loginUser == null ? null : loginUser.getPermissions();
        return permissions == null ? new String[0] : permissions.toArray(new String[0]);
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
        info.put("username", loginUser.getUsername());
        info.put("user_id", loginUser.getUserId());
        info.put("dept_id", loginUser.getDeptId());
        info.put("roles", getRoles(token));
        return info;
    }
}
