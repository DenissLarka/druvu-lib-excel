package com.druvu.excel.reader;

import java.nio.file.Path;
import java.util.Objects;

/**
 * The front door for reading. One sheet's rows, each turned into a record of yours:
 *
 * <pre>{@code
 * List<Expense> expenses = ExcelReader.read(Path.of("household.xlsx"))
 *         .sheet("Expenses")
 *         .rows(row -> new Expense(row.date("Date"), row.text("Description"), row.decimal("Amount")));
 * }</pre>
 *
 * <p>The first row of a sheet holds the headers; every other row is read by them. A value is converted only where no
 * guess is involved - see {@link Row}.
 *
 * @author Deniss Larka <br>
 *     on 07 Nov 2026
 */
public final class ExcelReader {

    private ExcelReader() {}

    /** An xlsx file to read. Nothing is opened until rows or sheet names are asked for. */
    public static ExcelFile read(Path file) {
        Objects.requireNonNull(file, "file");
        return new ExcelFile(file);
    }
}
