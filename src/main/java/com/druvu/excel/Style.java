package com.druvu.excel;

import java.util.Objects;

/**
 * How a cell looks: an immutable value, independent of any workbook.
 *
 * <p>Start from a predefined constant and refine it: {@code MONEY.withFill(Fill.ORANGE)}. Number format and highlight
 * are separate axes, so any format combines with any fill. Two styles with equal components are the same style - the
 * file holds each distinct one once, however many cells or rows ask for it.
 *
 * @param format an Excel number format, e.g. {@code "#,##0.00"}; {@code "General"} is Excel's own default
 * @param fill the cell background
 * @param align the horizontal alignment
 * @param bold whether the text is bold
 */
public record Style(String format, Fill fill, Align align, boolean bold) {

    /** Excel's own name for "no number format". */
    static final String GENERAL = "General";

    /** Excel's default look: values shown as they are. */
    public static final Style NONE = new Style(GENERAL, Fill.NONE, Align.AUTO, false);

    /** Whole numbers with a thousands separator. */
    public static final Style INTEGER = NONE.withFormat("#,##0");

    /** Two decimals with a thousands separator. */
    public static final Style MONEY = NONE.withFormat("#,##0.00");

    /** A fraction shown as a percentage: 0.081 reads 8.10%. */
    public static final Style PERCENT = NONE.withFormat("0.00%");

    /** A calendar date, ISO order. */
    public static final Style DATE = NONE.withFormat("yyyy-mm-dd");

    /** A date with the time of day, ISO order. */
    public static final Style TIMESTAMP = NONE.withFormat("yyyy-mm-dd hh:mm:ss");

    /** The header row: bold, centred, on grey. */
    public static final Style HEADER = NONE.withBold().withFill(Fill.GREY).withAlign(Align.CENTER);

    public Style {
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(fill, "fill");
        Objects.requireNonNull(align, "align");
        if (format.isBlank()) {
            throw new IllegalArgumentException("format is blank - use \"General\" for Excel's default");
        }
    }

    public Style withFormat(String newFormat) {
        return new Style(newFormat, fill, align, bold);
    }

    public Style withFill(Fill newFill) {
        return new Style(format, newFill, align, bold);
    }

    public Style withAlign(Align newAlign) {
        return new Style(format, fill, newAlign, bold);
    }

    public Style withBold() {
        return new Style(format, fill, align, true);
    }
}
