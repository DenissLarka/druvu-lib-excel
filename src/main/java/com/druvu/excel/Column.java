package com.druvu.excel;

import java.util.Objects;
import java.util.function.Function;

/** One column of a sheet: its header, and for a row the style and the value of its cell. */
record Column<T>(String header, Function<? super T, Style> style, Function<? super T, ?> value) {

    Column {
        Objects.requireNonNull(header, "header");
        Objects.requireNonNull(style, "style");
        Objects.requireNonNull(value, "value");
    }
}
