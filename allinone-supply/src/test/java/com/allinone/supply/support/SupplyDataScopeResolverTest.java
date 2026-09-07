package com.allinone.supply.support;

import static com.allinone.supply.support.SupplyDataScopeResolver.MODE_ALL;
import static com.allinone.supply.support.SupplyDataScopeResolver.MODE_DEPT;
import static com.allinone.supply.support.SupplyDataScopeResolver.MODE_SELF;
import static com.allinone.supply.support.SupplyDataScopeResolver.RANK_EXEC;
import static com.allinone.supply.support.SupplyDataScopeResolver.RANK_LEADER;
import static com.allinone.supply.support.SupplyDataScopeResolver.RANK_STAFF;
import static com.allinone.supply.support.SupplyDataScopeResolver.resolveMode;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class SupplyDataScopeResolverTest {

    @Test
    void adminSeesAll() {
        assertThat(resolveMode(true, null, Set.of())).isEqualTo(MODE_ALL);
    }

    @Test
    void execSeesAll() {
        assertThat(resolveMode(false, RANK_EXEC, Set.of())).isEqualTo(MODE_ALL);
        assertThat(resolveMode(false, " exec ", Set.of())).isEqualTo(MODE_ALL);
    }

    @Test
    void leaderSeesOwnDepartment() {
        assertThat(resolveMode(false, RANK_LEADER, Set.of())).isEqualTo(MODE_DEPT);
    }

    @Test
    void staffSeesOwnDocuments() {
        assertThat(resolveMode(false, RANK_STAFF, Set.of("supervisor"))).isEqualTo(MODE_SELF);
    }

    @Test
    void unconfiguredRankFallsBackToLegacyRoleRule() {
        // 未配置职级 + 持有旧全局角色 → 仍全局可见(过渡期不回退功能)
        assertThat(resolveMode(false, null, Set.of("supervisor"))).isEqualTo(MODE_ALL);
        assertThat(resolveMode(false, null, Set.of("finance", "purchaser"))).isEqualTo(MODE_ALL);
        assertThat(resolveMode(false, null, Set.of("purchaser"))).isEqualTo(MODE_SELF);
        assertThat(resolveMode(false, null, Set.of())).isEqualTo(MODE_SELF);
        assertThat(resolveMode(false, null, null)).isEqualTo(MODE_SELF);
    }
}
