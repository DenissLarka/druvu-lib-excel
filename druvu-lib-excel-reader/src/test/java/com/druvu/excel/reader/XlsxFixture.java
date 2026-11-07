package com.druvu.excel.reader;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A small xlsx written from raw XML: the parts a spreadsheet program writes and the druvu writer does not - formula
 * results, error cells, inline strings, the 1904 date system, any number format.
 */
final class XlsxFixture {

    private static final String MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String RELATIONSHIPS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";

    private final Map<String, String> sheets = new LinkedHashMap<>();
    private final List<String> sharedStrings = new ArrayList<>();
    private final List<String> customFormats = new ArrayList<>();
    private final List<Integer> cellFormats = new ArrayList<>(List.of(0));
    private boolean date1904;
    private String sheetPrologue = "";

    static XlsxFixture workbook() {
        return new XlsxFixture();
    }

    /** A sheet; {@code rows} is the content of its {@code <sheetData>}. */
    XlsxFixture sheet(String name, String rows) {
        sheets.put(name, rows);
        return this;
    }

    /** The shared strings, each the content of one {@code <si>}; a cell refers to one by its index. */
    XlsxFixture sharedStrings(String... items) {
        sharedStrings.addAll(List.of(items));
        return this;
    }

    /** A cell style with a built-in number format; the result is the {@code s} attribute of a cell. */
    int style(int builtInFormatId) {
        cellFormats.add(builtInFormatId);
        return cellFormats.size() - 1;
    }

    /** A cell style with a custom number format code; the result is the {@code s} attribute of a cell. */
    int style(String formatCode) {
        customFormats.add(formatCode);
        return style(163 + customFormats.size());
    }

    XlsxFixture date1904() {
        date1904 = true;
        return this;
    }

    /** XML placed before each sheet's root element, such as a DOCTYPE. */
    XlsxFixture sheetPrologue(String xml) {
        sheetPrologue = xml;
        return this;
    }

    Path save(Path file) {
        try (OutputStream out = Files.newOutputStream(file);
                ZipOutputStream zip = new ZipOutputStream(out)) {
            put(zip, "[Content_Types].xml", contentTypes());
            put(zip, "_rels/.rels", rootRelationships());
            put(zip, "xl/workbook.xml", workbookXml());
            put(zip, "xl/_rels/workbook.xml.rels", workbookRelationships());
            put(zip, "xl/styles.xml", stylesXml());
            put(zip, "xl/sharedStrings.xml", sharedStringsXml());
            int index = 1;
            for (String rows : sheets.values()) {
                put(zip, "xl/worksheets/sheet" + index++ + ".xml", sheetXml(rows));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return file;
    }

    private static void put(ZipOutputStream zip, String name, String xml) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(xml.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String contentTypes() {
        StringBuilder xml =
                new StringBuilder("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                        + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                        + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                        + override("/xl/workbook.xml", "sheet.main")
                        + override("/xl/styles.xml", "styles")
                        + override("/xl/sharedStrings.xml", "sharedStrings"));
        for (int index = 1; index <= sheets.size(); index++) {
            xml.append(override("/xl/worksheets/sheet" + index + ".xml", "worksheet"));
        }
        return xml.append("</Types>").toString();
    }

    private static String override(String part, String kind) {
        return "<Override PartName=\"" + part
                + "\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml." + kind + "+xml\"/>";
    }

    private static String rootRelationships() {
        return "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Target=\"xl/workbook.xml\" Type=\"" + RELATIONSHIPS
                + "/officeDocument\"/></Relationships>";
    }

    private String workbookXml() {
        StringBuilder xml = new StringBuilder("<workbook xmlns=\"" + MAIN + "\" xmlns:r=\"" + RELATIONSHIPS + "\">");
        if (date1904) {
            xml.append("<workbookPr date1904=\"1\"/>");
        }
        xml.append("<sheets>");
        int index = 1;
        for (String name : sheets.keySet()) {
            xml.append("<sheet name=\"")
                    .append(name)
                    .append("\" sheetId=\"")
                    .append(index)
                    .append("\" r:id=\"rId")
                    .append(index++)
                    .append("\"/>");
        }
        return xml.append("</sheets></workbook>").toString();
    }

    private String workbookRelationships() {
        StringBuilder xml = new StringBuilder(
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for (int index = 1; index <= sheets.size(); index++) {
            xml.append("<Relationship Id=\"rId")
                    .append(index)
                    .append("\" Target=\"worksheets/sheet")
                    .append(index)
                    .append(".xml\" Type=\"")
                    .append(RELATIONSHIPS)
                    .append("/worksheet\"/>");
        }
        return xml.append("</Relationships>").toString();
    }

    private String stylesXml() {
        StringBuilder xml = new StringBuilder("<styleSheet xmlns=\"" + MAIN + "\"><numFmts count=\"")
                .append(customFormats.size())
                .append("\">");
        for (int index = 0; index < customFormats.size(); index++) {
            xml.append("<numFmt numFmtId=\"")
                    .append(164 + index)
                    .append("\" formatCode=\"")
                    .append(customFormats.get(index).replace("\"", "&quot;"))
                    .append("\"/>");
        }
        xml.append("</numFmts><cellXfs count=\"").append(cellFormats.size()).append("\">");
        for (int formatId : cellFormats) {
            xml.append("<xf numFmtId=\"").append(formatId).append("\"/>");
        }
        return xml.append("</cellXfs></styleSheet>").toString();
    }

    private String sharedStringsXml() {
        StringBuilder xml = new StringBuilder("<sst xmlns=\"" + MAIN + "\">");
        for (String item : sharedStrings) {
            xml.append("<si>").append(item).append("</si>");
        }
        return xml.append("</sst>").toString();
    }

    private String sheetXml(String rows) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + sheetPrologue + "<worksheet xmlns=\"" + MAIN
                + "\"><sheetData>" + rows + "</sheetData></worksheet>";
    }
}
