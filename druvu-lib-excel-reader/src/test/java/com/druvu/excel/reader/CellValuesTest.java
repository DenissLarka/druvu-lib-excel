package com.druvu.excel.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** The cells a spreadsheet program writes: rich and inline text, formula results, errors, dates by their format. */
public class CellValuesTest {

    private List<Row> rows;

    @BeforeClass
    public void writeTheKinds() {
        XlsxFixture workbook = XlsxFixture.workbook()
                .sharedStrings("<r><t>Fami</t></r><r><rPr><b/></rPr><t>lie</t></r><rPh><t>ignored</t></rPh>");
        int customDate = workbook.style("dd.mm.yyyy");
        int builtInDate = workbook.style(14);
        int timeOfDay = workbook.style("h:mm");
        int dateAndTime = workbook.style(22);
        Path file = workbook.sheet("Cells", """
                        <row r="1">%s%s</row>\
                        <row r="2">%s<c r="B2" t="s"><v>0</v></c></row>\
                        <row r="3">%s<c r="B3" t="inlineStr"><is><t>Atelier Nord</t></is></c></row>\
                        <row r="4">%s<c r="B4"><f>A1*2</f><v>24.6</v></c></row>\
                        <row r="5">%s<c r="B5" t="str"><f>"x"&amp;"y"</f><v>xy</v></c></row>\
                        <row r="6">%s<c r="B6" t="b"><f>TRUE()</f><v>1</v></c></row>\
                        <row r="7">%s<c r="B7" t="e"><f>1/0</f><v>#DIV/0!</v></c></row>\
                        <row r="8">%s<c r="B8" t="e"><v>#N/A</v></c></row>\
                        <row r="9">%s<c r="B9"><f>SUM(A1:A3)</f></c></row>\
                        <row r="10">%s<c r="B10" s="%d"><v>45930</v></c></row>\
                        <row r="11">%s<c r="B11" s="%d"><v>45930</v></c></row>\
                        <row r="12">%s<c r="B12" s="%d"><v>0.5</v></c></row>\
                        <row r="13">%s<c r="B13" s="%d"><v>45930.75</v></c></row>\
                        <row r="14">%s<c r="B14"><v>45930</v></c></row>\
                        """.formatted(
                                inline("A1", "Kind"),
                                inline("B1", "Value"),
                                inline("A2", "rich text"),
                                inline("A3", "inline text"),
                                inline("A4", "formula number"),
                                inline("A5", "formula text"),
                                inline("A6", "formula boolean"),
                                inline("A7", "formula error"),
                                inline("A8", "error"),
                                inline("A9", "formula without result"),
                                inline("A10", "custom date"),
                                customDate,
                                inline("A11", "built-in date"),
                                builtInDate,
                                inline("A12", "time of day"),
                                timeOfDay,
                                inline("A13", "date and time"),
                                dateAndTime,
                                inline("A14", "plain number")))
                .save(OutputFiles.path("cell-kinds.xlsx"));
        rows = ExcelReader.read(file).sheet("Cells").rows();
    }

    @Test
    public void textJoinsRichRunsAndReadsInlineStrings() {
        assertThat(kind("rich text").text("Value")).isEqualTo("Familie");
        assertThat(kind("inline text").text("Value")).isEqualTo("Atelier Nord");
    }

    @Test
    public void aFormulaGivesItsSavedResult() {
        assertThat(kind("formula number").decimal("Value")).isEqualTo(new BigDecimal("24.6"));
        assertThat(kind("formula text").text("Value")).isEqualTo("xy");
        assertThat(kind("formula boolean").bool("Value")).isTrue();
    }

    @Test
    public void errorsAndMissingResultsAreRefused() {
        assertThatThrownBy(() -> kind("formula error").decimal("Value"))
                .hasMessage("Cells!B7 'Value' holds the error #DIV/0!");
        assertThatThrownBy(() -> kind("error").text("Value")).hasMessage("Cells!B8 'Value' holds the error #N/A");
        assertThatThrownBy(() -> kind("formula without result").decimal("Value"))
                .hasMessage("Cells!B9 'Value' holds the formula =SUM(A1:A3) without its result - open the file in"
                        + " Excel and save it");
    }

