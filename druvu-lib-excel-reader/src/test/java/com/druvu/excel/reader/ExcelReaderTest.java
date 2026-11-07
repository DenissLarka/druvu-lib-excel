package com.druvu.excel.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.druvu.excel.writer.Excel;
import com.druvu.excel.writer.Style;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** What the writer writes, the reader reads back: every type, by header, in order, with the sheets by name. */
public class ExcelReaderTest {

    record Invoice(
            String number,
            String customer,
            LocalDate due,
            LocalDateTime sent,
            BigDecimal amount,
            BigDecimal rate,
            long quantity,
            boolean paid,
            String note) {}

    record Customer(String name, String city) {}

    private static final List<Invoice> INVOICES = List.of(
            new Invoice(
                    "2026-041",
                    "Familie Keller",
                    LocalDate.of(2026, 9, 30),
                    LocalDateTime.of(2026, 9, 1, 14, 30),
                    new BigDecimal("1840.5"),
                    new BigDecimal("0.0525"),
                    12,
                    true,
                    "paid early"),
            new Invoice(
                    "2026-042",
                    "Atelier Nord",
                    LocalDate.of(2026, 10, 15),
                    LocalDateTime.of(2026, 9, 2, 9, 5, 30),
                    new BigDecimal("620"),
                    new BigDecimal("0.081"),
                    3,
                    false,
                    null));

    private Path file;

    @BeforeClass
    public void writeTheWorkbook() {
        file = OutputFiles.path("invoices.xlsx");
        Excel.workbook()
                .sheet(Excel.sheet("Invoices", INVOICES)
                        .column("Number", Invoice::number)
                        .column("Customer", Invoice::customer)
                        .column("Due", Invoice::due)
                        .column("Sent", Invoice::sent)
                        .column("Amount", Style.MONEY, Invoice::amount)
                        .column("Rate", Style.PERCENT, Invoice::rate)
                        .column("Quantity", Invoice::quantity)
                        .column("Paid", Invoice::paid)
                        .column("Note", Invoice::note))
                .sheet(Excel.sheet("Customers", List.of(new Customer("Familie Keller", "Basel")))
                        .column("Name", Customer::name)
                        .column("City", Customer::city))
                .save(file);
    }

    @Test
    public void readsBackEveryRowItsRecord() {
        List<Invoice> invoices = ExcelReader.read(file)
                .sheet("Invoices")
                .rows(row -> new Invoice(
                        row.text("Number"),
                        row.text("Customer"),
                        row.date("Due"),
                        row.timestamp("Sent"),
                        row.decimal("Amount"),
                        row.decimal("Rate"),
                        row.integer("Quantity"),
                        row.bool("Paid"),
                        row.isEmpty("Note") ? null : row.text("Note")));

        assertThat(invoices).isEqualTo(INVOICES);
    }

    @Test
    public void listsTheSheetsInTabOrder() {
        assertThat(ExcelReader.read(file).sheetNames()).containsExactly("Invoices", "Customers");
        assertThat(ExcelReader.read(file).firstSheet().rows()).hasSize(2);
        assertThat(ExcelReader.read(file).sheet("Customers").rows(row -> row.text("City")))
                .containsExactly("Basel");
    }

    @Test
    public void numbersRowsAsExcelDoes() {
        assertThat(ExcelReader.read(file).sheet("Invoices").rows(Row::number)).containsExactly(2, 3);
    }

    @Test
    public void showsAnyCellAsText() {
        Row first = ExcelReader.read(file).sheet("Invoices").rows().getFirst();

        assertThat(first.text("Amount")).isEqualTo("1840.5");
        assertThat(first.text("Due")).isEqualTo("2026-09-30");
        assertThat(first.text("Paid")).isEqualTo("TRUE");
    }

    @Test
    public void refusesAnEmptyCellNamingIt() {
        Row second = ExcelReader.read(file).sheet("Invoices").rows().get(1);

        assertThat(second.isEmpty("Note")).isTrue();
        assertThatThrownBy(() -> second.text("Note"))
                .isInstanceOf(CellFormatException.class)
                .hasMessage("Invoices!I3 'Note' is empty");
    }

    @Test
    public void refusesAnUnknownSheetListingTheSheets() {
        assertThatThrownBy(() -> ExcelReader.read(file).sheet("Expenses").rows())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("no sheet 'Expenses' in " + file + " - its sheets: Invoices, Customers");
    }

    @Test
    public void refusesAnUnknownHeaderListingTheHeaders() {
        Row first = ExcelReader.read(file).sheet("Customers").rows().getFirst();

        assertThatThrownBy(() -> first.text("Town"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sheet 'Customers' has no header 'Town' - its headers: Name, City");
    }

    @Test
    public void aSheetWithOnlyHeadersHasNoRows() {
        Path headersOnly = OutputFiles.path("headers-only.xlsx");
        Excel.sheet("Empty", List.<Customer>of()).column("Name", Customer::name).save(headersOnly);

        assertThat(ExcelReader.read(headersOnly).firstSheet().rows()).isEmpty();
    }
}
