package com.nforceone.nforcehq.user;

import com.nforceone.nforcehq.attendance.AttendanceBreakRepository;
import com.nforceone.nforcehq.attendance.AttendanceMode;
import com.nforceone.nforcehq.attendance.AttendanceRecord;
import com.nforceone.nforcehq.attendance.AttendanceRecordRepository;
import com.nforceone.nforcehq.attendance.RegularizationView;
import com.nforceone.nforcehq.common.ApiException;
import com.nforceone.nforcehq.dashboard.ApprovalItem;
import com.nforceone.nforcehq.leave.LeaveRequestRepository;
import com.nforceone.nforcehq.leave.LeaveRequestStatus;
import com.nforceone.nforcehq.reports.TeamReportExportService;
import com.nforceone.nforcehq.reports.TeamReportRow;
import com.nforceone.nforcehq.security.JwtPrincipal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/team")
@RequiredArgsConstructor
public class TeamController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneOffset.UTC);

    private final UserRepository userRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceBreakRepository attendanceBreakRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final TeamService teamService;
    private final TeamReportExportService teamReportExportService;

    @GetMapping("/today-status")
    public List<TeamMemberStatus> todayStatus(@AuthenticationPrincipal JwtPrincipal principal) {
        UUID organizationId = principal.effectiveOrganizationId();
        if (organizationId == null) {
            return List.of();
        }

        List<User> reports = userRepository.findByManagerIdAndOrganizationId(principal.userId(), organizationId);
        if (reports.isEmpty()) {
            return List.of();
        }
        List<UUID> userIds = reports.stream().map(User::getId).toList();
        LocalDate today = LocalDate.now();

        Map<UUID, AttendanceRecord> recordsByUser = new HashMap<>();
        attendanceRecordRepository.findByOrganizationIdAndUserIdInAndWorkDate(organizationId, userIds, today)
                .forEach(r -> recordsByUser.put(r.getUserId(), r));

        Set<UUID> onBreakRecordIds = attendanceBreakRepository
                .findByAttendanceRecordIdInAndBreakEndAtIsNull(recordsByUser.values().stream().map(AttendanceRecord::getId).toList())
                .stream().map(com.nforceone.nforcehq.attendance.AttendanceBreak::getAttendanceRecordId)
                .collect(java.util.stream.Collectors.toSet());

        Set<UUID> onLeaveToday = leaveRequestRepository
                .findByOrganizationIdAndUserIdInAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        organizationId, userIds, LeaveRequestStatus.APPROVED, today, today)
                .stream().map(com.nforceone.nforcehq.leave.LeaveRequest::getUserId)
                .collect(java.util.stream.Collectors.toSet());

        return reports.stream().map(user -> {
            if (onLeaveToday.contains(user.getId())) {
                return new TeamMemberStatus(user.getId(), user.getFullName(), user.getJobTitle(),
                        user.getAvatarInitials(), "On leave", null);
            }
            AttendanceRecord record = recordsByUser.get(user.getId());
            if (record == null || record.getClockInAt() == null) {
                return new TeamMemberStatus(user.getId(), user.getFullName(), user.getJobTitle(),
                        user.getAvatarInitials(), "Not clocked in", null);
            }
            String clockInTime = TIME_FMT.format(record.getClockInAt());
            if (onBreakRecordIds.contains(record.getId())) {
                return new TeamMemberStatus(user.getId(), user.getFullName(), user.getJobTitle(),
                        user.getAvatarInitials(), "On break", clockInTime);
            }
            if (record.getClockOutAt() != null) {
                return new TeamMemberStatus(user.getId(), user.getFullName(), user.getJobTitle(),
                        user.getAvatarInitials(), "Clocked out", clockInTime);
            }
            String status = record.getMode() == AttendanceMode.WFH ? "Remote" : "In office";
            return new TeamMemberStatus(user.getId(), user.getFullName(), user.getJobTitle(),
                    user.getAvatarInitials(), status, clockInTime);
        }).toList();
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN','SUPER_ADMIN','PLATFORM_ADMIN')")
    public TeamStats stats(@AuthenticationPrincipal JwtPrincipal principal) {
        return teamService.stats(principal);
    }

    @GetMapping("/attention")
    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN','SUPER_ADMIN','PLATFORM_ADMIN')")
    public List<ApprovalItem> attention(@AuthenticationPrincipal JwtPrincipal principal) {
        return teamService.attention(principal);
    }

    @GetMapping("/calendar")
    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN','SUPER_ADMIN','PLATFORM_ADMIN')")
    public List<TeamCalendarRow> calendar(@AuthenticationPrincipal JwtPrincipal principal, @RequestParam(required = false) String month) {
        return teamService.calendar(principal, month);
    }

    @GetMapping("/punctuality")
    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN','SUPER_ADMIN','PLATFORM_ADMIN')")
    public TeamPunctualityResponse punctuality(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        LocalDate today = LocalDate.now();
        LocalDate rangeEnd = end != null ? LocalDate.parse(end) : today;
        LocalDate rangeStart = start != null ? LocalDate.parse(start) : rangeEnd.minusDays(6);
        return teamService.punctuality(principal, rangeStart, rangeEnd);
    }

    @GetMapping("/negligence")
    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN','SUPER_ADMIN','PLATFORM_ADMIN')")
    public List<NegligenceEntry> negligence(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        LocalDate today = LocalDate.now();
        LocalDate rangeEnd = end != null ? LocalDate.parse(end) : today;
        LocalDate rangeStart = start != null ? LocalDate.parse(start) : rangeEnd.minusDays(29);
        return teamService.negligence(principal, rangeStart, rangeEnd);
    }

    @GetMapping("/regularizations")
    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN','SUPER_ADMIN','PLATFORM_ADMIN')")
    public List<RegularizationView> regularizations(@AuthenticationPrincipal JwtPrincipal principal) {
        return teamService.regularizations(principal);
    }

    @GetMapping("/assignments")
    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN','SUPER_ADMIN','PLATFORM_ADMIN')")
    public List<AssignmentEntry> assignments(@AuthenticationPrincipal JwtPrincipal principal) {
        return teamService.assignments(principal);
    }

    @GetMapping("/reports/monthly")
    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN','SUPER_ADMIN','PLATFORM_ADMIN')")
    public ResponseEntity<byte[]> teamMonthlyReport(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam String month,
            @RequestParam(defaultValue = "csv") String format) {
        YearMonth ym;
        try {
            ym = YearMonth.parse(month);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "month must be in YYYY-MM format");
        }
        return exportTeamReport(principal, ym.atDay(1), ym.atEndOfMonth(), "Team monthly report — " + month, "team-monthly-report-" + month, format);
    }

    @GetMapping("/reports/weekly-timesheet")
    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN','SUPER_ADMIN','PLATFORM_ADMIN')")
    public ResponseEntity<byte[]> teamWeeklyTimesheet(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam String weekStart,
            @RequestParam(defaultValue = "csv") String format) {
        LocalDate start;
        try {
            start = LocalDate.parse(weekStart);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "weekStart must be in YYYY-MM-DD format");
        }
        return exportTeamReport(principal, start, start.plusDays(6), "Team weekly timesheet — " + weekStart, "team-weekly-timesheet-" + weekStart, format);
    }

    private ResponseEntity<byte[]> exportTeamReport(JwtPrincipal principal, LocalDate from, LocalDate to, String title, String filenameBase, String format) {
        List<TeamReportRow> rows = teamService.reportRows(principal, from, to);

        byte[] body;
        MediaType contentType;
        String extension;
        switch (format.toLowerCase()) {
            case "xlsx" -> {
                body = teamReportExportService.toXlsx(rows, title.length() > 31 ? title.substring(0, 31) : title);
                contentType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                extension = "xlsx";
            }
            case "pdf" -> {
                body = teamReportExportService.toPdf(rows, title);
                contentType = MediaType.APPLICATION_PDF;
                extension = "pdf";
            }
            case "csv" -> {
                body = teamReportExportService.toCsv(rows);
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
