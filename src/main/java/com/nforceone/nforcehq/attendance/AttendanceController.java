package com.nforceone.nforcehq.attendance;

import com.nforceone.nforcehq.security.JwtPrincipal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AttendanceQueryService attendanceQueryService;
    private final RegularizationService regularizationService;
    private final MismatchService mismatchService;

    @PostMapping("/clock-in")
    public AttendanceTodayResponse clockIn(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestBody(required = false) ClockInRequest request) {
        return attendanceService.clockIn(principal, request);
    }

    @PostMapping("/clock-out")
    public AttendanceTodayResponse clockOut(@AuthenticationPrincipal JwtPrincipal principal) {
        return attendanceService.clockOut(principal);
    }

    @PostMapping("/breaks/start")
    public AttendanceTodayResponse startBreak(@AuthenticationPrincipal JwtPrincipal principal) {
        return attendanceService.startBreak(principal);
    }

    @PostMapping("/breaks/end")
    public AttendanceTodayResponse endBreak(@AuthenticationPrincipal JwtPrincipal principal) {
        return attendanceService.endBreak(principal);
    }

    @GetMapping("/me/today")
    public AttendanceTodayResponse today(@AuthenticationPrincipal JwtPrincipal principal) {
        return attendanceService.today(principal);
    }

    @GetMapping("/summary")
    public AttendanceSummaryResponse summary(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(required = false) String view,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) UUID departmentId) {
        return attendanceQueryService.summary(principal, view, month, departmentId);
    }

    @GetMapping("/heatmap")
    public List<HeatmapDay> heatmap(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) UUID userId) {
        return attendanceQueryService.heatmap(principal, month, userId);
    }

    @GetMapping("/logs")
    public List<AttendanceLogRow> logs(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(required = false) String view,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return attendanceQueryService.logs(principal, view, month, departmentId, page, size);
    }

    @GetMapping("/logs/export")
    public ResponseEntity<byte[]> exportLogs(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(required = false) String view,
            @RequestParam(required = false) String month) {
        List<AttendanceLogRow> rows = attendanceQueryService.logsForExport(principal, view, month, null);

        StringBuilder csv = new StringBuilder("Date,Employee,Clock In,Clock Out,Break (min),Hours,Mode,Status\n");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm").withZone(java.time.ZoneOffset.UTC);
        for (AttendanceLogRow row : rows) {
            double hours = row.totalWorkedMinutes() != null ? row.totalWorkedMinutes() / 60.0 : 0;
            csv.append(row.workDate()).append(',')
                    .append(row.employeeName() != null ? row.employeeName() : "").append(',')
                    .append(row.clockInAt() != null ? timeFmt.format(row.clockInAt()) : "").append(',')
                    .append(row.clockOutAt() != null ? timeFmt.format(row.clockOutAt()) : "").append(',')
                    .append(row.totalBreakMinutes()).append(',')
                    .append(String.format("%.2f", hours)).append(',')
                    .append(row.mode() != null ? row.mode() : "").append(',')
                    .append(row.status()).append('\n');
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("attendance-logs.csv").build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/regularizations")
    @ResponseStatus(HttpStatus.CREATED)
    public RegularizationView submitRegularization(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestBody SubmitRegularizationRequest request) {
        return regularizationService.submit(principal, request);
    }

    @GetMapping("/mismatches")
    public List<MismatchView> mismatches(@AuthenticationPrincipal JwtPrincipal principal) {
        return mismatchService.openMismatches(principal);
    }

    @PostMapping("/mismatches/{id}/resolve")
    public void resolveMismatch(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable String id) {
        mismatchService.resolve(principal, id);
    }
}
