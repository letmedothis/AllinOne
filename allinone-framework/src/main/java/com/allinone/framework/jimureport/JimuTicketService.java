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
 * 1. 已登录前端调用受保护的 POST /system/jimu/ticket，请求目标相对路径
 * 2. 服务端生成256-bit随机ticket，Redis TTL 60秒，绑定userId、token UUID、目标路径和用途
 * 3. iframe 打开 /jmreport/auth/ticket?ticket=...&target=...
 * 4. 服务端原子 GET+DELETE ticket，设置短时会话Cookie，再302到不含ticket的目标URL
 */
@Service
public class JimuTicketService {

    private static final Logger log = LoggerFactory.getLogger(JimuTicketService.class);

    private static final String TICKET_KEY_PREFIX = CacheConstants.LOGIN_TOKEN_KEY + "jimu_ticket:";
    
    /**
     * 票据有效期（秒）
     */
    private static final long TICKET_EXPIRE_SECONDS = 60;

    @Autowired
    private RedisCache redisCache;

    /**
     * 生成一次性票据
     * 
     * @param userId 用户ID
     * @param tokenUuid 用户的token UUID
     * @param targetPath 目标路径
     * @return 票据字符串
     */
    public String generateTicket(Long userId, String tokenUuid, String targetPath) {
        String ticket = IdUtils.fastUUID();
        String ticketKey = TICKET_KEY_PREFIX + ticket;
        
        // 票据信息
        TicketInfo ticketInfo = new TicketInfo();
        ticketInfo.setUserId(userId);
        ticketInfo.setTokenUuid(tokenUuid);
        ticketInfo.setTargetPath(targetPath);
        ticketInfo.setCreateTime(System.currentTimeMillis());
        
        // 存储到Redis，设置过期时间
        redisCache.setCacheObject(ticketKey, ticketInfo, TICKET_EXPIRE_SECONDS, TimeUnit.SECONDS);
        
        log.info("生成JimuReport票据: userId={}, targetPath={}, ticket={}", userId, targetPath, ticket);
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
        TicketInfo ticketInfo = redisCache.getCacheObject(ticketKey);
        if (ticketInfo != null) {
            // 删除票据，确保一次性使用
            redisCache.deleteObject(ticketKey);
            log.info("消费JimuReport票据: userId={}, targetPath={}", ticketInfo.getUserId(), ticketInfo.getTargetPath());
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
        private String tokenUuid;
        private String targetPath;
        private Long createTime;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getTokenUuid() {
            return tokenUuid;
        }

        public void setTokenUuid(String tokenUuid) {
            this.tokenUuid = tokenUuid;
        }

        public String getTargetPath() {
            return targetPath;
        }

        public void setTargetPath(String targetPath) {
            this.targetPath = targetPath;
        }

        public Long getCreateTime() {
            return createTime;
        }

        public void setCreateTime(Long createTime) {
            this.createTime = createTime;
        }
    }
}