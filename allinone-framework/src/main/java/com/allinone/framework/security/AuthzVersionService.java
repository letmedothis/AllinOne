package com.allinone.framework.security;

import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.allinone.common.constant.CacheConstants;
import com.allinone.common.core.domain.model.LoginUser;
import com.allinone.common.core.redis.RedisCache;

/**
 * 授权版本服务
 * 用于解决在线用户权限缓存刷新覆盖不全的问题
 * 通过版本号机制，当角色、菜单、用户角色等发生变化时，递增受影响用户的授权版本
 * 过滤器每次请求比较当前用户版本，不一致时要求重新登录
 */
@Service
public class AuthzVersionService {

    private static final Logger log = LoggerFactory.getLogger(AuthzVersionService.class);

    private static final String AUTHZ_VERSION_KEY = CacheConstants.LOGIN_TOKEN_KEY + "authz_version:";

    @Autowired
    private RedisCache redisCache;

    /**
     * 获取用户的授权版本
     * 如果不存在，返回0
     */
    public long getVersion(Long userId) {
        String key = AUTHZ_VERSION_KEY + userId;
        Long version = redisCache.getCacheObject(key);
        return version != null ? version : 0L;
    }

    /**
     * 递增用户的授权版本
     * 当角色、菜单、用户角色、dataScope、角色状态等发生变化时调用
     */
    public void incrementVersion(Long userId) {
        String key = AUTHZ_VERSION_KEY + userId;
        Long currentVersion = redisCache.getCacheObject(key);
        long newVersion = (currentVersion != null ? currentVersion : 0L) + 1;
        // 版本 key 不设 TTL：会话可无限滑动续期，若版本先于会话过期，
        // getVersion()=0 与会话内快照恒不匹配，会导致在线用户被误判为"权限已变更"强制踢线
        redisCache.setCacheObject(key, newVersion);
        log.info("用户[{}]授权版本递增至[{}]", userId, newVersion);
    }

    /**
     * 批量递增多个用户的授权版本
     * 用于角色权限变更时，刷新所有持有该角色的用户
     */
    public void incrementVersion(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        for (Long userId : userIds) {
            incrementVersion(userId);
        }
    }

    /**
     * 删除用户的授权版本缓存
     * 用户登出时调用
     */
    public void removeVersion(Long userId) {
        String key = AUTHZ_VERSION_KEY + userId;
        redisCache.deleteObject(key);
    }

    /**
     * 检查用户的授权版本是否匹配
     * 用于过滤器中验证用户权限是否仍然有效
     */
    public boolean isVersionMatch(Long userId, long loginVersion) {
        long currentVersion = getVersion(userId);
        return currentVersion == loginVersion;
    }

    /**
     * 初始化用户的授权版本（登录时调用）
     * 返回分配的版本号
     */
    public long initVersion(Long userId) {
        long currentVersion = getVersion(userId);
        if (currentVersion == 0) {
            // 首次登录，初始化版本号为1
            incrementVersion(userId);
            return 1;
        }
        return currentVersion;
    }
}