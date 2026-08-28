package com.allinone.collect.service.impl;

import com.allinone.common.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;

/** Converts Luckysheet values to typed JDBC parameters. */
final class WriteBackValueConverter {
    private static final Logger log = LoggerFactory.getLogger(WriteBackValueConverter.class);
    private static final LocalDate EXCEL_EPOCH = LocalDate.of(1899, 12, 30);
    private static final Map<String, String> TYPE_ALIASES = Map.ofEntries(
            Map.entry("string", "string"), Map.entry("varchar", "string"),
            Map.entry("char", "string"), Map.entry("text", "string"),
            Map.entry("integer", "integer"), Map.entry("int", "integer"),
            Map.entry("bigint", "integer"), Map.entry("long", "integer"),
            Map.entry("decimal", "decimal"), Map.entry("number", "decimal"),
            Map.entry("numeric", "decimal"), Map.entry("amount", "decimal"),
            Map.entry("date", "date"), Map.entry("datetime", "datetime"),
            Map.entry("timestamp", "datetime"), Map.entry("boolean", "boolean"),
            Map.entry("bool", "boolean")
    );
    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy年M月d日")
    };
    private static final DateTimeFormatter[] DATETIME_FORMATS = {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/M/d H:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/M/d H:mm")
    };

    Object convert(Object rawValue, String displayValue, String configuredType) {
        String type = normalizeType(configuredType);
        Object value = "string".equals(type) && displayValue != null ? displayValue : rawValue;
        if (value == null || value instanceof String && ((String) value).isBlank()) {
            return "string".equals(type) ? (value == null ? null : "") : null;
        }

        try {
            return switch (type) {
                case "integer" -> toLong(value);
                case "decimal" -> toDecimal(value);
                case "date" -> toDate(value);
                case "datetime" -> toDateTime(value);
                case "boolean" -> toBoolean(value);
                default -> value.toString();
            };
        } catch (RuntimeException ex) {
            throw new ServiceException("值“" + value + "”不能转换为 " + type)
                    .setDetailMessage(ex.getMessage());
        }
    }

    String normalizeType(String configuredType) {
        String requested = configuredType == null || configuredType.isBlank()
                ? "string" : configuredType.trim().toLowerCase(Locale.ROOT);
        String normalized = TYPE_ALIASES.get(requested);
        if (normalized == null) {
            log.warn("未知回写数据类型 {}，暂按 string 兼容处理", configuredType);
            return "string";
        }
        return normalized;
    }

    private Long toLong(Object value) {
        BigDecimal decimal = toDecimal(value);
        return decimal.longValueExact();
    }

    private BigDecimal toDecimal(Object value) {
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return new BigDecimal(number.toString());
        return new BigDecimal(value.toString().trim().replace(",", ""));
    }

    private LocalDate toDate(Object value) {
        if (value instanceof Number number) return excelDateTime(number).toLocalDate();
        String text = value.toString().trim();
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try { return LocalDate.parse(text, formatter); }
            catch (DateTimeParseException ignored) { }
        }
        return toDateTime(text).toLocalDate();
    }

    private LocalDateTime toDateTime(Object value) {
        if (value instanceof Number number) return excelDateTime(number);
        String text = value.toString().trim();
        for (DateTimeFormatter formatter : DATETIME_FORMATS) {
            try { return LocalDateTime.parse(text, formatter); }
            catch (DateTimeParseException ignored) { }
        }
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try { return LocalDate.parse(text, formatter).atStartOfDay(); }
            catch (DateTimeParseException ignored) { }
        }
        throw new DateTimeParseException("不支持的日期时间格式", text, 0);
    }

    private LocalDateTime excelDateTime(Number value) {
        BigDecimal serial = new BigDecimal(value.toString());
        long days = serial.longValue();
        long nanos = serial.subtract(BigDecimal.valueOf(days))
                .multiply(BigDecimal.valueOf(86_400_000_000_000L)).longValue();
        return LocalDateTime.of(EXCEL_EPOCH.plusDays(days), LocalTime.MIDNIGHT).plusNanos(nanos);
    }

    private Boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) return bool;
        String text = value.toString().trim().toLowerCase(Locale.ROOT);
        if ("true".equals(text) || "1".equals(text) || "是".equals(text) || "yes".equals(text)) return true;
        if ("false".equals(text) || "0".equals(text) || "否".equals(text) || "no".equals(text)) return false;
        throw new IllegalArgumentException("仅支持 true/false、1/0、是/否、yes/no");
    }
}
