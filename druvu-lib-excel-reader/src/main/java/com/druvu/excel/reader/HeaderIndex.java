package com.druvu.excel.reader;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A sheet's header row: which column each header names. */
final class HeaderIndex {

    private final String sheet;
    private final Map<String, Integer> columns;
    private final Set<String> repeated;

    private HeaderIndex(String sheet, Map<String, Integer> columns, Set<String> repeated) {
        this.sheet = sheet;
        this.columns = columns;
        this.repeated = repeated;
    }

    /** The headers of a row, trimmed. A cell with no text in it names no column. */
    static HeaderIndex of(String sheet, List<CellValue> headerRow) {
        Map<String, Integer> columns = new LinkedHashMap<>();
        Set<String> repeated = new HashSet<>();
        for (int column = 0; column < headerRow.size(); column++) {
            String header = headerText(headerRow.get(column));
            if (!header.isEmpty() && columns.putIfAbsent(header, column) != null) {
                repeated.add(header);
            }
        }
        return new HeaderIndex(sheet, Map.copyOf(columns), Set.copyOf(repeated));
    }

    /** The column under this header, zero-based. */
    int column(String header) {
        if (repeated.contains(header)) {
            throw new IllegalArgumentException(
                    "sheet '" + sheet + "' has the header '" + header + "' twice - rename one of them in Excel");
        }
        Integer column = columns.get(header);
        if (column == null) {
            throw new IllegalArgumentException("sheet '" + sheet + "' has no header '" + header + "' - its headers: "
                    + String.join(", ", inOrder()));
        }
        return column;
    }

    private List<String> inOrder() {
        return columns.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .toList();
    }

    private static String headerText(CellValue cell) {
        return switch (cell) {
            case CellValue.Text(String text) -> text.strip();
            case CellValue.Numeric(var number) -> CellConversions.plain(number);
            default -> "";
        };
    }
}
