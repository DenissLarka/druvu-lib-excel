package com.druvu.excel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.List;
import org.testng.annotations.Test;

public class TestWorkbook {

    @Test
    public void sheetsAppearInTheOrderAdded() throws IOException {
        var workbook = Excel.workbook()
                .sheet(Excel.sheet("Invoices", Invoice.BOTH).column("Customer", Invoice::customer))
                .sheet(Excel.sheet("Years", List.of(2025, 2026)).column("Year", year -> year));

        try (var reread = Reread.of(workbook)) {
            assertThat(reread.getNumberOfSheets()).isEqualTo(2);
            assertThat(reread.getSheetName(0)).isEqualTo("Invoices");
            assertThat(reread.getSheetName(1)).isEqualTo("Years");
            assertThat(reread.getSheet("Years").getRow(2).getCell(0).getNumericCellValue())
                    .isEqualTo(2026d);
        }
    }

    @Test
    public void twoSheetsCannotShareAName() {
        var workbook = Excel.workbook().sheet(Excel.sheet("Invoices", Invoice.BOTH));

        assertThatThrownBy(() -> workbook.sheet(Excel.sheet("INVOICES", Invoice.BOTH)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'INVOICES'");
    }

    @Test
    public void anEmptyWorkbookIsRefused() {
        assertThatThrownBy(() -> Reread.of(Excel.workbook())).isInstanceOf(IllegalStateException.class);
    }
}
