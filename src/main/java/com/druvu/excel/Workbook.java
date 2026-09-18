package com.druvu.excel;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An xlsx file with several sheets, in the order they are added:
 *
 * <pre>{@code
 * Excel.workbook()
 *         .sheet(Excel.sheet("Customers", customers).column("Name", Customer::name))
 *         .sheet(Excel.sheet("Invoices", invoices).column("Number", Invoice::number))
 *         .save(Path.of("books.xlsx"));
 * }</pre>
 *
 * <p>Nothing is written until {@link #save} or {@link #writeTo}. Rows are then streamed: they leave memory a batch at a
 * time, so a sheet can be far larger than the heap.
 */
public final class Workbook {

    private static final String APPLICATION = "druvu-lib-excel";
    private static final String APPLICATION_VERSION = "1.0";

    private final List<Sheet<?>> sheets = new ArrayList<>();

    Workbook() {}

    public Workbook sheet(Sheet<?> sheet) {
        Objects.requireNonNull(sheet, "sheet");
        if (sheets.stream().anyMatch(other -> other.name().equalsIgnoreCase(sheet.name()))) {
            throw new IllegalArgumentException("workbook already has a sheet named '" + sheet.name() + "'");
        }
        sheets.add(sheet);
        return this;
    }

    /**
     * Writes the file - all of it or none of it. The rows go to a temporary file next to the target, which replaces the
     * target only once everything is written; a refused value leaves no half-filled report behind, and an existing file
     * is untouched until its replacement is complete.
     */
    public void save(Path file) {
        Objects.requireNonNull(file, "file");
        Path target = file.toAbsolutePath();
        Path directory = target.getParent();
        Path fileName = target.getFileName();
        if (directory == null || fileName == null) {
            throw new IllegalArgumentException("not a file: " + file);
        }
        try {
            Path unfinished = Files.createTempFile(directory, fileName.toString(), ".tmp");
            try {
                writeTo(Files.newOutputStream(unfinished));
                Files.move(unfinished, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(unfinished);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write " + file, e);
        }
    }

    /** Writes the workbook and closes the stream. */
    public void writeTo(OutputStream out) {
        Objects.requireNonNull(out, "out");
        try (out;
                var engine = new org.dhatim.fastexcel.Workbook(out, APPLICATION, APPLICATION_VERSION)) {
            if (sheets.isEmpty()) {
                throw new IllegalStateException("workbook has no sheets");
            }
            for (Sheet<?> sheet : sheets) {
                SheetWriter.write(sheet, engine.newWorksheet(sheet.name()));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write workbook", e);
        }
    }
}
