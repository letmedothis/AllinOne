package com.allinone.supply.service;

import com.allinone.common.exception.ServiceException;

/** 数电票号码(20 位数字)规范化与校验。 */
public final class DigitInvoiceNumbers {
    private static final String TWENTY_DIGITS = "[0-9]{20}";

    private DigitInvoiceNumbers() {
    }

    /** 去首尾空白;不做其它改写(保留前导零)。 */
    public static String normalize(String number) {
        return number == null ? null : number.trim();
    }

    public static boolean looksLikeDigital(String number) {
        String value = normalize(number);
        return value != null && value.matches(TWENTY_DIGITS);
    }

    /** 数电票号码必须为 20 位数字。 */
    public static void require20Digits(String number, String field) {
        String value = normalize(number);
        if (value == null || !value.matches(TWENTY_DIGITS)) {
            throw new ServiceException(field + "应为 20 位数字的数电票号码，实际为：" + (value == null ? "空" : value));
        }
    }
}
