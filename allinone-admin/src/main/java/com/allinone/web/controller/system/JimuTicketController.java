package com.allinone.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.allinone.common.constant.Constants;
import com.allinone.common.core.controller.BaseController;
import com.allinone.common.core.domain.AjaxResult;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.ServletUtils;
import com.allinone.common.utils.StringUtils;
import com.allinone.framework.jimureport.JimuTicketService;

/**
 * JimuReport/JimuBI 一次性票据
 *
 * 已登录前端调用 POST /system/jimu/ticket，用当前请求头中的JWT换取一次性ticket，
 * 随后 iframe URL 携带 ticket 访问内嵌引擎，由 JimuReportTokenService 消费ticket完成鉴权，
 * 避免把长期有效的JWT直接暴露在URL中。该接口不在Security匿名白名单内，默认要求登录态。
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/jimu/ticket")
public class JimuTicketController extends BaseController
{
    // 令牌自定义标识
    @Value("${token.header}")
    private String header;

    @Autowired
    private JimuTicketService jimuTicketService;

    /**
     * 申请一次性报表票据（任何已登录用户）
     */
    @PostMapping
    public AjaxResult generate()
    {
        // 从当前请求头取原始JWT令牌
        String token = ServletUtils.getRequest().getHeader(header);
        if (StringUtils.isNotEmpty(token) && token.startsWith(Constants.TOKEN_PREFIX))
        {
            token = token.substring(Constants.TOKEN_PREFIX.length());
        }
        Long userId = SecurityUtils.getUserId();
        if (StringUtils.isEmpty(token) || StringUtils.isNull(userId))
        {
            return error("未登录或令牌无效");
        }
        // 注意不能用 success(String) 形参的重载：ticket 是字符串会绑定到 msg 字段，
        // 前端从 data 字段取票据。须显式走 (msg, data) 重载把票据放入 data。
        return AjaxResult.success("操作成功", jimuTicketService.generateTicket(userId, token));
    }
}
