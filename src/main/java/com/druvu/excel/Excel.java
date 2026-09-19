package com.druvu.excel;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * The front door. One sheet, straight to a file:
 *
 * <pre>{@code
 * Excel.sheet("Customers", customers)
 *         .column("Name", Customer::name)
 *         .column("Balance", Style.MONEY, Customer::balance)
 *         .save(Path.of("customers.xlsx"));
 * }</pre>
 *
 * <p>Several sheets go through {@link #workbook()}.
 *
 * @author Deniss Larka <br>
 *     on 18 Sep 2026
 */
public final class Excel {

    private Excel() {}

    /** A sheet with one row per element. */
    public static <T> Sheet<T> sheet(String name, Iterable<? extends T> rows) {
        return new Sheet<>(name, rows);
    }

    /** A sheet with one row per element. A stream can be read once, so such a sheet can be written once. */
    public static <T> Sheet<T> sheet(String name, Stream<T> rows) {
        Objects.requireNonNull(rows, "rows");
        return new Sheet<>(name, rows::iterator);
    }

    /** An empty workbook, for a file with several sheets. */
    public static Workbook workbook() {
        return new Workbook();
    }
}
