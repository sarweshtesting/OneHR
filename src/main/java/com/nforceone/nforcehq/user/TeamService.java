package com.nforceone.nforcehq.user;

import com.nforceone.nforcehq.attendance.AttendanceMode;
import com.nforceone.nforcehq.attendance.AttendanceRecord;
import com.nforceone.nforcehq.attendance.AttendanceRecordRepository;
import com.nforceone.nforcehq.attendance.AttendanceSource;
import com.nforceone.nforcehq.attendance.AttendanceStatus;
import com.nforceone.nforcehq.common.ApiException;
import com.nforceone.nforcehq.dashboard.ApprovalItem;
import com.nforceone.nforcehq.dashboard.ApprovalsService;
import com.nforceone.nforcehq.leave.LeaveRequest;
import com.nforceone.nforcehq.leave.LeaveRequestRepository;
import com.nforceone.nforcehq.leave.LeaveRequestStatus;
import com.nforceone.nforcehq.org.Holiday;
import com.nforceone.nforcehq.org.HolidayRepository;
import com.nforceone.nforcehq.security.JwtPrincipal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backs the "My Team" page: everything scoped to the caller's own direct
 * reports (managerId == caller), regardless of what their role would
 * otherwise let them see via {@link com.nforceone.nforcehq.common.ApprovalScope}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final UserRepository userRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final HolidayRepository holidayRepository;
    private final ApprovalsService approvalsService;

    public TeamStats stats(JwtPrincipal principal) {
        UUID organizationId = requireOrganization(principal);
        List<UUID> reportIds = directReportIds(principal, organizationId);
        if (reportIds.isEmpty()) {
            return new TeamStats(0, 0, 0, 0, 0, 0);
        }

        LocalDate today = LocalDate.now();
        List<AttendanceRecord> todayRecords = attendanceRecordRepository
                .findByOrganizationIdAndUserIdInAndWorkDate(organizationId, reportIds, today);

        int onTime = (int) todayRecords.stream().filter(r -> r.getStatus() == AttendanceStatus.ON_TIME).count();
        int late = (int) todayRecords.stream().filter(r -> r.getStatus() == AttendanceStatus.LATE).count();
        int wfh = (int) todayRecords.stream().filter(r -> r.getMode() == AttendanceMode.WFH).count();
        int remote = (int) todayRecords.stream().filter(r -> r.getSource() == AttendanceSource.WEB_CLOCK).count();
        int attentionCount = approvalsService.pendingForUsers(organizationId, reportIds).size();

        return new TeamStats(reportIds.size(), onTime, late, wfh, remote, attentionCount);
    }

    public List<ApprovalItem> attention(JwtPrincipal principal) {
        UUID organizationId = requireOrganization(principal);
        List<UUID> reportIds = directReportIds(principal, organizationId);
        return approvalsService.pendingForUsers(organizationId, reportIds);
    }

    public List<TeamCalendarRow> calendar(JwtPrincipal principal, String month) {
        UUID organizationId = requireOrganization(principal);
        List<User> reports = userRepository.findByManagerIdAndOrganizationId(principal.userId(), organizationId);
        if (reports.isEmpty()) {
            return List.of();
        }
        List<UUID> userIds = reports.stream().map(User::getId).toList();
        YearMonth ym = parseMonth(month);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();
        LocalDate today = LocalDate.now();

        Set<LocalDate> holidays = holidayRepository.findByOrganizationIdOrderByHolidayDateAsc(organizationId).stream()
                .map(Holiday::getHolidayDate)
                .filter(d -> !d.isBefore(monthStart) && !d.isAfter(monthEnd))
                .collect(java.util.stream.Collectors.toSet());

        List<LeaveRequest> leaveOverlaps = leaveRequestRepository
                .findByOrganizationIdAndUserIdInAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        organizationId, userIds, LeaveRequestStatus.APPROVED, monthEnd, monthStart);

        Map<String, AttendanceRecord> recordKey = new HashMap<>();
        attendanceRecordRepository
                .findByOrganizationIdAndUserIdInAndWorkDateBetweenOrderByWorkDateDesc(organizationId, userIds, monthStart, monthEnd)
                .forEach(r -> recordKey.put(r.getUserId() + "|" + r.getWorkDate(), r));

        return reports.stream().map(user -> {
            List<TeamCalendarDay> days = new ArrayList<>();
            for (LocalDate d = monthStart; !d.isAfter(monthEnd); d = d.plusDays(1)) {
                days.add(new TeamCalendarDay(d, dayStatus(user.getId(), d, today, holidays, leaveOverlaps, recordKey)));
            }
            return new TeamCalendarRow(user.getId(), user.getFullName(), user.getAvatarInitials(), days);
        }).toList();
    }

    private String dayStatus(UUID userId, LocalDate date, LocalDate today, Set<LocalDate> holidays,
            List<LeaveRequest> leaveOverlaps, Map<String, AttendanceRecord> recordKey) {
        if (holidays.contains(date)) {
            return "HOLIDAY";
        }
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return "WEEKLY_OFF";
        }
        boolean onLeave = leaveOverlaps.stream().anyMatch(l -> l.getUserId().equals(userId)
                && !date.isBefore(l.getStartDate()) && !date.isAfter(l.getEndDate()));
        if (onLeave) {
            return "ON_LEAVE";
        }
        AttendanceRecord record = recordKey.get(userId + "|" + date);
        if (record != null) {
            return record.getMode() == AttendanceMode.WFH ? "WFH_ON_DUTY" : "PRESENT";
        }
        if (date.isBefore(today)) {
            return "MISSING_ATTENDANCE";
        }
        return "PRESENT";
    }

    public TeamPunctualityResponse punctuality(JwtPrincipal principal, LocalDate start, LocalDate end) {
        UUID organizationId = requireOrganization(principal);
        List<User> reports = userRepository.findByManagerIdAndOrganizationId(principal.userId(), organizationId);
        if (reports.isEmpty()) {
            return new TeamPunctualityResponse(0, 0, 0, List.of(), List.of());
        }
        List<UUID> userIds = reports.stream().map(User::getId).toList();

        List<AttendanceRecord> records = attendanceRecordRepository
                .findByOrganizationIdAndUserIdInAndWorkDateBetweenOrderByWorkDateDesc(organizationId, userIds, start, end);

        Map<UUID, List<AttendanceRecord>> byUser = new HashMap<>();
        records.forEach(r -> byUser.computeIfAbsent(r.getUserId(), k -> new ArrayList<>()).add(r));

        List<PunctualityEntry> leaderboard = reports.stream().map(user -> {
            List<AttendanceRecord> userRecords = byUser.getOrDefault(user.getId(), List.of());
            int total = userRecords.size();
            int onTime = (int) userRecords.stream().filter(r -> r.getStatus() == AttendanceStatus.ON_TIME).count();
            double percent = total == 0 ? 0 : Math.round(onTime * 1000.0 / total) / 10.0;
            return new PunctualityEntry(user.getId(), user.getFullName(), user.getJobTitle(), user.getAvatarInitials(),
                    onTime, total, percent);
        }).sorted((a, b) -> Double.compare(b.onTimePercent(), a.onTimePercent())).toList();

        Map<LocalDate, Integer> onTimeByDate = new HashMap<>();
        for (AttendanceRecord r : records) {
            if (r.getStatus() == AttendanceStatus.ON_TIME) {
                onTimeByDate.merge(r.getWorkDate(), 1, Integer::sum);
            }
        }
        List<DailyOnTimeCount> dailyCounts = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) {
                continue;
            }
            dailyCounts.add(new DailyOnTimeCount(d, onTimeByDate.getOrDefault(d, 0)));
        }

        double avg = dailyCounts.isEmpty() ? 0
                : Math.round(dailyCounts.stream().mapToInt(DailyOnTimeCount::onTimeCount).average().orElse(0) * 10.0) / 10.0;
        int min = dailyCounts.isEmpty() ? 0 : dailyCounts.stream().mapToInt(DailyOnTimeCount::onTimeCount).min().orElse(0);
        int max = dailyCounts.isEmpty() ? 0 : dailyCounts.stream().mapToInt(DailyOnTimeCount::onTimeCount).max().orElse(0);

        return new TeamPunctualityResponse(avg, min, max, leaderboard, dailyCounts);
    }

    private List<UUID> directReportIds(JwtPrincipal principal, UUID organizationId) {
        return userRepository.findByManagerIdAndOrganizationId(principal.userId(), organizationId).stream()
                .map(User::getId).toList();
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid month, expected YYYY-MM");
        }
    }

    private UUID requireOrganization(JwtPrincipal principal) {
        UUID organizationId = principal.effectiveOrganizationId();
        if (organizationId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select an organization first");
        }
        return organizationId;
    }
}
