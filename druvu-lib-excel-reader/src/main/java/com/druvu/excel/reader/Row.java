package com.druvu.excel.reader;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * One row of a sheet, read by its headers. Each accessor returns the cell under that header, converted only where no
 * guess is involved:
 *
 * <ul>
 *   <li>a number comes back as Excel shows it, at most 15 significant digits; a number typed as text is read when it is
 *       plain digits with a dot ({@code -1234.50}) - never with a thousands separator, a comma or a currency sign
 *   <li>a date comes from a cell Excel formats as a date, or from ISO text ({@code 2026-09-30}); never from
 *       {@code 30/09/2026}, which could be either order
 *   <li>a formula gives the result Excel saved with the file
 * </ul>
 *
 * <p>An empty cell, a cell holding an error ({@code #N/A}) or a value the accessor cannot take exactly is refused with
 * a {@link CellFormatException} naming the cell. A column that may be empty is checked with {@link #isEmpty} first. An
 * unknown header is refused with an {@link IllegalArgumentException} listing the headers the sheet has.
 *
 * @author Deniss Larka <br>
 *     on 07 Nov 2026
 */
public final class Row {

    private final String sheet;
    private final int number;
    private final HeaderIndex headers;
    private final List<CellValue> cells;

    Row(String sheet, int number, HeaderIndex headers, List<CellValue> cells) {
        this.sheet = sheet;
        this.number = number;
        this.headers = headers;
        this.cells = List.copyOf(cells);
    }

    /** The row's number as Excel shows it on the left, for messages such as "row 14: no amount". */
    public int number() {
        return number;
    }

    /** Whether the cell under this header holds nothing, or only spaces. */
    public boolean isEmpty(String header) {
        return CellConversions.isEmpty(cell(header));
    }

    /** The cell as text: a number in its plain form ({@code 1001}), a date in ISO form, a boolean as TRUE or FALSE. */
    public String text(String header) {
        return CellConversions.text(cell(header), at(header));
    }

    /** The cell as a number, at most 15 significant digits, the way Excel shows it. */
    public BigDecimal decimal(String header) {
        return CellConversions.decimal(cell(header), at(header));
    }

    /** The cell as a whole number. A fraction, or a number beyond {@code long}, is refused. */
    public long integer(String header) {
        return CellConversions.integer(cell(header), at(header));
    }

    /** The cell as a date. A cell holding a date and a time of day is refused: read it with {@link #timestamp}. */
    public LocalDate date(String header) {
        return CellConversions.date(cell(header), at(header));
    }

    /** The cell as a date and a time of day. */
    public LocalDateTime timestamp(String header) {
        return CellConversions.timestamp(cell(header), at(header));
    }

    /** The cell as a boolean: Excel's TRUE or FALSE, or the text true or false in any case. */
    public boolean bool(String header) {
        return CellConversions.bool(cell(header), at(header));
    }

    @Override
    public String toString() {
        return "row " + number + " of sheet '" + sheet + "'";
    }

    private CellValue cell(String header) {
        int column = headers.column(header);
        return column < cells.size() ? cells.get(column) : CellValue.EMPTY;
    }

    private CellPosition at(String header) {
        return new CellPosition(sheet, number, headers.column(header), header);
    }
}
