package com.allinone.common.utils.uuid;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class IdUtilsTest {

    @Test
    void nextLongIdIsUniquePositiveAndJavascriptSafe() {
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            ids.add(IdUtils.nextLongId());
        }

        assertThat(ids).hasSize(1_000);
        assertThat(ids).allMatch(id -> id > 0 && id <= 9_007_199_254_740_991L);
    }
}
