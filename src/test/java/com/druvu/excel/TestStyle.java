package com.druvu.excel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Supplier;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.testng.annotations.Test;

/** Styles as values: refining, equality, refusals. */
public class TestStyle {

    @Test
    public void refiningAStyleLeavesTheOriginalUntouched() {
        Style alert = Style.MONEY.withFill(Fill.ORANGE);

        assertThat(alert.format()).isEqualTo("#,##0.00");
        assertThat(alert.fill()).isEqualTo(Fill.ORANGE);
        assertThat(Style.MONEY.fill()).isEqualTo(Fill.NONE);
    }

    @Test
    public void stylesWithEqualComponentsAreTheSameStyle() {
        Style one = Style.MONEY.withFill(Fill.ORANGE).withBold();
        Style other = Style.NONE.withBold().withFill(Fill.ORANGE).withFormat("#,##0.00");

        assertThat(one).isEqualTo(other).hasSameHashCodeAs(other);
    }

    @Test
    public void formatAndHighlightAreIndependentAxes() {
        Style style = Style.DATE.withFill(Fill.RED).withAlign(Align.CENTER);

        assertThat(style).isEqualTo(new Style("yyyy-mm-dd", Fill.RED, Align.CENTER, false));
    }

    @Test
    public void headerIsBoldCentredOnGrey() {
        assertThat(Style.HEADER).isEqualTo(new Style("General", Fill.GREY, Align.CENTER, true));
    }

    @Test
    public void nullComponentsAreRefused() {
        assertThatThrownBy(building(() -> Style.NONE.withFormat(absent()))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(building(() -> Style.NONE.withFill(absent()))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(building(() -> Style.NONE.withAlign(absent()))).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void blankFormatIsRefused() {
        assertThatThrownBy(building(() -> Style.NONE.withFormat(" ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("General");
    }

    private static ThrowingCallable building(Supplier<Style> style) {
        return style::get;
    }

    /** A null the caller did not mean to pass - what the refusal exists for. */
    private static <T> T absent() {
        return null;
    }
}
