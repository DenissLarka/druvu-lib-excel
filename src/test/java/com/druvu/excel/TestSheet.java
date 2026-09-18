package com.druvu.excel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.testng.annotations.Test;

public class TestSheet {

    private static Sheet<Invoice> invoices() {
        return Excel.sheet("Invoices", Invoice.BOTH);
    }

    @Test
    public void headersComeFirstBoldInDeclarationOrder() throws IOException {
        var sheet = invoices().column("Number", Invoice::number).column("Customer", Invoice::customer);

        try (var reread = Reread.of(sheet)) {
            assertThat(Reread.cell(reread, 0, 0).getStringCellValue()).isEqualTo("Number");
            assertThat(Reread.cell(reread, 0, 1).getStringCellValue()).isEqualTo("Customer");
            var header = Reread.cell(reread, 0, 1).getCellStyle();
            assertThat(header.getFont().getBold()).isTrue();
            assertThat(header.getAlignment()).isEqualTo(HorizontalAlignment.CENTER);
        }
    }

    @Test
    public void oneRowPerElement() throws IOException {
        var sheet = invoices().column("Customer", Invoice::customer);

        try (var reread = Reread.of(sheet)) {
            assertThat(reread.getSheet("Invoices").getLastRowNum()).isEqualTo(2);
            assertThat(Reread.cell(reread, 1, 0).getStringCellValue()).isEqualTo("Familie Keller");
            assertThat(Reread.cell(reread, 2, 0).getStringCellValue()).isEqualTo("Atelier Morel");
        }
    }

    @Test
    public void valuesKeepTheirExcelType() throws IOException {
        var sheet = invoices()
                .column("Number", Invoice::number)
                .column("Amount", Invoice::amount)
                .column("Paid", invoice -> invoice.status() == Invoice.Status.PAID)
                .column("Status", Invoice::status)
                .column("Ratio", invoice -> 0.25d)
                .column("Count", invoice -> 3_000_000_000L);

        try (var reread = Reread.of(sheet)) {
            assertThat(Reread.cell(reread, 1, 0).getNumericCellValue()).isEqualTo(20260017d);
            assertThat(BigDecimal.valueOf(Reread.cell(reread, 1, 1).getNumericCellValue()))
                    .isEqualByComparingTo("1250.50");
            assertThat(Reread.cell(reread, 1, 2).getBooleanCellValue()).isFalse();
            assertThat(Reread.cell(reread, 2, 2).getBooleanCellValue()).isTrue();
            assertThat(Reread.cell(reread, 1, 3).getStringCellValue()).isEqualTo("OPEN");
            assertThat(Reread.cell(reread, 1, 4).getNumericCellValue()).isEqualTo(0.25d);
            assertThat(Reread.cell(reread, 1, 5).getNumericCellValue()).isEqualTo(3_000_000_000d);
        }
    }

    @Test
    public void datesReadAsDatesWithoutAnyStyle() throws IOException {
        var sheet = invoices().column("Due", Invoice::due).column("Booked", Invoice::booked);

        try (var reread = Reread.of(sheet)) {
            var due = Reread.cell(reread, 1, 0);
            assertThat(due.getLocalDateTimeCellValue().toLocalDate()).isEqualTo(LocalDate.of(2026, 10, 18));
            assertThat(due.getCellStyle().getDataFormatString()).isEqualTo("yyyy-mm-dd");
            var booked = Reread.cell(reread, 1, 1);
            assertThat(booked.getLocalDateTimeCellValue()).isEqualTo(LocalDateTime.of(2026, 9, 18, 14, 30, 5));
            assertThat(booked.getCellStyle().getDataFormatString()).isEqualTo("yyyy-mm-dd hh:mm:ss");
        }
    }

    @Test
    public void aWholeNumberGetsNoThousandsSeparator() throws IOException {
        var sheet = invoices().column("Number", Invoice::number);

        try (var reread = Reread.of(sheet)) {
            assertThat(Reread.cell(reread, 1, 0).getCellStyle().getDataFormatString())
                    .isEqualTo("General");
        }
    }

    @Test
    public void aHighlightOnADateColumnIsStillADate() throws IOException {
        var sheet = invoices().column("Due", Style.NONE.withFill(Fill.RED), Invoice::due);

        try (var reread = Reread.of(sheet)) {
            var style = Reread.cell(reread, 1, 0).getCellStyle();
            assertThat(style.getDataFormatString()).isEqualTo("yyyy-mm-dd");
            assertThat(style.getFillPattern()).isEqualTo(FillPatternType.SOLID_FOREGROUND);
        }
    }

    @Test
    public void anExplicitFormatWins() throws IOException {
        var sheet = invoices().column("Amount", Style.MONEY, Invoice::amount);

        try (var reread = Reread.of(sheet)) {
            assertThat(Reread.cell(reread, 1, 0).getCellStyle().getDataFormatString())
                    .isEqualTo("#,##0.00");
        }
    }

