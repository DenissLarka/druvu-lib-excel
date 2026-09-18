package com.druvu.excel;

import java.util.Locale;
import org.dhatim.fastexcel.StyleSetter;

/**
 * Hands a {@link Style} to the xlsx engine, one cell at a time.
 *
 * <p>An xlsx holds about 64,000 cell styles at most, and a style chosen per row by a function would reach that cap if
 * every row stored its own. The engine stores styles by value, so a file holds only as many as there are different
 * looks - which is why {@link Style} is a value. {@code TestCellStyles} holds the engine to that.
 */
final class CellStyles {

    private CellStyles() {}

    static void apply(Style style, StyleSetter cell) {
        if (style.equals(Style.NONE)) {
            return;
        }
        if (!Style.GENERAL.equals(style.format())) {
            cell.format(style.format());
        }
        if (style.fill() != Fill.NONE) {
            cell.fillColor(style.fill().hex());
        }
        if (style.align() != Align.AUTO) {
            cell.horizontalAlignment(style.align().name().toLowerCase(Locale.ROOT));
        }
        if (style.bold()) {
            cell.bold();
        }
        cell.set();
    }
}
