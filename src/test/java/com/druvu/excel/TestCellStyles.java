package com.druvu.excel;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.IntFunction;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.testng.annotations.Test;

/** Every file is written by the library's engine and read back by POI - never verified by the code that wrote it. */
public class TestCellStyles {

    @Test
    public void everyComponentReachesTheFile() throws IOException {
        Style style = Style.MONEY.withFill(Fill.GREEN).withAlign(Align.CENTER).withBold();

        try (var reread = reread(write(1, row -> style))) {
            XSSFCellStyle cellStyle = styleOfRow(reread, 0);
            assertThat(cellStyle.getDataFormatString()).isEqualTo("#,##0.00");
            assertThat(cellStyle.getAlignment()).isEqualTo(HorizontalAlignment.CENTER);
            assertThat(cellStyle.getFillPattern()).isEqualTo(FillPatternType.SOLID_FOREGROUND);
            assertThat(cellStyle.getFillForegroundColorColor().getARGBHex()).endsWith("C6EFCE");
            assertThat(cellStyle.getFont().getBold()).isTrue();
        }
    }

    @Test
    public void everyFillOfThePaletteReachesTheFileAsItsOwnColour() throws IOException {
        Fill[] palette = {Fill.NONE, Fill.GREEN, Fill.RED, Fill.YELLOW, Fill.ORANGE, Fill.BLUE, Fill.LAVENDER, Fill.GREY
        };

        try (var reread = reread(write(palette.length, row -> Style.NONE.withFill(palette[row])))) {
            for (int row = 0; row < palette.length; row++) {
                XSSFCellStyle cellStyle = styleOfRow(reread, row);
                if (palette[row].equals(Fill.NONE)) {
                    assertThat(cellStyle.getFillPattern()).isEqualTo(FillPatternType.NO_FILL);
                } else {
                    assertThat(cellStyle.getFillForegroundColorColor().getARGBHex())
                            .endsWith(palette[row].hex());
                }
            }
        }
    }

    @Test
    public void aColourOfYourOwnReachesTheFileDarkOnesIncluded() throws IOException {
        try (var reread = reread(write(1, row -> Style.NONE.withFill(Fill.of("#1f3864"))))) {
            XSSFCellStyle cellStyle = styleOfRow(reread, 0);
            assertThat(cellStyle.getFillPattern()).isEqualTo(FillPatternType.SOLID_FOREGROUND);
            assertThat(cellStyle.getFillForegroundColorColor().getARGBHex()).endsWith("1F3864");
        }
    }

    @Test
    public void noStyleLeavesExcelsDefaults() throws IOException {
        try (var reread = reread(write(1, row -> Style.NONE))) {
            XSSFCellStyle cellStyle = styleOfRow(reread, 0);
            assertThat(cellStyle.getDataFormatString()).isEqualTo("General");
            assertThat(cellStyle.getFillPattern()).isEqualTo(FillPatternType.NO_FILL);
            assertThat(cellStyle.getAlignment()).isEqualTo(HorizontalAlignment.GENERAL);
            assertThat(cellStyle.getFont().getBold()).isFalse();
        }
    }

    @Test
    public void aFormatAloneTouchesNothingElse() throws IOException {
        try (var reread = reread(write(1, row -> Style.DATE))) {
            XSSFCellStyle cellStyle = styleOfRow(reread, 0);
            assertThat(cellStyle.getDataFormatString()).isEqualTo("yyyy-mm-dd");
            assertThat(cellStyle.getFillPattern()).isEqualTo(FillPatternType.NO_FILL);
            assertThat(cellStyle.getAlignment()).isEqualTo(HorizontalAlignment.GENERAL);
        }
    }

    @Test
    public void aStyleChosenPerRowNeverGrowsTheFile() throws IOException {
        int styles;
        try (var two = reread(write(2, TestCellStyles::alternating))) {
            styles = two.getNumCellStyles();
        }

        try (var many = reread(write(20_000, TestCellStyles::alternating))) {
            assertThat(many.getNumCellStyles()).isEqualTo(styles);
            assertThat(styleOfRow(many, 19_999).getFillPattern()).isEqualTo(FillPatternType.SOLID_FOREGROUND);
        }
    }

    @Test
    public void everyBoldStyleSharesOneFont() throws IOException {
        int fonts;
        try (var one = reread(write(1, row -> Style.HEADER))) {
            fonts = one.getNumberOfFonts();
        }

        Style[] bold = {Style.HEADER, Style.MONEY.withBold(), Style.DATE.withBold()};
        try (var three = reread(write(3, row -> bold[row]))) {
            assertThat(three.getNumberOfFonts()).isEqualTo(fonts);
        }
    }

    private static Style alternating(int row) {
        return row % 2 == 0 ? Style.MONEY : Style.MONEY.withFill(Fill.RED);
    }

    /** One number per row in column A, styled by the given function. */
    private static byte[] write(int rows, IntFunction<Style> styleOfRow) throws IOException {
        var out = new ByteArrayOutputStream();
        try (var workbook = new Workbook(out, "druvu-lib-excel", "1.0")) {
            Worksheet sheet = workbook.newWorksheet("Styles");
            for (int row = 0; row < rows; row++) {
                sheet.value(row, 0, 1234.5);
                CellStyles.apply(styleOfRow.apply(row), sheet.style(row, 0));
            }
        }
        return out.toByteArray();
    }

    private static XSSFWorkbook reread(byte[] bytes) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(bytes));
    }

    private static XSSFCellStyle styleOfRow(XSSFWorkbook workbook, int row) {
        return workbook.getSheet("Styles").getRow(row).getCell(0).getCellStyle();
    }
}
