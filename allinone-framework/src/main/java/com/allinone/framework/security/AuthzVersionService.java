package com.allinone.framework.security;

import java.util.Collection;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import com.allinone.common.constant.CacheConstants;
import com.allinone.common.core.domain.model.LoginUser;

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

    /** 兼容旧 FastJson2 Long 值（如 1L/"1L"）并在 Redis 内原子递增。 */
    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>(
        "local value = redis.call('GET', KEYS[1]); "
            + "if not value then value = '0' end; "
            + "value = string.gsub(value, '[^0-9%-]', ''); "
            + "local next = (tonumber(value) or 0) + 1; "
            + "redis.call('SET', KEYS[1], tostring(next)); return next;",
        Long.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 获取用户的授权版本
     * 如果不存在，返回0
     */
    public long getVersion(Long userId) {
        String key = AUTHZ_VERSION_KEY + userId;
        String version = stringRedisTemplate.opsForValue().get(key);
        if (version == null) {
            return 0L;
        }
        String normalized = version.replaceAll("[^0-9-]", "");
        return normalized.isEmpty() ? 0L : Long.parseLong(normalized);
    }

    /**
     * 递增用户的授权版本
     * 当角色、菜单、用户角色、dataScope、角色状态等发生变化时调用
     */
    public void incrementVersion(Long userId) {
        String key = AUTHZ_VERSION_KEY + userId;
        Long newVersion = stringRedisTemplate.execute(INCREMENT_SCRIPT, Collections.singletonList(key));
        if (newVersion == null) {
            throw new IllegalStateException("授权版本递增失败");
        }
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
        stringRedisTemplate.delete(key);
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
