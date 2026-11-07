package com.druvu.excel.reader;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Whether a cell format shows a date or a time. Excel stores a date as a number and says "this is a date" only through
 * the cell's format: a built-in format id, or a format code such as {@code dd.mm.yyyy}.
 */
final class DateFormats {

    /**
     * Parts of a format code that print literally or set a colour, a condition or a locale: quoted text, an escaped
     * character, a {@code [...]} section other than an elapsed time such as {@code [h]}, and the padding marks
     * {@code _x} and {@code *x}.
     */
    private static final Pattern LITERALS =
            Pattern.compile("\"[^\"]*\"|\\\\.|\\[(?![hms]+])[^]]*]|[_*].", Pattern.CASE_INSENSITIVE);

    private static final Pattern DATE_OR_TIME_TOKEN = Pattern.compile("[ymdhs]");
    private static final Pattern DATE_TOKEN = Pattern.compile("[yd]");

    private DateFormats() {}

    /** A date, a date with a time, or a time of day. */
    static boolean isDateOrTime(Integer formatId, String formatCode) {
        if (formatId != null && isBuiltInDateOrTime(formatId)) {
            return true;
        }
        return formatCode != null
                && DATE_OR_TIME_TOKEN.matcher(tokens(formatCode)).find();
    }

    /**
     * A time of day with no date part, such as {@code h:mm} or {@code [h]:mm:ss}. Without a format code (the East Asian
     * built-in dates come without one) it is a date.
     */
    static boolean isTimeOfDayOnly(String formatCode) {
        if (formatCode == null) {
            return false;
        }
        String tokens = tokens(formatCode);
        return !DATE_TOKEN.matcher(tokens).find() && (tokens.contains("h") || tokens.contains("s"));
    }

    /**
     * The built-in date and time formats: 14-22 and 45-47 everywhere, 27-36 and 50-58 the East Asian dates. The ids
     * between them (37-44, 48, 49) are number and text formats.
     */
    private static boolean isBuiltInDateOrTime(int formatId) {
        return formatId >= 14 && formatId <= 22
                || formatId >= 27 && formatId <= 36
                || formatId >= 45 && formatId <= 47
                || formatId >= 50 && formatId <= 58;
    }

    /** The format code without its literal parts, in lower case, with the words that hold no date letter removed. */
    private static String tokens(String formatCode) {
        return LITERALS.matcher(formatCode)
                .replaceAll("")
                .toLowerCase(Locale.ROOT)
                .replace("general", "")
                .replace("am/pm", "")
                .replace("a/p", "");
    }
}
