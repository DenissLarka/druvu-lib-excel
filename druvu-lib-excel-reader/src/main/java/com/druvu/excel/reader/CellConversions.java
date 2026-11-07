package com.druvu.excel.reader;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The reading rules, one method per Java type: a value is converted only where no guess is involved, everything else is
 * refused with a {@link CellFormatException} that names the cell.
 */
final class CellConversions {

    /** Excel keeps 15 significant digits and shows no more; the file may store a 17-digit binary leftover. */
    private static final MathContext EXCEL_DIGITS = new MathContext(15, RoundingMode.HALF_EVEN);

    /** A number typed as text that needs no guess: digits, an optional minus, an optional dot with digits. */
    private static final Pattern PLAIN_NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");

    private CellConversions() {}

    static boolean isEmpty(CellValue cell) {
        return cell instanceof CellValue.Empty || cell instanceof CellValue.Text(String text) && text.isBlank();
    }

    static String text(CellValue cell, CellPosition at) {
        return switch (cell) {
            case CellValue.Text(String text) when !text.isBlank() -> text;
            case CellValue.Numeric(BigDecimal number) -> plain(number);
            case CellValue.DateTime dateTime -> iso(dateTime);
            case CellValue.Logical(boolean logical) -> logical ? "TRUE" : "FALSE";
            default -> throw refused(cell, at);
        };
    }

    static BigDecimal decimal(CellValue cell, CellPosition at) {
        return switch (cell) {
            case CellValue.Numeric(BigDecimal number) -> asShown(number);
            case CellValue.Text(String text) when !text.isBlank() -> parseNumber(text, at);
            case CellValue.DateTime dateTime ->
                throw new CellFormatException(at + " holds the date " + iso(dateTime) + ", not a number");
            case CellValue.Logical(boolean logical) ->
                throw new CellFormatException(at + " holds " + (logical ? "TRUE" : "FALSE") + ", not a number");
            default -> throw refused(cell, at);
        };
    }

    static long integer(CellValue cell, CellPosition at) {
        BigDecimal number = decimal(cell, at);
        if (number.stripTrailingZeros().scale() > 0) {
            throw new CellFormatException(at + " holds " + plain(number) + ", not a whole number");
        }
        try {
            return number.longValueExact();
        } catch (ArithmeticException tooLarge) {
            throw new CellFormatException(at + " holds " + plain(number) + ", beyond what a long holds");
        }
    }

    static LocalDate date(CellValue cell, CellPosition at) {
        return switch (cell) {
            case CellValue.DateTime dateTime when dateTime.timeOfDayOnly() -> throw timeOfDayOnly(dateTime, at);
            case CellValue.DateTime dateTime
            when hasTimeOfDay(dateTime) ->
                throw new CellFormatException(
                        at + " holds the date and time " + iso(dateTime) + " - read it with timestamp()");
            case CellValue.DateTime dateTime -> dateTime.value().toLocalDate();
            case CellValue.Text(String text) when !text.isBlank() -> parseDate(text, at);
            case CellValue.Numeric(BigDecimal number) -> throw notADate(number, at);
            default -> throw refused(cell, at);
        };
    }

    static LocalDateTime timestamp(CellValue cell, CellPosition at) {
        return switch (cell) {
            case CellValue.DateTime dateTime when dateTime.timeOfDayOnly() -> throw timeOfDayOnly(dateTime, at);
            case CellValue.DateTime dateTime -> dateTime.value();
            case CellValue.Text(String text) when !text.isBlank() -> parseTimestamp(text, at);
            case CellValue.Numeric(BigDecimal number) -> throw notADate(number, at);
            default -> throw refused(cell, at);
        };
    }

    static boolean bool(CellValue cell, CellPosition at) {
        return switch (cell) {
            case CellValue.Logical logical -> logical.value();
            case CellValue.Text(String text) when !text.isBlank() -> parseBool(text, at);
            case CellValue.Numeric(BigDecimal number) ->
                throw new CellFormatException(at + " holds the number " + plain(number) + ", not TRUE or FALSE");
            default -> throw refused(cell, at);
        };
    }

    /** A number in its plain form, as Excel shows it: {@code 1001}, never {@code 1001.0} or {@code 1.001E+3}. */
    static String plain(BigDecimal number) {
        return asShown(number).toPlainString();
    }

    /** At most 15 significant digits, without trailing zeros, and never in exponent form (100, not 1E+2). */
    private static BigDecimal asShown(BigDecimal number) {
        BigDecimal shown = number.round(EXCEL_DIGITS).stripTrailingZeros();
        return shown.scale() < 0 ? shown.setScale(0) : shown;
    }

    private static BigDecimal parseNumber(String text, CellPosition at) {
        String trimmed = text.strip();
        if (!PLAIN_NUMBER.matcher(trimmed).matches()) {
            throw new CellFormatException(at + ": text \"" + text
                    + "\" is not a number - expected digits with a dot, like -1234.50, or a number cell");
        }
        return new BigDecimal(trimmed);
    }

    private static boolean parseBool(String text, CellPosition at) {
        return switch (text.strip().toLowerCase(Locale.ROOT)) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new CellFormatException(at + ": text \"" + text + "\" is not TRUE or FALSE");
        };
    }

    private static LocalDate parseDate(String text, CellPosition at) {
        try {
            return LocalDate.parse(text.strip());
        } catch (DateTimeParseException notIso) {
            throw new CellFormatException(
                    at + ": text \"" + text + "\" is not a date - expected 2026-09-30, or a cell formatted as a date");
        }
    }

    private static LocalDateTime parseTimestamp(String text, CellPosition at) {
        String trimmed = text.strip();
        try {
            return trimmed.length() == 10
                    ? LocalDate.parse(trimmed).atStartOfDay()
                    : LocalDateTime.parse(trimmed.replace(' ', 'T'));
        } catch (DateTimeParseException notIso) {
            throw new CellFormatException(at + ": text \"" + text
                    + "\" is not a date and time - expected 2026-09-30 14:30, or a cell formatted as a date");
        }
    }

    private static String iso(CellValue.DateTime dateTime) {
        LocalDateTime value = dateTime.value();
        if (dateTime.timeOfDayOnly()) {
            return value.toLocalTime().toString();
        }
        return hasTimeOfDay(dateTime) ? value.toString() : value.toLocalDate().toString();
    }

    private static boolean hasTimeOfDay(CellValue.DateTime dateTime) {
        return !LocalTime.MIDNIGHT.equals(dateTime.value().toLocalTime());
    }

    private static CellFormatException timeOfDayOnly(CellValue.DateTime dateTime, CellPosition at) {
        return new CellFormatException(at + " holds the time of day " + iso(dateTime) + ", not a date");
    }

    private static CellFormatException notADate(BigDecimal number, CellPosition at) {
        return new CellFormatException(
                at + " holds the number " + plain(number) + ", not a date - format the column as Date in Excel");
    }

    /** The refusals every accessor shares: nothing in the cell, an error, a formula without its saved result. */
    private static CellFormatException refused(CellValue cell, CellPosition at) {
        return switch (cell) {
            case CellValue.ExcelError(String code) -> new CellFormatException(at + " holds the error " + code);
            case CellValue.NoResult(String formula) ->
                new CellFormatException(at + " holds the formula =" + formula
                        + " without its result - open the file in Excel and save it");
            default -> new CellFormatException(at + " is empty");
        };
    }
}
