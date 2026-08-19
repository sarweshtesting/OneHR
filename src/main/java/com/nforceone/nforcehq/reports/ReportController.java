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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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

/** Self-service CSV exports — always scoped to the requester's own data, regardless
 * of role, since these are personal timesheets rather than team/org reports. */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneOffset.UTC);
    private static final String[] HEADERS = {"Date", "Day", "Mode", "Clock In", "Clock Out", "Hours Worked", "Status", "Client Hours"};

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final ClientLogRepository clientLogRepository;

    @GetMapping("/monthly")
    public ResponseEntity<byte[]> monthly(@AuthenticationPrincipal JwtPrincipal principal, @RequestParam String month) {
        YearMonth ym;
        try {
            ym = YearMonth.parse(month);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "month must be in YYYY-MM format");
        }
        return export(principal, ym.atDay(1), ym.atEndOfMonth(), "monthly-report-" + month + ".csv");
    }

    @GetMapping("/weekly-timesheet")
    public ResponseEntity<byte[]> weeklyTimesheet(@AuthenticationPrincipal JwtPrincipal principal, @RequestParam String weekStart) {
        LocalDate start;
        try {
            start = LocalDate.parse(weekStart);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "weekStart must be in YYYY-MM-DD format");
        }
        return export(principal, start, start.plusDays(6), "weekly-timesheet-" + weekStart + ".csv");
    }

    private ResponseEntity<byte[]> export(JwtPrincipal principal, LocalDate from, LocalDate to, String filename) {
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

        StringBuilder csv = new StringBuilder(String.join(",", HEADERS)).append('\n');
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            AttendanceRecord record = recordsByDate.get(date);
            List<ClientLog> logs = clientLogsByDate.getOrDefault(date, List.of());
            String clientHours = logs.stream()
                    .map(l -> l.getClientName() + ": " + String.format("%.2f", l.getLoggedHours().doubleValue()) + "h")
                    .collect(Collectors.joining("; "));

            csv.append(date).append(',')
                    .append(date.getDayOfWeek() == DayOfWeek.SUNDAY || date.getDayOfWeek() == DayOfWeek.SATURDAY ? "Weekend" : date.getDayOfWeek()).append(',')
                    .append(record != null && record.getMode() != null ? record.getMode() : "").append(',')
                    .append(record != null && record.getClockInAt() != null ? TIME_FMT.format(record.getClockInAt()) : "").append(',')
                    .append(record != null && record.getClockOutAt() != null ? TIME_FMT.format(record.getClockOutAt()) : "").append(',')
                    .append(record != null && record.getTotalWorkedMinutes() != null ? String.format("%.2f", record.getTotalWorkedMinutes() / 60.0) : "").append(',')
                    .append(record != null ? record.getStatus() : "NO_RECORD").append(',')
                    .append(escapeCsv(clientHours)).append('\n');
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).contentType(MediaType.parseMediaType("text/csv")).body(csv.toString().getBytes());
    }

    private static String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
