package com.druvu.excel.reader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Where the tests write their files: the module's {@code target/test-files}, gone at the next {@code mvn clean}. */
final class OutputFiles {

    private static final Path FOLDER = Path.of("target", "test-files");

    private OutputFiles() {}

    static Path path(String name) {
        try {
            Files.createDirectories(FOLDER);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return FOLDER.resolve(name);
    }
}
