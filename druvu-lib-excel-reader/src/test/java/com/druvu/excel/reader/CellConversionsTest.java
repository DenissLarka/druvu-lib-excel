package com.druvu.excel.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.BiFunction;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/** The reading rules: exact conversions taken, guesses refused, every refusal naming the cell. */
public class CellConversionsTest {

    private static final CellPosition AT = new CellPosition("Expenses", 14, 3, "Amount");
    private static final LocalDateTime SEPT_30 = LocalDateTime.of(2026, 9, 30, 0, 0);

    @Test
    public void decimalIsTheNumberAsExcelShowsIt() {
        assertThat(CellConversions.decimal(numeric("0.30000000000000004"), AT)).isEqualTo(new BigDecimal("0.3"));
        assertThat(CellConversions.decimal(numeric("1840.50"), AT)).isEqualTo(new BigDecimal("1840.5"));
        assertThat(CellConversions.decimal(numeric("100"), AT)).hasToString("100");
        assertThat(CellConversions.decimal(numeric("123456789012345678"), AT).toPlainString())
                .isEqualTo("123456789012346000");
    }

    @Test
    public void decimalTakesPlainDigitsTypedAsText() {
        assertThat(CellConversions.decimal(text(" -1234.50 "), AT)).isEqualTo(new BigDecimal("-1234.50"));
    }

    @DataProvider
    Object[][] guessedNumbers() {
        return new Object[][] {{"12,50"}, {"1,234.50"}, {"€12"}, {"1e5"}, {"12."}};
    }

    @Test(dataProvider = "guessedNumbers")
    public void decimalRefusesTextThatNeedsAGuess(String typed) {
        assertThatThrownBy(() -> CellConversions.decimal(text(typed), AT))
                .isInstanceOf(CellFormatException.class)
                .hasMessage("Expenses!D14 'Amount': text \"" + typed
                        + "\" is not a number - expected digits with a dot, like -1234.50, or a number cell");
    }

    @Test
    public void decimalRefusesDatesAndBooleans() {
        assertThatThrownBy(() -> CellConversions.decimal(date(SEPT_30), AT))
                .hasMessage("Expenses!D14 'Amount' holds the date 2026-09-30, not a number");
        assertThatThrownBy(() -> CellConversions.decimal(new CellValue.Logical(true), AT))
                .hasMessage("Expenses!D14 'Amount' holds TRUE, not a number");
    }

    @Test
    public void integerTakesWholeNumbersOnly() {
        assertThat(CellConversions.integer(numeric("12.0"), AT)).isEqualTo(12L);
        assertThat(CellConversions.integer(text("42"), AT)).isEqualTo(42L);
        assertThatThrownBy(() -> CellConversions.integer(numeric("12.5"), AT))
                .hasMessage("Expenses!D14 'Amount' holds 12.5, not a whole number");
        assertThatThrownBy(() -> CellConversions.integer(numeric("1E+19"), AT))
                .hasMessage("Expenses!D14 'Amount' holds 10000000000000000000, beyond what a long holds");
    }

    @Test
    public void dateTakesADateCellOrIsoText() {
        assertThat(CellConversions.date(date(SEPT_30), AT)).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(CellConversions.date(text("2026-09-30"), AT)).isEqualTo(LocalDate.of(2026, 9, 30));
    }

    @Test
    public void dateRefusesWhatWouldLoseOrGuess() {
        assertThatThrownBy(() -> CellConversions.date(date(SEPT_30.withHour(14).withMinute(30)), AT))
                .hasMessage(
                        "Expenses!D14 'Amount' holds the date and time 2026-09-30T14:30 - read it with timestamp()");
        assertThatThrownBy(() -> CellConversions.date(timeOfDay(12, 30), AT))
                .hasMessage("Expenses!D14 'Amount' holds the time of day 12:30, not a date");
        assertThatThrownBy(() -> CellConversions.date(text("30/09/2026"), AT))
                .hasMessage("Expenses!D14 'Amount': text \"30/09/2026\" is not a date - expected 2026-09-30,"
                        + " or a cell formatted as a date");
        assertThatThrownBy(() -> CellConversions.date(numeric("46295"), AT))
                .hasMessage("Expenses!D14 'Amount' holds the number 46295, not a date - format the column as Date"
                        + " in Excel");
    }

