package com.allinone.supply.service;

import static com.allinone.supply.service.DigitInvoiceNumbers.looksLikeDigital;
import static com.allinone.supply.service.DigitInvoiceNumbers.normalize;
import static com.allinone.supply.service.DigitInvoiceNumbers.require20Digits;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DigitInvoiceNumbersTest {

    @Test
    void looksLikeDigitalAcceptsOnlyTwentyDigits() {
        assertThat(looksLikeDigital("12345678901234567890")).isTrue();
        assertThat(looksLikeDigital(" 12345678901234567890 ")).isTrue();
        assertThat(looksLikeDigital("1234567890123456789")).isFalse();
        assertThat(looksLikeDigital("123456789012345678901")).isFalse();
        assertThat(looksLikeDigital("1234567890123456789A")).isFalse();
        assertThat(looksLikeDigital(null)).isFalse();
    }

    @Test
    void normalizeTrimsOnlyWhitespaceAndKeepsLeadingZeros() {
        assertThat(normalize(" 01234567890123456789 ")).isEqualTo("01234567890123456789");
        assertThat(normalize(null)).isNull();
    }

    @Test
    void require20DigitsRejectsWrongLength() {
        assertThatThrownBy(() -> require20Digits("1234567890123456789", "数电票号码"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("20 位");
    }
}
