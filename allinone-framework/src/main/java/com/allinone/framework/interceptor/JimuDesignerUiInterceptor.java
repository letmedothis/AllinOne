package com.allinone.framework.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JimuReport 设计器界面开关拦截器。
 *
 * JimuReport 引擎本身不提供设计器开关属性，本拦截器使 jimureport.ui-enable 配置真正生效：
 * 关闭时拒绝访问设计器页面（/jmreport/design、/jmreport/list），报表查看与数据接口不受影响。
 *
 * @author allinone
 */
@Component
public class JimuDesignerUiInterceptor implements HandlerInterceptor
{
    @Value("${jimureport.ui-enable:true}")
    private boolean uiEnable;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        if (!uiEnable)
        {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("JimuReport designer UI is disabled (jimureport.ui-enable=false)");
            return false;
        }
        return true;
    }
}
