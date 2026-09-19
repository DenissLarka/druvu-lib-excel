package com.druvu.excel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.testng.annotations.Test;

/** The predefined colours and colours of your own: what is accepted, what is refused, what is equal. */
public class TestFill {

    @Test
    public void theSameColourIsTheSameFillHoweverItWasWritten() {
        assertThat(Fill.of("C6EFCE")).isEqualTo(Fill.GREEN).hasSameHashCodeAs(Fill.GREEN);
        assertThat(Fill.of("c6efce")).isEqualTo(Fill.GREEN);
        assertThat(Fill.of("#c6efce")).isEqualTo(Fill.GREEN);
    }

    @Test
    public void aColourOfYourOwnIsKeptAsSixUpperCaseDigits() {
        assertThat(Fill.of("#1f3864").hex()).isEqualTo("1F3864");
    }

    @Test
    public void aStyleWithYourOwnColourDedupesLikeAnyOther() {
        Style one = Style.MONEY.withFill(Fill.of("1f3864"));
        Style other = Style.MONEY.withFill(Fill.of("#1F3864"));

        assertThat(one).isEqualTo(other).hasSameHashCodeAs(other);
    }

    @Test
    public void whatIsNotAColourIsRefused() {
        for (String notAColour : new String[] {"C6EFC", "C6EFCE0", "GGGGGG", "red", "##C6EFCE", "C6 EFCE"}) {
            assertThatThrownBy(() -> Fill.of(notAColour))
                    .as(notAColour)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("six hex digits");
        }
    }

    @Test
    public void blankPointsAtNone() {
        assertThatThrownBy(() -> Fill.of(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Fill.NONE");
    }

    @Test
    public void nullIsRefused() {
        assertThatThrownBy(() -> Fill.of(absent())).isInstanceOf(NullPointerException.class);
    }

    /** A null the caller did not mean to pass - what the refusal exists for. */
    private static <T> T absent() {
        return null;
    }
}
