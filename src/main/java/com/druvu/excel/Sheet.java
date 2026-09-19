package com.druvu.excel;

import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * One sheet, described in one place: its rows, and for each column a header, a look and the value to show.
 *
 * <p>Columns appear in the order they are declared. A column without a style gets a sensible one from its values -
 * dates read as dates - and a style that names no number format keeps that behaviour, so
 * {@code Style.NONE.withFill(Fill.RED)} on a date column is still a date.
 *
 * <p>A cell holds text, a number, a boolean, a date or nothing: a value function may answer a {@code String}, an enum,
 * a {@code Boolean}, a whole number, a {@code BigDecimal}, a {@code double}, a {@code LocalDate} or a
 * {@code LocalDateTime}; {@code null} or an empty {@code Optional} leaves the cell empty, and a filled {@code Optional}
 * is shown as what it holds. Anything else is refused, by column name, rather than guessed at.
 *
 * @param <T> what one row is made from
 * @author Deniss Larka <br>
 *     on 18 Sep 2026
 */
public final class Sheet<T> {

    private static final int MAX_NAME_LENGTH = 31;
    private static final Pattern FORBIDDEN_IN_NAME = Pattern.compile("[\\[\\]:*?/\\\\]");

    private final String name;
    private final Iterable<? extends T> rows;
    private final List<Column<T>> columns = new ArrayList<>();

    Sheet(String name, Iterable<? extends T> rows) {
        this.name = validName(name);
        this.rows = Objects.requireNonNull(rows, "rows");
    }

    /** A column styled by what its values are. */
    public Sheet<T> column(String header, Function<? super T, ?> value) {
        return column(header, Style.NONE, value);
    }

    /** A column with one style for every row. */
    public Sheet<T> column(String header, Style style, Function<? super T, ?> value) {
        Objects.requireNonNull(style, "style");
        return column(header, row -> style, value);
    }

    /** A column whose style is chosen row by row - an overdue amount in red, the rest plain. */
    public Sheet<T> column(String header, Function<? super T, Style> style, Function<? super T, ?> value) {
        columns.add(new Column<>(header, style, value));
        return this;
    }

    /** Writes a workbook holding just this sheet - all of it or none of it, see {@link Workbook#save}. */
    public void save(Path file) {
        new Workbook().sheet(this).save(file);
    }

    /** Writes a workbook holding just this sheet, and closes the stream. */
    public void writeTo(OutputStream out) {
        new Workbook().sheet(this).writeTo(out);
    }

    String name() {
        return name;
    }

    Iterable<? extends T> rows() {
        return rows;
    }

    List<Column<T>> columns() {
        return List.copyOf(columns);
    }

    /** Excel's own rules. Refused here, by name, rather than silently rewritten further down. */
    private static String validName(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank() || name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("sheet name must be 1 to 31 characters: '" + name + "'");
        }
        if (FORBIDDEN_IN_NAME.matcher(name).find()) {
            throw new IllegalArgumentException("sheet name must not contain [ ] : * ? / \\ : '" + name + "'");
        }
        return name;
    }
}
