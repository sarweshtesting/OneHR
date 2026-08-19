package com.nforceone.nforcehq.reports;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/** Renders monthly/weekly report rows into CSV, XLSX, or PDF bytes — mirrors
 * AttendanceExportService's approach, just for the self-service reports. */
@Component
public class ReportExportService {

    private static final String[] HEADERS = {"Date", "Day", "Mode", "Clock In", "Clock Out", "Hours Worked", "Status", "Client Hours"};
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneOffset.UTC);

    public byte[] toCsv(List<ReportRow> rows) {
        StringBuilder csv = new StringBuilder(String.join(",", HEADERS)).append('\n');
        for (ReportRow row : rows) {
            for (String cell : cells(row)) {
                csv.append(escapeCsv(cell)).append(',');
            }
            csv.setLength(csv.length() - 1);
            csv.append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] toXlsx(List<ReportRow> rows, String sheetName) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet(sheetName);
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }
            int rowIndex = 1;
            for (ReportRow row : rows) {
                Row r = sheet.createRow(rowIndex++);
                String[] cells = cells(row);
                for (int i = 0; i < cells.length; i++) {
                    r.createCell(i).setCellValue(cells[i]);
                }
            }
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public byte[] toPdf(List<ReportRow> rows, String title) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            float margin = 40;
            float rowHeight = 16;
            float[] colWidths = {55, 65, 45, 50, 50, 60, 55, 140};

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream content = new PDPageContentStream(document, page);
            float y = page.getMediaBox().getHeight() - margin;

            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
            content.beginText();
            content.newLineAtOffset(margin, y);
            content.showText(title);
            content.endText();
            y -= rowHeight * 1.5f;

            y = writeRow(content, HEADERS, colWidths, margin, y, rowHeight, true);

            for (ReportRow row : rows) {
                if (y < margin + rowHeight) {
                    content.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    content = new PDPageContentStream(document, page);
                    y = page.getMediaBox().getHeight() - margin;
                    y = writeRow(content, HEADERS, colWidths, margin, y, rowHeight, true);
                }
                y = writeRow(content, cells(row), colWidths, margin, y, rowHeight, false);
            }
            content.close();
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private String[] cells(ReportRow row) {
        return new String[] {
                row.date().toString(),
                row.day(),
                row.mode() != null ? row.mode() : "",
                row.clockInAt() != null ? TIME_FMT.format(row.clockInAt()) : "",
                row.clockOutAt() != null ? TIME_FMT.format(row.clockOutAt()) : "",
                row.hoursWorked() != null ? String.format("%.2f", row.hoursWorked()) : "",
                row.status() != null ? row.status() : "",
                row.clientHours() != null ? row.clientHours() : "",
        };
    }

    private float writeRow(PDPageContentStream content, String[] cells, float[] colWidths, float margin, float y,
            float rowHeight, boolean bold) throws IOException {
        content.setFont(bold ? new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD) : new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
        float x = margin;
        for (int i = 0; i < cells.length; i++) {
            content.beginText();
            content.newLineAtOffset(x, y);
            content.showText(truncate(cells[i], 28));
            content.endText();
            x += colWidths[i];
        }
        return y - rowHeight;
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    private static String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
