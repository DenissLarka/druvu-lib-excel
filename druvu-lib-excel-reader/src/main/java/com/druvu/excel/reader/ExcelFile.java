package com.druvu.excel.reader;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.dhatim.fastexcel.reader.Sheet;

/**
 * An xlsx file, from {@link ExcelReader#read}. It holds no open file: each call that needs the content opens the file,
 * reads it and closes it, so there is nothing to close.
 *
 * @author Deniss Larka <br>
 *     on 07 Nov 2026
 */
public final class ExcelFile {

    private final Path file;

    ExcelFile(Path file) {
        this.file = file;
    }

    /** The sheets' names, in the order of their tabs. */
    public List<String> sheetNames() {
        return Workbooks.read(
                file, workbook -> workbook.getSheets().map(Sheet::getName).toList());
    }

    /** The sheet with this name, exactly as on its tab. An unknown name is refused when its rows are read. */
    public ExcelSheet sheet(String name) {
        Objects.requireNonNull(name, "name");
        return new ExcelSheet(file, name);
    }

    /** The first sheet, whatever its name. */
    public ExcelSheet firstSheet() {
        return new ExcelSheet(file, null);
    }
}
