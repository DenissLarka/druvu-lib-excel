package com.druvu.excel.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.dhatim.fastexcel.reader.ExcelReaderException;
import org.testng.annotations.Test;

/** The files that are not an xlsx are refused by name, and a file's XML cannot reach outside it. */
public class WorkbooksTest {

    /** The first bytes of an OLE2 container: an .xls, or an xlsx saved with a password. */
    private static final byte[] OLE2_HEAD = {-48, -49, 17, -32, -95, -79, 26, -31, 0, 0};

    @Test
    public void refusesAMissingFile() {
        Path missing = OutputFiles.path("missing.xlsx");

        assertThatThrownBy(() -> ExcelReader.read(missing).sheetNames())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("not a file: " + missing);
    }

    @Test
    public void refusesAnXlsOrAPasswordProtectedFile() throws IOException {
        Path old = Files.write(OutputFiles.path("old.xls"), OLE2_HEAD);

        assertThatThrownBy(() -> ExcelReader.read(old).sheetNames())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(old + " is an .xls file (Excel 97-2003) or a password-protected one - save it in Excel as"
                        + " .xlsx, without a password");
    }

    @Test
    public void refusesAFileThatIsNotAZip() throws IOException {
        Path notes = Files.writeString(OutputFiles.path("notes.xlsx"), "Date;Amount\n2026-09-30;12.50\n");

        assertThatThrownBy(() -> ExcelReader.read(notes).sheetNames())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(notes + " is not an xlsx file");
    }

    @Test
    public void refusesAZipThatIsNotAWorkbookWithoutTheEnginesException() throws IOException {
        Path archive = OutputFiles.path("archive.xlsx");
        try (OutputStream out = Files.newOutputStream(archive);
                ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("hello.txt"));
            zip.write("hello".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        assertThatThrownBy(() -> ExcelReader.read(archive).sheetNames())
                .hasMessageStartingWith("cannot read " + archive)
                .isNotInstanceOf(ExcelReaderException.class);
    }

    @Test
    public void anExternalEntityCannotReadAnotherFile() throws IOException {
        Path secret = Files.writeString(OutputFiles.path("secret.txt"), "the secret");
        Path file = XlsxFixture.workbook()
                .sheetPrologue("<!DOCTYPE worksheet [<!ENTITY leak SYSTEM \"" + secret.toUri() + "\">]>")
                .sheet(
                        "Attack",
                        "<row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>Header</t></is></c></row>"
                                + "<row r=\"2\"><c r=\"A2\" t=\"inlineStr\"><is><t>&leak;</t></is></c></row>")
                .save(OutputFiles.path("external-entity.xlsx"));

        assertThatThrownBy(() -> ExcelReader.read(file).firstSheet().rows())
                .hasMessageStartingWith("cannot read " + file)
                .satisfies(refusal -> assertThat(refusal.getMessage()).doesNotContain("the secret"));
    }
}
