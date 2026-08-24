package com.allinone.collect.service.impl;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DataWriteBackServiceImplTest {

    @Test
    void parsesScalarAndObjectValuesFromEverySheet() {
        String workbook = "[{\"celldata\":[{\"r\":1,\"c\":2,\"v\":\"first\"}]},"
            + "{\"celldata\":[{\"r\":1,\"c\":2,\"v\":{\"v\":9,\"m\":\"9\"}}]}]";

        Map<String, String> values = new TestableDataWriteBackService().parse(workbook);

        assertThat(values)
            .containsEntry("0,1,2", "first")
            .containsEntry("1,1,2", "9");
    }

    private static final class TestableDataWriteBackService extends DataWriteBackServiceImpl {
        Map<String, String> parse(String formData) {
            return parseFormDataToCellMap(formData);
        }
    }
}
