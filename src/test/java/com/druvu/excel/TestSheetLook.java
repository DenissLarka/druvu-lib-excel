package com.druvu.excel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;
import org.testng.annotations.Test;

/** The look and the guarantees every sheet gets without asking. */
public class TestSheetLook {

    private static final double CHARACTER = 256d;

    @Test
    public void theHeaderStaysInViewWhileScrolling() throws IOException {
        var sheet = Excel.sheet("Invoices", Invoice.BOTH).column("Customer", Invoice::customer);

        try (var reread = Reread.of(sheet)) {
            var pane = reread.getSheetAt(0).getPaneInformation();
            assertThat(pane.isFreezePane()).isTrue();
            assertThat(pane.getHorizontalSplitPosition()).isEqualTo((short) 1);
            assertThat(pane.getVerticalSplitPosition()).isEqualTo((short) 0);
        }
    }

    @Test
    public void everyColumnCanBeFilteredOverAllRows() throws IOException {
        var sheet = Excel.sheet("Invoices", Invoice.BOTH)
                .column("Customer", Invoice::customer)
                .column("Amount", Style.MONEY, Invoice::amount);

        try (var reread = Reread.of(sheet)) {
            assertThat(reread.getSheetAt(0).getCTWorksheet().getAutoFilter().getRef())
                    .isEqualTo("A1:B3");
        }
    }

    @Test
    public void columnsAreAsWideAsWhatTheyHold() throws IOException {
        var sheet = Excel.sheet("Invoices", Invoice.BOTH)
                .column("No", invoice -> 7)
                .column("Customer", invoice -> invoice.customer() + " and a rather long remark")
                .column("Essay", invoice -> "x".repeat(500));

        try (var reread = Reread.of(sheet)) {
            var written = reread.getSheetAt(0);
            double narrow = written.getColumnWidth(0) / CHARACTER;
            double wide = written.getColumnWidth(1) / CHARACTER;
            double capped = written.getColumnWidth(2) / CHARACTER;
            assertThat(narrow).isBetween(5d, 9d);
            assertThat(wide).isGreaterThanOrEqualTo("Familie Keller and a rather long remark".length());
            assertThat(capped).isBetween(59d, 61d);
        }
    }

    @Test
    public void aLongSheetArrivesWhole() throws IOException {
        int rows = 2 * SheetWriter.BATCH + 500;
        var sheet = Excel.sheet("Numbers", IntStream.rangeClosed(1, rows).boxed())
                .column("N", n -> n)
                .column("Half", Style.MONEY, n -> BigDecimal.valueOf(n).movePointLeft(1));

        try (var reread = Reread.of(sheet)) {
            var written = reread.getSheetAt(0);
            assertThat(written.getLastRowNum()).isEqualTo(rows);
            assertThat(written.getRow(SheetWriter.BATCH).getCell(0).getNumericCellValue())
                    .isEqualTo(SheetWriter.BATCH);
            assertThat(written.getRow(rows).getCell(0).getNumericCellValue()).isEqualTo(rows);
            assertThat(written.getRow(rows).getCell(1).getCellStyle().getDataFormatString())
                    .isEqualTo("#,##0.00");
            assertThat(written.getCTWorksheet().getAutoFilter().getRef()).isEqualTo("A1:B" + (rows + 1));
            assertThat(written.getColumnWidth(1) / CHARACTER).isGreaterThanOrEqualTo("100.00".length());
        }
    }

    /**
     * The engine keeps the file's shared table of strings in memory until the last row, so text that went there would
     * stop a long sheet from streaming (measured: a million distinct texts exhaust a 64 MB heap). Only headers may.
     */
    @Test
    public void rowTextStaysOutOfTheSharedStringTable() throws IOException {
        var sheet = Excel.sheet("Invoices", Invoice.BOTH)
                .column("Customer", Invoice::customer)
                .column("Status", Invoice::status);

        try (var reread = Reread.of(sheet)) {
            assertThat(reread.getSharedStringSource().getUniqueCount()).isEqualTo(2);
            assertThat(Reread.cell(reread, 1, 0).getStringCellValue()).isEqualTo("Familie Keller");
            assertThat(Reread.cell(reread, 2, 1).getStringCellValue()).isEqualTo("PAID");
        }
    }

    @Test
    public void exactlyOneBatchArrivesWhole() throws IOException {
        var sheet = Excel.sheet(
                        "Numbers", IntStream.rangeClosed(1, SheetWriter.BATCH).boxed())
                .column("N", n -> n);

        try (var reread = Reread.of(sheet)) {
            assertThat(reread.getSheetAt(0).getLastRowNum()).isEqualTo(SheetWriter.BATCH);
        }
    }

    @Test
    public void anAmountIsWrittenDigitForDigit() throws IOException {
        var sheet = Excel.sheet("Amounts", List.of(new BigDecimal("1234567890123.45"), new BigDecimal("1E+3")))
                .column("Amount", amount -> amount);

        try (var reread = Reread.of(sheet)) {
            assertThat(new BigDecimal(Reread.cell(reread, 1, 0).getRawValue()))
                    .isEqualByComparingTo("1234567890123.45");
            assertThat(Reread.cell(reread, 2, 0).getNumericCellValue()).isEqualTo(1000d);
        }
    }

    @Test
    public void aNumberExcelWouldChangeIsRefused() {
        var sheet = Excel.sheet("Amounts", List.of(new BigDecimal("12345678901234567890.12")))
                .column("Amount", amount -> amount);

        assertThatThrownBy(() -> Reread.of(sheet))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'Amount'");
    }
}
