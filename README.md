# druvu-lib-excel

[![CI](https://github.com/DenissLarka/druvu-lib-excel/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/DenissLarka/druvu-lib-excel/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.druvu/druvu-lib-excel.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.druvu/druvu-lib-excel)
![Java](https://img.shields.io/badge/Java-25-blue)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE)

A Java library that writes Excel files (`.xlsx`). You describe a sheet in one place - its rows, and for
each column a header and the value to show - and get a file that opens in Excel looking finished: bold
header that stays in view, filters, fitted column widths, dates that are dates, money that is money.

It writes; it does not read Excel files.

Part of [druvu.com](https://druvu.com).

## Quick start: one file, no project

Save this as `Report.java` and run `jbang Report.java`. [JBang](https://www.jbang.dev) fetches Java 25
and the library from Maven Central by itself - there is nothing else to install or configure.

```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS com.druvu:druvu-lib-excel:1.0.0

import com.druvu.excel.Excel;
import com.druvu.excel.Fill;
import com.druvu.excel.Style;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

public class Report {

    record Invoice(String number, String customer, LocalDate due, BigDecimal amount, boolean paid) {}

    public static void main(String[] args) throws Exception {
        List<Invoice> invoices = List.of(
                new Invoice("2026-041", "Familie Keller", LocalDate.of(2026, 9, 30), new BigDecimal("1840.50"), true),
                new Invoice("2026-042", "Atelier Nord", LocalDate.of(2026, 10, 15), new BigDecimal("620.00"), false),
                new Invoice("2026-043", "Praxis Weber", LocalDate.of(2026, 10, 31), new BigDecimal("2315.75"), false));

        Excel.sheet("Invoices", invoices)
                .column("Number", Invoice::number)
                .column("Customer", Invoice::customer)
                .column("Due", Invoice::due)
                .column("Amount", invoice -> invoice.paid() ? Style.MONEY : Style.MONEY.withFill(Fill.ORANGE), Invoice::amount)
                .column("Paid", Invoice::paid)
                .save(Path.of("invoices.xlsx"));

        System.out.println("invoices.xlsx written - open it in Excel.");
    }
}
```

Everything public lives in one package, `com.druvu.excel`: `Excel`, `Sheet`, `Workbook`, `Style`, `Fill`, `Align`.

## What every sheet gets without asking

- the header row: bold, centred, on grey, in the order the columns were declared
- the header stays in view while scrolling
- a filter on every column, over all rows
- each column as wide as what it holds
- dates shown as dates, whole numbers without a thousands separator, an empty cell where a value is missing

## Columns

A column is a header plus a function from one row to the value to show. There are three forms:

```java
.column("Customer", Invoice::customer)                        // Excel's default look
.column("Amount", Style.MONEY, Invoice::amount)               // one style for the whole column
.column("Amount", invoice -> invoice.paid()                   // a style chosen row by row
        ? Style.MONEY
        : Style.MONEY.withFill(Fill.ORANGE), Invoice::amount)
```

Rows come from any `Iterable` (a `List`, a `Set`) or from a `Stream`.

### What a value may be

| The function answers | The cell holds |
|---|---|
| `String` (any `CharSequence`), an `enum` | text |
| `int`, `long`, `short`, `byte`, `BigDecimal`, `double`, `float` | a number |
| `LocalDate` | a date |
| `LocalDateTime` | a date with the time of day |
| `boolean` | TRUE / FALSE |
| `null`, or an empty `Optional` | nothing - an empty cell |
| an `Optional` holding one of the above | what it holds |

Anything else - an `Instant`, a `List`, an object of your own - is refused with an
`IllegalArgumentException` that names the column. Convert it in the value function:
`.column("Booked", tx -> tx.bookedAt().atZone(zone).toLocalDateTime())`.

## Styles

A `Style` is an immutable value. Start from a predefined one and refine it; the original is never changed.

| Constant | Looks like |
|---|---|
| `Style.NONE` | Excel's default |
| `Style.INTEGER` | `12,345` |
| `Style.MONEY` | `12,345.60` |
| `Style.PERCENT` | `8.10%` for the value `0.081` |
| `Style.DATE` | `2026-09-30` |
| `Style.TIMESTAMP` | `2026-09-30 14:05:00` |
| `Style.HEADER` | bold, centred, on grey - what the header row uses |

```java
Style.MONEY.withBold()                      // bold
Style.MONEY.withFill(Fill.ORANGE)           // a background colour
Style.NONE.withAlign(Align.CENTER)          // Align.AUTO, LEFT, CENTER, RIGHT
Style.NONE.withFormat("0.0000")             // any Excel number format
Style.MONEY.withFill(Fill.GREEN).withBold() // they chain
```

A style that names no number format keeps the column's natural one, so `Style.NONE.withFill(Fill.RED)`
on a date column is still a date.

### Background colours

Predefined: `Fill.NONE`, `Fill.GREEN`, `Fill.RED`, `Fill.YELLOW`, `Fill.ORANGE`, `Fill.BLUE`,
`Fill.LAVENDER`, `Fill.GREY` - Excel's own soft colours, on which black text stays readable.

A colour of your own: `Fill.of("1F3864")` - six hex digits, either case, a leading `#` is fine. There is
no font colour, so how text reads on your own colour is yours to judge.

## Several sheets in one file

```java
Excel.workbook()
        .sheet(Excel.sheet("Invoices", invoices).column("Number", Invoice::number))
        .sheet(Excel.sheet("Customers", customers).column("Name", Customer::name))
        .save(Path.of("books.xlsx"));
```

Sheets appear in the order added. Instead of `save(Path)`, `writeTo(OutputStream)` writes to any stream.

## A totals row

There is no separate footer: a totals row is one more row at the end, styled bold. Give the row type a
flag that says "I am the totals row", and let the columns that do not apply to it answer `null` - an
empty cell. A `record` keeps this short, and its accessors (`Line::item`) are what the columns use:

```java
record Line(String item, Integer quantity, BigDecimal amount, boolean total) {}

List<Line> lines = new ArrayList<>();
lines.add(new Line("Paper", 12, new BigDecimal("54.00"), false));
lines.add(new Line("Toner", 2, new BigDecimal("178.40"), false));

BigDecimal sum = lines.stream().map(Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
lines.add(new Line("Total", null, sum, true));            // no quantity on the totals row

Excel.sheet("Stock", lines)
        .column("Item", line -> line.total() ? Style.NONE.withBold() : Style.NONE, Line::item)
        .column("Quantity", Line::quantity)
        .column("Amount", line -> line.total() ? Style.MONEY.withBold() : Style.MONEY, Line::amount)
        .save(Path.of("stock.xlsx"));
```

The totals row is an ordinary row to Excel: sorting or filtering a column moves it with the others.

## What is refused, and why

The library refuses rather than writes a file that is quietly wrong. Each refusal is an exception whose
message names the column or the sheet.

- **A number Excel would change.** Excel keeps 15 significant digits. A `BigDecimal` or `long` with more
  would be rounded silently by Excel, so it is refused: `1234567890123.45` (15 digits) is written,
  `12345678901234.56` (16) is not. Round it in the value function - or, for an identifier such as an
  account number, answer it as a `String`.
- **A value with no Excel cell type** - see the table above. Also `NaN` and infinite doubles.
- **A sheet without columns, a workbook without sheets, two sheets with one name,** or a sheet name
  Excel itself would reject (longer than 31 characters, or containing `/ \ ? * [ ] :`).

Nothing is written until `save` or `writeTo`. If a write is refused, no file is left behind, and a file
that was already there is left untouched.

## Large sheets

Rows are streamed: they leave memory a batch at a time, so a sheet can be far larger than the heap.
Pass a `Stream` to keep the source lazy as well.

## With GnuCash books

Together with [druvu-acc](https://github.com/DenissLarka/druvu-acc-parent), a few lines turn a GnuCash
book into a spreadsheet - add both lines to a JBang script:

```java
//DEPS com.druvu:druvu-acc-gnucash-xml:2.2.1
//DEPS com.druvu:druvu-lib-excel:1.0.0
```

## Installation

On **Maven Central**. No extra repository, no credentials.

```xml
<dependency>
    <groupId>com.druvu</groupId>
    <artifactId>druvu-lib-excel</artifactId>
    <version>1.0.0</version>
</dependency>
```

```
//DEPS com.druvu:druvu-lib-excel:1.0.0                  (JBang script)
implementation("com.druvu:druvu-lib-excel:1.0.0")       (Gradle)
```

With the Java module system: `requires com.druvu.excel;`

## Requirements

- Java 25+ (a JBang script fetches it by itself)
- Maven 3.9+ - only to build the library from source

## Build

```
mvn verify
```

## License

[Apache License 2.0](LICENSE)
