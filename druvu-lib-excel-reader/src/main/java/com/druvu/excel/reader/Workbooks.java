package com.druvu.excel.reader;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.dhatim.fastexcel.reader.ExcelReaderException;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.ReadingOptions;

/**
 * Opens an xlsx file for one task and closes it again. A file that is not an xlsx is refused by name before the engine
 * sees it, and the engine's own exceptions never leave this class.
 */
final class Workbooks {

    /** The cell formats are read too: they are what tells a date from a number. */
    private static final ReadingOptions WITH_FORMATS = new ReadingOptions(true, false);

    private Workbooks() {}

    /** What a task does with an open workbook. */
    @FunctionalInterface
    interface Task<T> {
        T apply(ReadableWorkbook workbook) throws IOException;
    }

    static <T> T read(Path file, Task<T> task) {
        refuseIfNotXlsx(file);
        try (ReadableWorkbook workbook = new ReadableWorkbook(file.toFile(), WITH_FORMATS)) {
            return task.apply(workbook);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + file, e);
        } catch (ExcelReaderException e) {
            throw new IllegalArgumentException("cannot read " + file + ": " + e.getMessage(), e);
        }
    }

    /** An xlsx is a zip; an .xls and a password-protected xlsx are both an older container that is not one. */
    private static void refuseIfNotXlsx(Path file) {
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("not a file: " + file);
        }
        byte[] head;
        try (InputStream in = Files.newInputStream(file)) {
            head = in.readNBytes(8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + file, e);
        }
        if (head.length == 8 && ReadableWorkbook.isOLE2Header(head)) {
            throw new IllegalArgumentException(file
                    + " is an .xls file (Excel 97-2003) or a password-protected one - save it in Excel as .xlsx,"
                    + " without a password");
        }
        if (head.length < 4 || !ReadableWorkbook.isOOXMLZipHeader(head)) {
            throw new IllegalArgumentException(file + " is not an xlsx file");
        }
    }
}
