package com.druvu.excel.reader;

import java.io.Serial;

/**
 * A cell that does not hold what was asked of it: empty, an error such as {@code #N/A}, or a value that cannot be
 * converted without guessing. The message names the cell, its header and what it holds, for example {@code Expenses!D14
 * 'Amount': text "12,50" is not a number}. Catch it to refuse one row and read on.
 *
 * @author Deniss Larka <br>
 *     on 07 Nov 2026
 */
public final class CellFormatException extends IllegalArgumentException {

    @Serial
    private static final long serialVersionUID = 1L;

    CellFormatException(String message) {
        super(message);
    }
}
