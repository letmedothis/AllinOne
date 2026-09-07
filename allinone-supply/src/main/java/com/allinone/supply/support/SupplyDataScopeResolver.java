package com.allinone.supply.support;

import com.allinone.common.utils.SecurityUtils;
import com.allinone.supply.mapper.SupplyUserMapper;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 供应链数据范围解析(按职级,V2.0 §3)。
 *
 * 判定顺序:系统管理员 → ALL;rank_level=EXEC(总监) → ALL;LEADER(部门领导) → 本部门 DEPT;
 * STAFF(职员) → 本人 SELF;rank_level 未配置(过渡期)→ 回退旧逻辑:持有全局业务角色则 ALL,否则 SELF。
 */
@Component
public class SupplyDataScopeResolver {
    public static final String MODE_ALL = "ALL";
    public static final String MODE_DEPT = "DEPT";
    public static final String MODE_SELF = "SELF";
    public static final String RANK_EXEC = "EXEC";
    public static final String RANK_LEADER = "LEADER";
    public static final String RANK_STAFF = "STAFF";
    /** 旧数据范围中的“全局角色”,仅在 rank_level 未配置时作回退。 */
    public static final Set<String> LEGACY_GLOBAL_ROLES =
            Set.of("supervisor", "purchasing_supervisor", "purchase_supervisor", "finance");

    public static final String PARAM_MODE = "supplyScopeMode";
    public static final String PARAM_USER_ID = "supplyScopeUserId";
    public static final String PARAM_DEPT_ID = "supplyScopeDeptId";

    @Autowired
    private SupplyUserMapper userMapper;

    /** 解析当前登录用户的数据范围。 */
    public Scope current() {
        var login = SecurityUtils.getLoginUser();
        Long userId = login.getUser().getUserId();
        Long deptId = login.getUser().getDeptId();
        Set<String> roles = login.getUser().getRoles() == null ? Set.of()
                : login.getUser().getRoles().stream().map(role -> role.getRoleKey()).collect(Collectors.toSet());
        String rank = normalize(userMapper.selectRankLevel(userId));
        String mode = resolveMode(SecurityUtils.isAdmin(), rank, roles);
        return new Scope(mode, userId, deptId);
    }

    /** 把当前用户范围写入查询参数(供 MyBatis params.* 使用)。 */
    public void putInto(Map<String, Object> params) {
        Scope scope = current();
        params.put(PARAM_MODE, scope.mode());
        params.put(PARAM_USER_ID, scope.userId());
        params.put(PARAM_DEPT_ID, scope.deptId());
    }

    /** 纯逻辑判定,便于单测。 */
    static String resolveMode(boolean admin, String rank, Set<String> roleKeys) {
        if (admin) return MODE_ALL;
        if (RANK_EXEC.equals(normalize(rank))) return MODE_ALL;
        if (RANK_LEADER.equals(normalize(rank))) return MODE_DEPT;
        if (RANK_STAFF.equals(normalize(rank))) return MODE_SELF;
        boolean legacyGlobal = roleKeys != null
                && roleKeys.stream().anyMatch(LEGACY_GLOBAL_ROLES::contains);
        return legacyGlobal ? MODE_ALL : MODE_SELF;
    }

    private static String normalize(String rank) {
        return rank == null ? null : rank.trim().toUpperCase(Locale.ROOT);
    }

    /** 当前用户的数据范围快照。 */
    public static final class Scope {
        private final String mode;
        private final Long userId;
        private final Long deptId;

        Scope(String mode, Long userId, Long deptId) {
            this.mode = mode;
            this.userId = userId;
            this.deptId = deptId;
        }

        public String mode() { return mode; }
        public Long userId() { return userId; }
        public Long deptId() { return deptId; }
    }
}
