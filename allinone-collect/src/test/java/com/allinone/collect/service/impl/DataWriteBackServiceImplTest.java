package com.allinone.collect.service.impl;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DataWriteBackServiceImplTest {

    @Test
    void parsesScalarAndObjectValuesFromEverySheet() {
        String workbook = "[{\"celldata\":[{\"r\":1,\"c\":2,\"v\":\"first\"}]},"
            + "{\"celldata\":[{\"r\":1,\"c\":2,\"v\":{\"v\":9,\"m\":\"9\"}}]}]";

        Map<String, DataWriteBackServiceImpl.CellValue> values = new TestableDataWriteBackService().parse(workbook);

        assertThat(values.get("0,1,2").rawValue()).isEqualTo("first");
        assertThat(values.get("1,1,2").rawValue()).isEqualTo(9);
        assertThat(values.get("1,1,2").displayValue()).isEqualTo("9");
    }

    private static final class TestableDataWriteBackService extends DataWriteBackServiceImpl {
        Map<String, CellValue> parse(String formData) {
            return parseFormDataToCellMap(formData);
        }
    }
}
