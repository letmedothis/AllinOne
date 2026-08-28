package com.allinone.framework.jimureport;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.allinone.common.constant.CacheConstants;
import com.allinone.common.core.redis.RedisCache;
import com.allinone.common.utils.uuid.IdUtils;

/**
 * JimuReport/JimuBI 一次性票据服务
 * 用于替代URL中的JWT令牌，提高安全性
 *
 * 流程：
 * 1. 已登录前端调用受保护的 POST /system/jimu/ticket，用当前登录态换取一次性ticket
 * 2. 服务端生成随机ticket，Redis TTL 60秒，绑定userId与原始JWT令牌
 * 3. iframe URL携带ticket访问内嵌引擎（如 /jmreport/view/xx?ticket=...）
 * 4. JimuReportTokenService.getToken 原子消费ticket，换出JWT完成后续鉴权
 */
@Service
public class JimuTicketService {

    private static final Logger log = LoggerFactory.getLogger(JimuTicketService.class);

    private static final String TICKET_KEY_PREFIX = CacheConstants.LOGIN_TOKEN_KEY + "jimu_ticket:";

    /**
     * 票据有效期（秒）
     */
    private static final int TICKET_EXPIRE_SECONDS = 60;

    @Autowired
    private RedisCache redisCache;

    /**
     * 生成一次性票据
     *
     * @param userId 用户ID
     * @param token 用户的原始JWT令牌
     * @return 票据字符串
     */
    public String generateTicket(Long userId, String token) {
        String ticket = IdUtils.fastUUID();
        String ticketKey = TICKET_KEY_PREFIX + ticket;

        // 票据信息
        TicketInfo ticketInfo = new TicketInfo();
        ticketInfo.setUserId(userId);
        ticketInfo.setToken(token);
        ticketInfo.setCreateTime(System.currentTimeMillis());

        // 存储到Redis，设置过期时间
        redisCache.setCacheObject(ticketKey, ticketInfo, TICKET_EXPIRE_SECONDS, TimeUnit.SECONDS);

        log.info("生成JimuReport票据: userId={}, ticket={}", userId, ticket);
        return ticket;
    }

    /**
     * 验证并消费票据（一次性使用）
     *
     * @param ticket 票据字符串
     * @return 票据信息，如果票据无效或已过期则返回null
     */
    public TicketInfo consumeTicket(String ticket) {
        if (ticket == null || ticket.isEmpty()) {
            return null;
        }

        String ticketKey = TICKET_KEY_PREFIX + ticket;

        // 原子获取并删除票据
        TicketInfo ticketInfo = redisCache.getAndDeleteCacheObject(ticketKey);
        if (ticketInfo != null) {
            log.info("消费JimuReport票据: userId={}", ticketInfo.getUserId());
        } else {
            log.warn("JimuReport票据无效或已过期: {}", ticket);
        }

        return ticketInfo;
    }

    /**
     * 票据信息类
     */
    public static class TicketInfo {
        private Long userId;
        private String token;
        private Long createTime;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public Long getCreateTime() {
            return createTime;
        }

        public void setCreateTime(Long createTime) {
            this.createTime = createTime;
        }
    }
}
