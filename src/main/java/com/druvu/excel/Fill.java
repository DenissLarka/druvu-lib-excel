package com.druvu.excel;

/**
 * A cell background. The palette is Excel's own soft set - the colours behind its "Good", "Bad" and "Neutral" cell
 * styles - plus the druvu lavender. It is closed on purpose: black text stays readable on every one of them, a promise
 * an arbitrary colour could not keep without a font colour to go with it.
 */
public enum Fill {
    NONE(""),
    GREEN("C6EFCE"),
    RED("FFC7CE"),
    YELLOW("FFEB9C"),
    ORANGE("FCE4D6"),
    BLUE("DDEBF7"),
    LAVENDER("D7E0FF"),
    GREY("D9D9D9");

    private final String hex;

    Fill(String hex) {
        this.hex = hex;
    }

    /** Red, green, blue as six hex digits. Empty for {@link #NONE}. */
    String hex() {
        return hex;
    }
}
