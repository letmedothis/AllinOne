package com.allinone.framework.jimureport;

import com.allinone.framework.web.service.TokenService;
import org.jeecg.modules.jmreport.api.JmReportTokenServiceI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

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
        String token = request.getParameter("token");
        if (token == null || token.isEmpty()) {
            token = request.getHeader("X-Access-Token");
        }
        if (token == null || token.isEmpty()) {
            token = request.getHeader("token");
        }
        return token;
    }

    /**
     * 校验 Token 有效性
     */
    @Override
    public Boolean verifyToken(String token) {
        try {
            String username = tokenService.getUsernameFromToken(token);
            return username != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 根据 Token 获取用户名
     */
    @Override
    public String getUsername(String token) {
        return tokenService.getUsernameFromToken(token);
    }

    @Override
    public String[] getRoles(String token) {
        return new String[0];
    }

    @Override
    public String[] getPermissions(String token) {
        return new String[0];
    }

    /**
     * 获取用户身份信息（包含部门、角色等上下文）
     * 用于报表 SQL 中的参数注入（如 #{dept_id}）
     */
    @Override
    public Map<String, Object> getUserInfo(String token) {
        Map<String, Object> info = new HashMap<>();
        info.put("username", tokenService.getUsernameFromToken(token));
        return info;
    }
}
