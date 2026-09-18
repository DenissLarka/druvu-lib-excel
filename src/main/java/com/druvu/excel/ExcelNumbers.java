package com.druvu.excel;

import java.math.BigDecimal;

/**
 * Refuses the numbers Excel cannot hold. Excel keeps 15 significant digits and silently drops the rest, so a longer
 * number would open as a different number than the one written - in a money column, a wrong amount nobody was told
 * about. The caller decides what to give up: round in the column's value function, or write the digits as text.
 */
final class ExcelNumbers {

    static final int SIGNIFICANT_DIGITS = 15;
    private static final long LARGEST_EXACT = 999_999_999_999_999L;

    private ExcelNumbers() {}

    static BigDecimal exact(BigDecimal number, String column) {
        if (number.stripTrailingZeros().precision() > SIGNIFICANT_DIGITS) {
            throw refused(number, column);
        }
        return number;
    }

    static long exact(long number, String column) {
        if (number > LARGEST_EXACT || number < -LARGEST_EXACT) {
            throw refused(number, column);
        }
        return number;
    }

    /** A double is what Excel itself stores - only the values it has no cell for are refused. */
    static double finite(double number, String column) {
        if (Double.isNaN(number) || Double.isInfinite(number)) {
            throw new IllegalArgumentException("column '" + column + "' produced " + number + " - Excel has no such"
                    + " number; decide in the column's value function what the cell should show");
        }
        return number;
    }

    private static IllegalArgumentException refused(Number number, String column) {
        return new IllegalArgumentException("column '" + column + "' produced " + number + " - Excel keeps "
                + SIGNIFICANT_DIGITS + " significant digits and would silently change it; round it in the column's"
                + " value function, or write an identifier as text");
    }
}
