package com.druvu.excel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** The row type the sheet tests are written against. */
record Invoice(int number, String customer, BigDecimal amount, LocalDate due, LocalDateTime booked, Status status) {

    enum Status {
        OPEN,
        PAID
    }

    static final Invoice FIRST = new Invoice(
            20260017,
            "Familie Keller",
            new BigDecimal("1250.50"),
            LocalDate.of(2026, 10, 18),
            LocalDateTime.of(2026, 9, 18, 14, 30, 5),
            Status.OPEN);

    static final Invoice SECOND = new Invoice(
            20260018,
            "Atelier Morel",
            new BigDecimal("89.90"),
            LocalDate.of(2026, 9, 1),
            LocalDateTime.of(2026, 8, 2, 9, 0, 0),
            Status.PAID);

    static final List<Invoice> BOTH = List.of(FIRST, SECOND);
}