    @Test
    public void aNumberIsADateWhenItsFormatSaysSo() {
        assertThat(kind("custom date").date("Value")).isEqualTo(LocalDate.of(2025, 9, 30));
        assertThat(kind("built-in date").date("Value")).isEqualTo(LocalDate.of(2025, 9, 30));
        assertThat(kind("date and time").timestamp("Value")).isEqualTo(LocalDateTime.of(2025, 9, 30, 18, 0));
        assertThat(kind("time of day").text("Value")).isEqualTo("12:00");
        assertThatThrownBy(() -> kind("plain number").date("Value"))
                .hasMessage("Cells!B14 'Value' holds the number 45930, not a date - format the column as Date in"
                        + " Excel");
    }

    @Test
    public void the1904DateSystemIsHonoured() {
        XlsxFixture workbook = XlsxFixture.workbook().date1904();
        int date = workbook.style(14);
        Path file = workbook.sheet(
                        "Mac",
                        "<row r=\"1\">" + inline("A1", "Day") + "</row>" + "<row r=\"2\"><c r=\"A2\" s=\"" + date
                                + "\"><v>44468</v></c></row>")
                .save(OutputFiles.path("date-1904.xlsx"));

        assertThat(ExcelReader.read(file).firstSheet().rows(row -> row.date("Day")))
                .containsExactly(LocalDate.of(2025, 9, 30));
    }

    @Test
    public void blankRowsAndMissingCellsAreEmpty() {
        Path file = XlsxFixture.workbook()
                .sheet("Sparse", """
                        <row r="1"></row>\
                        <row r="2">%s%s</row>\
                        <row r="3"><c r="C3"><v>12.5</v></c></row>\
                        <row r="5"><c r="A5" s="0"/></row>\
                        <row r="6">%s<c r="C6"><v>7</v></c></row>\
                        """.formatted(
                                inline("A2", " Date "), inline("C2", "Amount"), inline("A6", "2026-09-30")))
                .save(OutputFiles.path("sparse.xlsx"));
        List<Row> sparse = ExcelReader.read(file).firstSheet().rows();

        assertThat(sparse).extracting(Row::number).containsExactly(3, 6);
        assertThat(sparse.getFirst().isEmpty("Date")).isTrue();
        assertThat(sparse.getFirst().decimal("Amount")).isEqualTo(new BigDecimal("12.5"));
        assertThat(sparse.get(1).date("Date")).isEqualTo(LocalDate.of(2026, 9, 30));
    }

    @Test
    public void aRepeatedHeaderIsRefusedWhenRead() {
        Path file = XlsxFixture.workbook()
                .sheet("Twice", """
                        <row r="1">%s%s%s</row>\
                        <row r="2">%s<c r="B2"><v>1.5</v></c><c r="C2"><v>2.5</v></c></row>\
                        """.formatted(
                        inline("A1", "Name"), inline("B1", "Amount"), inline("C1", "Amount"), inline("A2", "Keller")))
                .save(OutputFiles.path("repeated-header.xlsx"));
        Row row = ExcelReader.read(file).firstSheet().rows().getFirst();

        assertThat(row.text("Name")).isEqualTo("Keller");
        assertThatThrownBy(() -> row.decimal("Amount"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sheet 'Twice' has the header 'Amount' twice - rename one of them in Excel");
    }

    private Row kind(String kind) {
        return rows.stream()
                .filter(row -> row.text("Kind").equals(kind))
                .findFirst()
                .orElseThrow();
    }

    private static String inline(String cell, String text) {
        return "<c r=\"" + cell + "\" t=\"inlineStr\"><is><t>" + text + "</t></is></c>";
    }
}
