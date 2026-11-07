package com.druvu.excel.reader;

/** Where a cell sits, for messages: {@code Expenses!D14 'Amount'}. */
record CellPosition(String sheet, int row, int column, String header) {

    @Override
    public String toString() {
        return sheet + "!" + columnLetters(column) + row + " '" + header + "'";
    }

    /** Excel's column name for a zero-based index: 0 is A, 25 is Z, 26 is AA. */
    static String columnLetters(int column) {
        StringBuilder letters = new StringBuilder();
        for (int rest = column + 1; rest > 0; rest = (rest - 1) / 26) {
            letters.insert(0, (char) ('A' + (rest - 1) % 26));
        }
        return letters.toString();
    }
}
