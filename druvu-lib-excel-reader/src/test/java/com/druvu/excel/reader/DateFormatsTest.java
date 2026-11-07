package com.druvu.excel.reader;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/** Which cell formats show a date or a time, and which a time of day alone. */
public class DateFormatsTest {

    @DataProvider
    Object[][] formats() {
        return new Object[][] {
            {14, "mm-dd-yy", true},
            {22, "m/d/yy h:mm", true},
            {30, null, true}, // an East Asian built-in date: no code in the file
            {164, "dd.mm.yyyy", true},
            {164, "yyyy-mm-dd hh:mm:ss", true},
            {164, "[$-F800]dddd\\,\\ mmmm\\ dd\\,\\ yyyy", true},
            {164, "[h]:mm:ss", true},
            {0, "General", false},
            {10, "0.00%", false},
            {11, "0.00E+00", false},
            {49, "@", false},
            {164, "#,##0.00\\ [$€-407]", false},
            {164, "[Red]#,##0", false},
            {164, "_(* #,##0.00_)", false},
            {164, "\"h\"0", false}
        };
    }

    @Test(dataProvider = "formats")
    public void tellsDatesFromNumbers(Integer formatId, String formatCode, boolean dateOrTime) {
        assertThat(DateFormats.isDateOrTime(formatId, formatCode)).isEqualTo(dateOrTime);
    }

    @DataProvider
    Object[][] times() {
        return new Object[][] {
            {"h:mm", true},
            {"h:mm AM/PM", true},
            {"[h]:mm:ss", true},
            {"mm:ss", true},
            {"d-mmm", false},
            {"mmm-yy", false},
            {"yyyy-mm-dd hh:mm", false},
            {null, false}
        };
    }

    @Test(dataProvider = "times")
    public void tellsTimesOfDayFromDates(String formatCode, boolean timeOfDayOnly) {
        assertThat(DateFormats.isTimeOfDayOnly(formatCode)).isEqualTo(timeOfDayOnly);
    }
}
