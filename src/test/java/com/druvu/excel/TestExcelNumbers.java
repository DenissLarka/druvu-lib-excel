package com.druvu.excel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.testng.annotations.Test;

/** Excel keeps fifteen significant digits: a number that fits passes, one it would round silently is refused. */
public class TestExcelNumbers {

    @Test
    public void fifteenSignificantDigitsPass() {
        var number = new BigDecimal("1234567890123.45");

        assertThat(ExcelNumbers.exact(number, "Amount")).isSameAs(number);
    }

    @Test
    public void aSixteenthDigitIsRefusedByColumnName() {
        assertThatThrownBy(() -> ExcelNumbers.exact(new BigDecimal("12345678901234.567"), "Amount"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'Amount'")
                .hasMessageContaining("12345678901234.567")
                .hasMessageContaining("15 significant digits");
    }

    @Test
    public void trailingZerosAreNotDigitsLost() {
        var number = new BigDecimal("1250.500000000000000000");

        assertThat(ExcelNumbers.exact(number, "Amount")).isSameAs(number);
    }

    @Test
    public void aSmallFractionCountsItsDigitsNotItsZeros() {
        var number = new BigDecimal("0.000000000000000000123");

        assertThat(ExcelNumbers.exact(number, "Rate")).isSameAs(number);
    }

    @Test
    public void wholeNumbersFollowTheSameRule() {
        assertThat(ExcelNumbers.exact(999_999_999_999_999L, "Id")).isEqualTo(999_999_999_999_999L);
        assertThat(ExcelNumbers.exact(-999_999_999_999_999L, "Id")).isEqualTo(-999_999_999_999_999L);
        assertThatThrownBy(() -> ExcelNumbers.exact(1_000_000_000_000_000L, "Id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'Id'")
                .hasMessageContaining("as text");
    }

    @Test
    public void numbersExcelHasNoCellForAreRefused() {
        assertThat(ExcelNumbers.finite(0.25d, "Ratio")).isEqualTo(0.25d);
        assertThatThrownBy(() -> ExcelNumbers.finite(Double.NaN, "Ratio"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'Ratio'");
        assertThatThrownBy(() -> ExcelNumbers.finite(Double.NEGATIVE_INFINITY, "Ratio"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
