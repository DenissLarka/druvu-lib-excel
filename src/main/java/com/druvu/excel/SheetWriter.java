package com.druvu.excel;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.dhatim.fastexcel.Worksheet;

/**
 * Streams one {@link Sheet} into the engine: the header row, then a row per element.
 *
 * <p>The look every sheet gets without asking: a bold header that stays in view, a filter on every column, and columns
 * wide enough for what they hold. Widths must reach the file before the first row does, so they are measured on the
 * first {@value #BATCH} rows; from there on rows leave memory a batch at a time, and a sheet can be any length.
 */
final class SheetWriter<T> {

    static final int BATCH = 1_000;
    private static final int MAX_WIDTH = 60;
    /** Room for the filter button, and for bold letters being wider than plain ones. */
    private static final int HEADER_EXTRA = 4;

    private static final int PADDING = 2;

    private final Sheet<T> sheet;
    private final List<Column<T>> columns;
    private final Worksheet target;
    private final int[] widths;

    private SheetWriter(Sheet<T> sheet, Worksheet target) {
        this.sheet = sheet;
        this.columns = sheet.columns();
        this.target = target;
        this.widths = new int[columns.size()];
    }

    static <T> void write(Sheet<T> sheet, Worksheet target) throws IOException {
        new SheetWriter<>(sheet, target).write();
    }

    private void write() throws IOException {
        if (columns.isEmpty()) {
            throw new IllegalStateException("sheet '" + sheet.name() + "' has no columns");
        }
        header();
        int r = 1;
        for (T row : sheet.rows()) {
            for (int c = 0; c < columns.size(); c++) {
                cell(r, c, row);
            }
            if (r % BATCH == 0) {
                release(r == BATCH);
            }
            r++;
        }
        release(r <= BATCH);
        // the engine writes the filter range last, so it can cover the rows actually written
        target.setAutoFilter(0, 0, r - 1, columns.size() - 1);
        target.finish();
    }

    private void header() {
        for (int c = 0; c < columns.size(); c++) {
            String header = columns.get(c).header();
            target.value(0, c, header);
            CellStyles.apply(Style.HEADER, target.style(0, c));
            widths[c] = header.length() + HEADER_EXTRA;
        }
        target.freezePane(0, 1);
    }

    /** Hands the rows written so far to the file. The first batch takes the column widths with it. */
    private void release(boolean first) throws IOException {
        if (first) {
            for (int c = 0; c < widths.length; c++) {
                target.width(c, Math.min(widths[c] + PADDING, MAX_WIDTH));
            }
        }
        target.flush();
    }

    private void cell(int r, int c, T row) {
        Column<T> column = columns.get(c);
        Object value = unwrapped(column.value().apply(row));
        Style style = column.style().apply(row);
        if (style == null) {
            throw new IllegalArgumentException("column '" + column.header() + "' chose no style - use Style.NONE");
        }
        String typeFormat = put(r, c, value, column.header());
        Style shown = Style.GENERAL.equals(style.format()) ? style.withFormat(typeFormat) : style;
        CellStyles.apply(shown, target.style(r, c));
        if (r <= BATCH) {
            widths[c] = Math.max(widths[c], CellWidth.of(value, shown.format()));
        }
    }

    /** An {@link Optional} is a value that may be missing - which a sheet already has a word for: an empty cell. */
    private static Object unwrapped(Object value) {
        return value instanceof Optional<?> maybe ? maybe.orElse(null) : value;
    }

    /**
     * Text goes into its own cell, not into the file's shared table of strings: the engine holds that table in memory
     * until the last row, so a long sheet of distinct texts would not stream. Compression makes up for the repetition.
     */
    private void text(int r, int c, String text) {
        target.inlineString(r, c, text);
    }

    /**
     * Writes the value as the cell type Excel expects for it, and answers the number format that type needs when the
     * style names none: a date stored under "General" would show as a bare serial number.
     *
     * <p>Whole numbers deliberately get no thousands separator: a customer number or a year is not a quantity.
     */
    private String put(int r, int c, Object value, String header) {
        switch (value) {
            case null -> {
                return Style.GENERAL;
            }
            case CharSequence text -> text(r, c, text.toString());
            case Enum<?> constant -> text(r, c, constant.name());
            case Boolean flag -> target.value(r, c, flag);
            case Integer number -> target.value(r, c, number);
            case Short number -> target.value(r, c, number);
            case Byte number -> target.value(r, c, number);
            case Long number -> target.value(r, c, ExcelNumbers.exact(number, header));
            case BigDecimal number -> target.value(r, c, ExcelNumbers.exact(number, header));
            case Double number -> target.value(r, c, ExcelNumbers.finite(number, header));
            case Float number -> target.value(r, c, ExcelNumbers.finite(number, header));
            case LocalDate date -> {
                target.value(r, c, date);
                return Style.DATE.format();
            }
            case LocalDateTime timestamp -> {
                target.value(r, c, timestamp);
                return Style.TIMESTAMP.format();
            }
            default ->
                throw new IllegalArgumentException("column '" + header + "' produced a "
                        + value.getClass().getName()
                        + " - turn it into text, a number, a boolean or a date in the column's value function");
        }
        return Style.GENERAL;
    }
}
