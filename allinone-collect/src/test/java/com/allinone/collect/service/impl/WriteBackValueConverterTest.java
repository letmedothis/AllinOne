package com.allinone.collect.service.impl;

import com.allinone.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WriteBackValueConverterTest {
    private final WriteBackValueConverter converter = new WriteBackValueConverter();

    @Test
    void usesRawValueForDecimalInsteadOfFormattedDisplayValue() {
        assertThat(converter.convert(1234.50, "1,234.50", "amount"))
                .isEqualTo(new BigDecimal("1234.5"));
    }

    @Test
    void supportsLegacyAliasesAndExactIntegers() {
        assertThat(converter.convert("42", null, "bigint")).isEqualTo(42L);
        assertThat(converter.normalizeType("varchar")).isEqualTo("string");
        assertThatThrownBy(() -> converter.convert("1.5", null, "integer"))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void convertsTextAndExcelSerialDates() {
        assertThat(converter.convert("2026/8/28", null, "date"))
                .isEqualTo(LocalDate.of(2026, 8, 28));
        assertThat(converter.convert(2.5, null, "datetime"))
                .isEqualTo(LocalDateTime.of(1900, 1, 1, 12, 0));
    }

    @Test
    void convertsBooleanAndUsesNullForBlankTypedValues() {
        assertThat(converter.convert("是", null, "boolean")).isEqualTo(true);
        assertThat(converter.convert("", null, "decimal")).isNull();
        assertThat(converter.convert("", null, "string")).isEqualTo("");
    }

    @Test
    void unknownTypeFallsBackToStringDuringCompatibilityPeriod() {
        assertThat(converter.convert(7, null, "custom_type")).isEqualTo("7");
    }
}
