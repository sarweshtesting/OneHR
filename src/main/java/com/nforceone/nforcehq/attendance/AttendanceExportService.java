package com.nforceone.nforcehq.attendance;

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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/** Renders attendance log rows into CSV, XLSX, or PDF bytes for the Attendance tab's export menu. */
@Component
public class AttendanceExportService {

    private static final String[] HEADERS = {"Date", "Employee", "Clock In", "Clock Out", "Break (min)", "Hours", "Mode", "Status"};
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneOffset.UTC);

    public byte[] toCsv(List<AttendanceLogRow> rows) {
        StringBuilder csv = new StringBuilder(String.join(",", HEADERS)).append('\n');
        for (AttendanceLogRow row : rows) {
            csv.append(row.workDate()).append(',')
                    .append(row.employeeName() != null ? row.employeeName() : "").append(',')
                    .append(row.clockInAt() != null ? TIME_FMT.format(row.clockInAt()) : "").append(',')
                    .append(row.clockOutAt() != null ? TIME_FMT.format(row.clockOutAt()) : "").append(',')
                    .append(row.totalBreakMinutes()).append(',')
                    .append(String.format("%.2f", hours(row))).append(',')
                    .append(row.mode() != null ? row.mode() : "").append(',')
                    .append(row.status()).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] toXlsx(List<AttendanceLogRow> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Attendance");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }
            int rowIndex = 1;
            for (AttendanceLogRow row : rows) {
                Row r = sheet.createRow(rowIndex++);
                r.createCell(0).setCellValue(row.workDate().toString());
                r.createCell(1).setCellValue(row.employeeName() != null ? row.employeeName() : "");
                r.createCell(2).setCellValue(row.clockInAt() != null ? TIME_FMT.format(row.clockInAt()) : "");
                r.createCell(3).setCellValue(row.clockOutAt() != null ? TIME_FMT.format(row.clockOutAt()) : "");
                r.createCell(4).setCellValue(row.totalBreakMinutes());
                r.createCell(5).setCellValue(hours(row));
                r.createCell(6).setCellValue(row.mode() != null ? row.mode() : "");
                r.createCell(7).setCellValue(row.status());
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

    public byte[] toPdf(List<AttendanceLogRow> rows) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            float margin = 40;
            float rowHeight = 16;
            float[] colWidths = {60, 100, 55, 55, 60, 45, 55, 60};

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream content = new PDPageContentStream(document, page);
            float y = page.getMediaBox().getHeight() - margin;
            y = writeRow(content, HEADERS, colWidths, margin, y, rowHeight, true);

            for (AttendanceLogRow row : rows) {
                if (y < margin + rowHeight) {
                    content.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    content = new PDPageContentStream(document, page);
                    y = page.getMediaBox().getHeight() - margin;
                    y = writeRow(content, HEADERS, colWidths, margin, y, rowHeight, true);
                }
                String[] cells = {
                        row.workDate().toString(),
                        row.employeeName() != null ? row.employeeName() : "",
                        row.clockInAt() != null ? TIME_FMT.format(row.clockInAt()) : "",
                        row.clockOutAt() != null ? TIME_FMT.format(row.clockOutAt()) : "",
                        String.valueOf(row.totalBreakMinutes()),
                        String.format("%.2f", hours(row)),
                        row.mode() != null ? row.mode() : "",
                        row.status(),
                };
                y = writeRow(content, cells, colWidths, margin, y, rowHeight, false);
            }
            content.close();
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private float writeRow(PDPageContentStream content, String[] cells, float[] colWidths, float margin, float y,
            float rowHeight, boolean bold) throws IOException {
        content.setFont(bold ? new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD) : new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
        float x = margin;
        for (int i = 0; i < cells.length; i++) {
            content.beginText();
            content.newLineAtOffset(x, y);
            content.showText(truncate(cells[i], 18));
            content.endText();
            x += colWidths[i];
        }
        return y - rowHeight;
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    private double hours(AttendanceLogRow row) {
        return row.totalWorkedMinutes() != null ? row.totalWorkedMinutes() / 60.0 : 0;
    }
}
