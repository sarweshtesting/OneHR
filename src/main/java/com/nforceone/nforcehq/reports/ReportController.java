package com.nforceone.nforcehq.reports;

import com.nforceone.nforcehq.attendance.AttendanceRecord;
import com.nforceone.nforcehq.attendance.AttendanceRecordRepository;
import com.nforceone.nforcehq.attendance.ClientLog;
import com.nforceone.nforcehq.attendance.ClientLogRepository;
import com.nforceone.nforcehq.common.ApiException;
import com.nforceone.nforcehq.security.JwtPrincipal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Self-service exports — always scoped to the requester's own data, regardless of
 * role, since these are personal timesheets rather than team/org reports. */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final ClientLogRepository clientLogRepository;
    private final ReportExportService reportExportService;

    @GetMapping("/monthly")
    public ResponseEntity<byte[]> monthly(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam String month,
            @RequestParam(defaultValue = "csv") String format) {
        YearMonth ym;
        try {
            ym = YearMonth.parse(month);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "month must be in YYYY-MM format");
        }
        return export(principal, ym.atDay(1), ym.atEndOfMonth(), "Monthly report — " + month, "monthly-report-" + month, format);
    }

    @GetMapping("/weekly-timesheet")
    public ResponseEntity<byte[]> weeklyTimesheet(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam String weekStart,
            @RequestParam(defaultValue = "csv") String format) {
        LocalDate start;
        try {
            start = LocalDate.parse(weekStart);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "weekStart must be in YYYY-MM-DD format");
        }
        return export(principal, start, start.plusDays(6), "Weekly timesheet — " + weekStart, "weekly-timesheet-" + weekStart, format);
    }

    private ResponseEntity<byte[]> export(JwtPrincipal principal, LocalDate from, LocalDate to, String title, String filenameBase, String format) {
        UUID organizationId = principal.effectiveOrganizationId();
        if (organizationId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select an organization first");
        }
        UUID userId = principal.userId();

        Map<LocalDate, AttendanceRecord> recordsByDate = attendanceRecordRepository
                .findByOrganizationIdAndUserIdInAndWorkDateBetweenOrderByWorkDateDesc(organizationId, List.of(userId), from, to)
                .stream()
                .collect(Collectors.toMap(AttendanceRecord::getWorkDate, r -> r));

        Map<LocalDate, List<ClientLog>> clientLogsByDate = clientLogRepository
                .findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(userId, from, to)
                .stream()
                .collect(Collectors.groupingBy(ClientLog::getWorkDate));

        List<ReportRow> rows = new java.util.ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            AttendanceRecord record = recordsByDate.get(date);
            List<ClientLog> logs = clientLogsByDate.getOrDefault(date, List.of());
            String clientHours = logs.stream()
                    .map(l -> l.getClientName() + ": " + String.format("%.2f", l.getLoggedHours().doubleValue()) + "h")
                    .collect(Collectors.joining("; "));
            String day = date.getDayOfWeek() == DayOfWeek.SUNDAY || date.getDayOfWeek() == DayOfWeek.SATURDAY
                    ? "Weekend" : date.getDayOfWeek().toString();

            rows.add(new ReportRow(
                    date, day,
                    record != null ? (record.getMode() != null ? record.getMode().name() : null) : null,
                    record != null ? record.getClockInAt() : null,
                    record != null ? record.getClockOutAt() : null,
                    record != null && record.getTotalWorkedMinutes() != null ? record.getTotalWorkedMinutes() / 60.0 : null,
                    record != null ? record.getStatus().name() : "NO_RECORD",
                    clientHours));
        }

        byte[] body;
        MediaType contentType;
        String extension;
        switch (format.toLowerCase()) {
            case "xlsx" -> {
                body = reportExportService.toXlsx(rows, title.length() > 31 ? title.substring(0, 31) : title);
                contentType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                extension = "xlsx";
            }
            case "pdf" -> {
                body = reportExportService.toPdf(rows, title);
                contentType = MediaType.APPLICATION_PDF;
                extension = "pdf";
            }
            case "csv" -> {
                body = reportExportService.toCsv(rows);
                contentType = MediaType.parseMediaType("text/csv");
                extension = "csv";
            }
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported export format: " + format);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(filenameBase + "." + extension).build());
        return ResponseEntity.ok().headers(headers).contentType(contentType).body(body);
    }
}