    @Test
    public void timestampTakesADateCellOrIsoText() {
        LocalDateTime afternoon = SEPT_30.withHour(14).withMinute(30);
        assertThat(CellConversions.timestamp(date(afternoon), AT)).isEqualTo(afternoon);
        assertThat(CellConversions.timestamp(text("2026-09-30 14:30"), AT)).isEqualTo(afternoon);
        assertThat(CellConversions.timestamp(text("2026-09-30T14:30"), AT)).isEqualTo(afternoon);
        assertThat(CellConversions.timestamp(text("2026-09-30"), AT)).isEqualTo(SEPT_30);
        assertThatThrownBy(() -> CellConversions.timestamp(text("30.09.2026 14:30"), AT))
                .isInstanceOf(CellFormatException.class);
        assertThatThrownBy(() -> CellConversions.timestamp(timeOfDay(12, 30), AT))
                .hasMessage("Expenses!D14 'Amount' holds the time of day 12:30, not a date");
    }

    @Test
    public void boolTakesTrueAndFalseOnly() {
        assertThat(CellConversions.bool(new CellValue.Logical(true), AT)).isTrue();
        assertThat(CellConversions.bool(text(" True "), AT)).isTrue();
        assertThat(CellConversions.bool(text("FALSE"), AT)).isFalse();
        assertThatThrownBy(() -> CellConversions.bool(text("yes"), AT))
                .hasMessage("Expenses!D14 'Amount': text \"yes\" is not TRUE or FALSE");
        assertThatThrownBy(() -> CellConversions.bool(new CellValue.Numeric(BigDecimal.ONE), AT))
                .hasMessage("Expenses!D14 'Amount' holds the number 1, not TRUE or FALSE");
    }

    @Test
    public void textShowsEachKindTheWayExcelDoes() {
        assertThat(CellConversions.text(text("Familie Keller"), AT)).isEqualTo("Familie Keller");
        assertThat(CellConversions.text(numeric("1001.0"), AT)).isEqualTo("1001");
        assertThat(CellConversions.text(date(SEPT_30), AT)).isEqualTo("2026-09-30");
        assertThat(CellConversions.text(date(SEPT_30.withHour(14).withMinute(30)), AT))
                .isEqualTo("2026-09-30T14:30");
        assertThat(CellConversions.text(timeOfDay(12, 30), AT)).isEqualTo("12:30");
        assertThat(CellConversions.text(new CellValue.Logical(false), AT)).isEqualTo("FALSE");
    }

    @DataProvider
    Object[][] accessors() {
        return new Object[][] {
            {(BiFunction<CellValue, CellPosition, Object>) CellConversions::text},
            {(BiFunction<CellValue, CellPosition, Object>) CellConversions::decimal},
            {(BiFunction<CellValue, CellPosition, Object>) CellConversions::integer},
            {(BiFunction<CellValue, CellPosition, Object>) CellConversions::date},
            {(BiFunction<CellValue, CellPosition, Object>) CellConversions::timestamp},
            {(BiFunction<CellValue, CellPosition, Object>) CellConversions::bool}
        };
    }

    @Test(dataProvider = "accessors")
    public void everyAccessorRefusesEmptyErrorAndMissingResult(BiFunction<CellValue, CellPosition, Object> accessor) {
        assertThatThrownBy(() -> accessor.apply(CellValue.EMPTY, AT)).hasMessage("Expenses!D14 'Amount' is empty");
        assertThatThrownBy(() -> accessor.apply(text("   "), AT)).hasMessage("Expenses!D14 'Amount' is empty");
        assertThatThrownBy(() -> accessor.apply(new CellValue.ExcelError("#N/A"), AT))
                .hasMessage("Expenses!D14 'Amount' holds the error #N/A");
        assertThatThrownBy(() -> accessor.apply(new CellValue.NoResult("A1*2"), AT))
                .hasMessage("Expenses!D14 'Amount' holds the formula =A1*2 without its result - open the file in"
                        + " Excel and save it");
    }

    @Test
    public void emptyMeansNothingOrOnlySpaces() {
        assertThat(CellConversions.isEmpty(CellValue.EMPTY)).isTrue();
        assertThat(CellConversions.isEmpty(text(" "))).isTrue();
        assertThat(CellConversions.isEmpty(text("x"))).isFalse();
        assertThat(CellConversions.isEmpty(new CellValue.NoResult("A1"))).isFalse();
        assertThat(CellConversions.isEmpty(new CellValue.ExcelError("#N/A"))).isFalse();
    }

    private static CellValue numeric(String stored) {
        return new CellValue.Numeric(new BigDecimal(stored));
    }

    private static CellValue text(String text) {
        return new CellValue.Text(text);
    }

    private static CellValue date(LocalDateTime value) {
        return new CellValue.DateTime(value, false);
    }

    private static CellValue timeOfDay(int hour, int minute) {
        return new CellValue.DateTime(LocalDateTime.of(1899, 12, 31, hour, minute), true);
    }
}
