package com.druvu.excel.reader;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * What one cell holds, as Excel sees it - the engine's cell model ends in {@link CellValues}, the conversions to Java
 * types live in {@link CellConversions}.
 */
sealed interface CellValue {

    CellValue EMPTY = new Empty();

    /** Text, exactly as stored. */
    record Text(String text) implements CellValue {}

    /** A number whose cell format is not a date format; the value exactly as the file stores it. */
    record Numeric(BigDecimal value) implements CellValue {}

    /** A number whose cell format is a date or a time format, already converted with the file's date system. */
    record DateTime(LocalDateTime value, boolean timeOfDayOnly) implements CellValue {}

    /** TRUE or FALSE. */
    record Logical(boolean value) implements CellValue {}

    /** An error Excel shows in place of a value, such as {@code #N/A} or {@code #DIV/0!}. */
    record ExcelError(String code) implements CellValue {}

    /** A formula saved without its result - the file was written by a program that does not calculate. */
    record NoResult(String formula) implements CellValue {}

    /** Nothing at all. */
    record Empty() implements CellValue {}
}
