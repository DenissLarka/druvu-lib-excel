package com.druvu.excel.reader;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Sheet;

/**
 * One sheet of an {@link ExcelFile}: its first row holds the headers, every other row is a {@link Row}.
 *
 * @author Deniss Larka <br>
 *     on 07 Nov 2026
 */
public final class ExcelSheet {

    private final Path file;
    private final String name; // null: the first sheet, whatever its name

    ExcelSheet(Path file, String name) {
        this.file = file;
        this.name = name;
    }

    /**
     * The rows below the header row, top to bottom. A row with nothing in it is left out, so blank lines between the
     * data and below it never reach the caller.
     */
    public List<Row> rows() {
        return Workbooks.read(file, workbook -> SheetRows.read(find(workbook)));
    }

    /** The rows below the header row, each turned into a value of yours - typically a record. */
    public <T> List<T> rows(Function<? super Row, ? extends T> rowToValue) {
        Objects.requireNonNull(rowToValue, "rowToValue");
        return rows().stream().<T>map(rowToValue).toList();
    }

    private Sheet find(ReadableWorkbook workbook) {
        if (name == null) {
            return workbook.getFirstSheet();
        }
        return workbook.findSheet(name)
                .orElseThrow(() -> new IllegalArgumentException("no sheet '" + name + "' in " + file + " - its sheets: "
                        + String.join(
                                ", ", workbook.getSheets().map(Sheet::getName).toList())));
    }
}