    @Test
    public void aStyleCanBeChosenRowByRow() throws IOException {
        var sheet = invoices()
                .column(
                        "Amount",
                        invoice -> invoice.status() == Invoice.Status.OPEN
                                ? Style.MONEY.withFill(Fill.ORANGE)
                                : Style.MONEY,
                        Invoice::amount);

        try (var reread = Reread.of(sheet)) {
            assertThat(Reread.cell(reread, 1, 0).getCellStyle().getFillPattern())
                    .isEqualTo(FillPatternType.SOLID_FOREGROUND);
            assertThat(Reread.cell(reread, 2, 0).getCellStyle().getFillPattern())
                    .isEqualTo(FillPatternType.NO_FILL);
        }
    }

    @Test
    public void aMissingValueIsAnEmptyCell() throws IOException {
        var sheet = invoices()
                .column("Customer", Invoice::customer)
                .column("Note", invoice -> invoice.status() == Invoice.Status.PAID ? "thanks" : null);

        try (var reread = Reread.of(sheet)) {
            var empty = Reread.cell(reread, 1, 1);
            assertThat(empty == null || empty.getCellType() == CellType.BLANK).isTrue();
            assertThat(Reread.cell(reread, 2, 1).getStringCellValue()).isEqualTo("thanks");
        }
    }

    @Test
    public void anOptionalIsShownAsWhatItHoldsOrLeftEmpty() throws IOException {
        var sheet = invoices()
                .column("Customer", Invoice::customer)
                .column(
                        "Paid on",
                        invoice -> invoice.status() == Invoice.Status.PAID
                                ? Optional.of(invoice.due())
                                : Optional.empty());

        try (var reread = Reread.of(sheet)) {
            var empty = Reread.cell(reread, 1, 1);
            assertThat(empty == null || empty.getCellType() == CellType.BLANK).isTrue();
            var paid = Reread.cell(reread, 2, 1);
            assertThat(paid.getLocalDateTimeCellValue().toLocalDate()).isEqualTo(LocalDate.of(2026, 9, 1));
            assertThat(paid.getCellStyle().getDataFormatString()).isEqualTo("yyyy-mm-dd");
        }
    }

    @Test
    public void rowsCanComeFromAStream() throws IOException {
        var sheet = Excel.sheet("Invoices", Invoice.BOTH.stream().filter(i -> i.status() == Invoice.Status.PAID))
                .column("Customer", Invoice::customer);

        try (var reread = Reread.of(sheet)) {
            assertThat(reread.getSheet("Invoices").getLastRowNum()).isEqualTo(1);
            assertThat(Reread.cell(reread, 1, 0).getStringCellValue()).isEqualTo("Atelier Morel");
        }
    }

    @Test
    public void noRowsStillGivesTheHeader() throws IOException {
        var sheet = Excel.sheet("Invoices", List.<Invoice>of()).column("Customer", Invoice::customer);

        try (var reread = Reread.of(sheet)) {
            assertThat(reread.getSheet("Invoices").getLastRowNum()).isZero();
            assertThat(Reread.cell(reread, 0, 0).getStringCellValue()).isEqualTo("Customer");
        }
    }

    @Test
    public void saveWritesTheFile() throws IOException {
        Path file = Files.createTempFile("druvu-lib-excel", ".xlsx");
        try {
            invoices().column("Customer", Invoice::customer).save(file);

            try (var reread = Reread.of(Files.readAllBytes(file))) {
                assertThat(Reread.cell(reread, 1, 0).getStringCellValue()).isEqualTo("Familie Keller");
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void aRefusedWriteLeavesNoFileBehind() throws IOException {
        Path directory = Files.createTempDirectory("druvu-lib-excel");
        Path file = directory.resolve("report.xlsx");
        try {
            var sheet = invoices().column("Booked at", invoice -> Instant.EPOCH);

            assertThatThrownBy(() -> sheet.save(file)).isInstanceOf(IllegalArgumentException.class);

            try (var left = Files.list(directory)) {
                assertThat(left).isEmpty();
            }
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void aRefusedWriteLeavesAnExistingFileUntouched() throws IOException {
        Path file = Files.createTempFile("druvu-lib-excel", ".xlsx");
        try {
            Files.writeString(file, "last month's report");
            var sheet = invoices().column("Booked at", invoice -> Instant.EPOCH);

            assertThatThrownBy(() -> sheet.save(file)).isInstanceOf(IllegalArgumentException.class);

            assertThat(file).hasContent("last month's report");
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void aValueExcelCannotHoldIsRefusedByColumnName() {
        var sheet = invoices().column("Booked at", invoice -> Instant.EPOCH);

        assertThatThrownBy(() -> Reread.of(sheet))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'Booked at'")
                .hasMessageContaining("java.time.Instant");
    }

    @Test
    public void aSheetWithoutColumnsIsRefused() {
        assertThatThrownBy(() -> Reread.of(invoices()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("'Invoices'");
    }

    @Test
    public void namesExcelWouldRejectAreRefused() {
        assertThatThrownBy(() -> Excel.sheet("Q3/Q4", Invoice.BOTH)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Excel.sheet(" ", Invoice.BOTH)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Excel.sheet("x".repeat(32), Invoice.BOTH))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
