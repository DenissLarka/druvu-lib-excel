package com.druvu.excel;

import java.math.BigDecimal;

/**
 * Estimates how many characters a cell shows, so columns can be sized without Excel's own measuring. An estimate on
 * purpose: it leans wide, because a slightly roomy column is fine and a column of {@code ####} is not.
 */
final class CellWidth {

    private static final int DATE = 10;
    private static final int TIMESTAMP = 19;
    private static final int BOOLEAN = 5;

    private CellWidth() {}

    static int of(Object value, String format) {
        return switch (value) {
            case null -> 0;
            case Boolean flag -> BOOLEAN;
            case BigDecimal number -> ofNumber(number, format);
            case Double number -> ofNumber(BigDecimal.valueOf(number), format);
            case Float number -> ofNumber(BigDecimal.valueOf(number), format);
            case Number number -> ofNumber(BigDecimal.valueOf(number.longValue()), format);
            case java.time.LocalDate date -> Math.max(DATE, format.length());
            case java.time.LocalDateTime timestamp -> Math.max(TIMESTAMP, format.length());
            default -> value.toString().length();
        };
    }

    private static int ofNumber(BigDecimal number, String format) {
        if (Style.GENERAL.equals(format)) {
            return number.toPlainString().length();
        }
        boolean percent = format.endsWith("%");
        BigDecimal shown = percent ? number.movePointRight(2) : number;
        int integerDigits = Math.max(1, shown.precision() - shown.scale());
        int separators = format.contains(",") ? (integerDigits - 1) / 3 : 0;
        int point = format.indexOf('.');
        int decimals = point < 0
                ? 0
                : (int) format.substring(point + 1)
                        .chars()
                        .filter(c -> c == '0' || c == '#')
                        .count();
        int sign = number.signum() < 0 ? 1 : 0;
        return sign + integerDigits + separators + (decimals > 0 ? decimals + 1 : 0) + (percent ? 1 : 0);
    }
}
