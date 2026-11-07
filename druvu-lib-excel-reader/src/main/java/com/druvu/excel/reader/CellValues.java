package com.druvu.excel.reader;

import java.math.BigDecimal;
import java.util.Set;
import org.dhatim.fastexcel.reader.Cell;

/** Turns one engine cell into a {@link CellValue}: the only class that knows the engine's cell model. */
final class CellValues {

    /**
     * The values Excel shows when a formula fails. The engine hands a formula's error and a formula's text result over
     * in the same shape, so a text result spelled exactly like one of these is taken for the error - the safe side.
     */
    private static final Set<String> ERROR_CODES = Set.of(
            "#NULL!",
            "#DIV/0!",
            "#VALUE!",
            "#REF!",
            "#NAME?",
            "#NUM!",
            "#N/A",
            "#GETTING_DATA",
            "#SPILL!",
            "#CALC!",
            "#FIELD!",
            "#BLOCKED!",
            "#CONNECT!",
            "#BUSY!",
            "#UNKNOWN!");

    private CellValues() {}

    /** The value of a cell the row has; a cell it does not have is {@link CellValue#EMPTY}. */
    static CellValue of(Cell cell) {
        return switch (cell.getType()) {
            case EMPTY -> CellValue.EMPTY;
            case STRING -> new CellValue.Text(cell.asString());
            case BOOLEAN -> new CellValue.Logical(cell.asBoolean());
            case ERROR -> new CellValue.ExcelError(cell.getRawValue());
            case NUMBER -> number(cell, cell.asNumber());
            case FORMULA -> formulaResult(cell);
        };
    }

    /** A formula's saved result, typed by what the file says it is. */
    private static CellValue formulaResult(Cell cell) {
        return switch (cell.getValue()) {
            case null -> new CellValue.NoResult(cell.getFormula());
            case BigDecimal number -> number(cell, number);
            case Boolean logical -> new CellValue.Logical(logical);
            case String text when ERROR_CODES.contains(text) -> new CellValue.ExcelError(text);
            case String text -> new CellValue.Text(text);
            default ->
                throw new IllegalStateException("unexpected formula result in " + cell.getAddress() + ": "
                        + cell.getValue().getClass().getName());
        };
    }

    private static CellValue number(Cell cell, BigDecimal value) {
        Integer formatId = cell.getDataFormatId();
        String formatCode = cell.getDataFormatString();
        if (DateFormats.isDateOrTime(formatId, formatCode)) {
            return new CellValue.DateTime(cell.asDate(), DateFormats.isTimeOfDayOnly(formatCode));
        }
        return new CellValue.Numeric(value);
    }
}
