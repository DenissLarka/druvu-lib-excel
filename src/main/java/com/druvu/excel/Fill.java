package com.druvu.excel;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A cell background: one of the predefined colours, or your own via {@link #of(String)}.
 *
 * <p>The predefined palette is Excel's own soft set - the colours behind its "Good", "Bad" and "Neutral" cell styles -
 * plus the druvu lavender. Black text stays readable on every one of them. That promise covers the predefined colours
 * only: there is no font colour to go with a fill, so how text reads on a colour of your own is yours to judge.
 *
 * <p>Two fills with the same colour are the same fill, however they were written: {@code Fill.of("#c6efce")} equals
 * {@link #GREEN}.
 *
 * @param hex red, green, blue as six upper-case hex digits; empty for {@link #NONE}
 * @author Deniss Larka <br>
 *     on 18 Sep 2026
 */
public record Fill(String hex) {

    private static final Pattern SIX_HEX_DIGITS = Pattern.compile("[0-9A-F]{6}");

    /** No background: the cell keeps Excel's own. */
    public static final Fill NONE = new Fill("");

    public static final Fill GREEN = new Fill("C6EFCE");
    public static final Fill RED = new Fill("FFC7CE");
    public static final Fill YELLOW = new Fill("FFEB9C");
    public static final Fill ORANGE = new Fill("FCE4D6");
    public static final Fill BLUE = new Fill("DDEBF7");
    public static final Fill LAVENDER = new Fill("D7E0FF");
    public static final Fill GREY = new Fill("D9D9D9");

    public Fill {
        Objects.requireNonNull(hex, "hex");
        hex = (hex.startsWith("#") ? hex.substring(1) : hex).toUpperCase(Locale.ROOT);
        if (!hex.isEmpty() && !SIX_HEX_DIGITS.matcher(hex).matches()) {
            throw new IllegalArgumentException("not a colour: '" + hex + "' - expected six hex digits, e.g. C6EFCE");
        }
    }

    /**
     * A colour of your own.
     *
     * @param hex red, green, blue as six hex digits, e.g. {@code "C6EFCE"}; either case, a leading {@code #} is fine
     */
    public static Fill of(String hex) {
        Objects.requireNonNull(hex, "hex");
        if (hex.isBlank()) {
            throw new IllegalArgumentException("colour is blank - use Fill.NONE for no background");
        }
        return new Fill(hex);
    }
}
