package com.druvu.excel.reader;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/** Excel's column letters and the cell name in messages. */
public class CellPositionTest {

    @DataProvider
    Object[][] columns() {
        return new Object[][] {{0, "A"}, {25, "Z"}, {26, "AA"}, {27, "AB"}, {701, "ZZ"}, {702, "AAA"}};
    }

    @Test(dataProvider = "columns")
    public void columnLettersFollowExcel(int column, String letters) {
        assertThat(CellPosition.columnLetters(column)).isEqualTo(letters);
    }

    @Test
    public void namesSheetCellAndHeader() {
        assertThat(new CellPosition("Expenses", 14, 3, "Amount")).hasToString("Expenses!D14 'Amount'");
    }
}
