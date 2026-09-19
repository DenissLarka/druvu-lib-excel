package com.druvu.excel;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.testng.annotations.Test;

/** How wide a column must be for what it holds. */
public class TestCellWidth {

    @Test
    public void textIsAsWideAsItIsLong() {
        assertThat(CellWidth.of("Familie Keller", "General")).isEqualTo(14);
    }

    @Test
    public void aFormattedNumberCountsSeparatorsAndDecimals() {
        assertThat(CellWidth.of(new BigDecimal("1250.5"), Style.MONEY.format())).isEqualTo("1,250.50".length());
        assertThat(CellWidth.of(new BigDecimal("-1234567.891"), Style.MONEY.format()))
                .isEqualTo("-1,234,567.89".length());
        assertThat(CellWidth.of(1234567, Style.INTEGER.format())).isEqualTo("1,234,567".length());
    }

    @Test
    public void aPercentageIsShownTimesHundred() {
        assertThat(CellWidth.of(new BigDecimal("0.081"), Style.PERCENT.format()))
                .isEqualTo("8.10%".length());
    }

    @Test
    public void anUnformattedNumberIsShownAsWritten() {
        assertThat(CellWidth.of(20260017, "General")).isEqualTo(8);
        assertThat(CellWidth.of(new BigDecimal("89.90"), "General")).isEqualTo(5);
    }

    @Test
    public void datesAreAsWideAsTheirFormat() {
        assertThat(CellWidth.of(LocalDate.of(2026, 9, 18), Style.DATE.format())).isEqualTo(10);
        assertThat(CellWidth.of(LocalDateTime.of(2026, 9, 18, 14, 30), Style.TIMESTAMP.format()))
                .isEqualTo(19);
    }

    @Test
    public void anEmptyCellTakesNoRoom() {
        assertThat(CellWidth.of(null, "General")).isZero();
    }
}
