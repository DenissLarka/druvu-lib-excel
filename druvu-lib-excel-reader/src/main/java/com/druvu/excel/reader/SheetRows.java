package com.druvu.excel.reader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.dhatim.fastexcel.reader.Sheet;

/** Reads one engine sheet into {@link Row}s: the first row with anything in it is the header row. */
final class SheetRows {

    private SheetRows() {}

    static List<Row> read(Sheet sheet) throws IOException {
        List<Row> rows = new ArrayList<>();
        HeaderIndex headers = null;
        for (org.dhatim.fastexcel.reader.Row engineRow : sheet.read()) {
            List<CellValue> cells = IntStream.range(0, engineRow.getCellCount())
                    .mapToObj(column -> engineRow
                            .getOptionalCell(column)
                            .map(CellValues::of)
                            .orElse(CellValue.EMPTY))
                    .toList();
            if (cells.stream().allMatch(CellConversions::isEmpty)) {
                continue;
            }
            if (headers == null) {
                headers = HeaderIndex.of(sheet.getName(), cells);
            } else {
                rows.add(new Row(sheet.getName(), engineRow.getRowNum(), headers, cells));
            }
        }
        return List.copyOf(rows);
    }
}
