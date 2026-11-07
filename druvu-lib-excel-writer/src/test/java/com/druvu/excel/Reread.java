package com.druvu.excel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** The independent reader: what the library wrote, opened with POI. */
final class Reread {

    private Reread() {}

    static XSSFWorkbook of(Sheet<?> sheet) {
        var out = new ByteArrayOutputStream();
        sheet.writeTo(out);
        return of(out.toByteArray());
    }

    static XSSFWorkbook of(Workbook workbook) {
        var out = new ByteArrayOutputStream();
        workbook.writeTo(out);
        return of(out.toByteArray());
    }

    static XSSFWorkbook of(byte[] bytes) {
        try {
            return new XSSFWorkbook(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A cell of the first sheet; row 0 is the header. */
    static XSSFCell cell(XSSFWorkbook workbook, int row, int column) {
        return workbook.getSheetAt(0).getRow(row).getCell(column);
    }
}
