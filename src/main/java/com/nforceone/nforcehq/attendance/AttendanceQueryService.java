package com.nforceone.nforcehq.attendance;

import com.nforceone.nforcehq.common.ApiException;
import com.nforceone.nforcehq.security.JwtPrincipal;
import com.nforceone.nforcehq.user.User;
import com.nforceone.nforcehq.user.UserRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceQueryService {

    // A fixed 8h standard shift for overtime/partial-day classification, matching the
    // 09:00-shiftStart cutoff used elsewhere — a per-shift schedule model can replace
    // this once shift management exists.
    private static final int STANDARD_SHIFT_MINUTES = 8 * 60;
    private static final int PARTIAL_DAY_THRESHOLD_MINUTES = 6 * 60;

    private static final Set<String> MANAGER_ROLES = Set.of("MANAGER", "ADMIN", "PLATFORM_ADMIN");

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final UserRepository userRepository;

    public AttendanceSummaryResponse summary(JwtPrincipal principal, String view, String month, UUID departmentId) {
        UUID organizationId = requireOrganization(principal);
        List<UUID> userIds = resolveTargetUserIds(principal, organizationId, view, departmentId);
        YearMonth ym = parseMonth(month);
        LocalDate today = LocalDate.now();
        LocalDate rangeEnd = ym.equals(YearMonth.from(today)) ? today : ym.atEndOfMonth();

        List<AttendanceRecord> records = attendanceRecordRepository
                .findByOrganizationIdAndUserIdInAndWorkDateBetweenOrderByWorkDateDesc(
                        organizationId, userIds, ym.atDay(1), ym.atEndOfMonth());

        long weekdaySlots = countWeekdays(ym.atDay(1), rangeEnd) * (long) userIds.size();

        long officeFull = 0, wfh = 0, partial = 0, onTime = 0, late = 0;
        double overtimeHours = 0;
        int overtimeSessions = 0;
        for (AttendanceRecord r : records) {
            boolean isPartial = r.getTotalWorkedMinutes() != null && r.getTotalWorkedMinutes() < PARTIAL_DAY_THRESHOLD_MINUTES;
            if (r.getMode() == AttendanceMode.WFH) {
                wfh++;
            } else if (isPartial) {
                partial++;
            } else {
                officeFull++;
            }
            if (r.getStatus() == AttendanceStatus.ON_TIME) onTime++;
            if (r.getStatus() == AttendanceStatus.LATE) late++;
            if (r.getTotalWorkedMinutes() != null && r.getTotalWorkedMinutes() > STANDARD_SHIFT_MINUTES) {
                overtimeHours += (r.getTotalWorkedMinutes() - STANDARD_SHIFT_MINUTES) / 60.0;
                overtimeSessions++;
            }
        }

        long recordedSlots = records.size();
        long leaveOffSlots = Math.max(0, weekdaySlots - recordedSlots);
        double denom = weekdaySlots == 0 ? 1 : weekdaySlots;
        long completedShifts = onTime + late;

        return new AttendanceSummaryResponse(
                round(officeFull * 100.0 / denom),
                round(wfh * 100.0 / denom),
                round(partial * 100.0 / denom),
                round(leaveOffSlots * 100.0 / denom),
                completedShifts == 0 ? 0 : round(onTime * 100.0 / completedShifts),
                round(overtimeHours),
                overtimeSessions);
    }

    public List<HeatmapDay> heatmap(JwtPrincipal principal, String month, UUID targetUserId) {
        UUID organizationId = requireOrganization(principal);
        UUID userId = resolveSingleTargetUser(principal, organizationId, targetUserId);
        YearMonth ym = parseMonth(month);

        List<AttendanceRecord> records = attendanceRecordRepository
                .findByOrganizationIdAndUserIdInAndWorkDateBetweenOrderByWorkDateDesc(
                        organizationId, List.of(userId), ym.atDay(1), ym.atEndOfMonth());
        Map<LocalDate, AttendanceRecord> byDate = new HashMap<>();
        records.forEach(r -> byDate.put(r.getWorkDate(), r));

        List<HeatmapDay> days = new java.util.ArrayList<>();
        for (LocalDate d = ym.atDay(1); !d.isAfter(ym.atEndOfMonth()); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) {
                days.add(new HeatmapDay(d, "weekend"));
                continue;
            }
            AttendanceRecord r = byDate.get(d);
            if (r == null) {
                days.add(new HeatmapDay(d, "none"));
            } else if (r.getMode() == AttendanceMode.WFH) {
                days.add(new HeatmapDay(d, "wfh"));
            } else if (r.getTotalWorkedMinutes() != null && r.getTotalWorkedMinutes() < PARTIAL_DAY_THRESHOLD_MINUTES) {
                days.add(new HeatmapDay(d, "partial"));
            } else {
                days.add(new HeatmapDay(d, "full"));
            }
        }
        return days;
    }

    public List<AttendanceLogRow> logs(JwtPrincipal principal, String view, String month, UUID departmentId, int page, int size) {
        UUID organizationId = requireOrganization(principal);
        List<UUID> userIds = resolveTargetUserIds(principal, organizationId, view, departmentId);
        YearMonth ym = parseMonth(month);

        Page<AttendanceRecord> result = attendanceRecordRepository
                .findByOrganizationIdAndUserIdInAndWorkDateBetweenOrderByWorkDateDesc(
                        organizationId, userIds, ym.atDay(1), ym.atEndOfMonth(), PageRequest.of(page, size));

        Map<UUID, String> namesById = namesFor(result.getContent().stream().map(AttendanceRecord::getUserId).toList());
        return result.getContent().stream().map(r -> toRow(r, namesById)).toList();
    }

    public List<AttendanceLogRow> logsForExport(JwtPrincipal principal, String view, String month, UUID departmentId) {
        UUID organizationId = requireOrganization(principal);
        List<UUID> userIds = resolveTargetUserIds(principal, organizationId, view, departmentId);
        YearMonth ym = parseMonth(month);

        List<AttendanceRecord> records = attendanceRecordRepository
                .findByOrganizationIdAndUserIdInAndWorkDateBetweenOrderByWorkDateDesc(
                        organizationId, userIds, ym.atDay(1), ym.atEndOfMonth());
        Map<UUID, String> namesById = namesFor(records.stream().map(AttendanceRecord::getUserId).toList());
        return records.stream().map(r -> toRow(r, namesById)).toList();
    }

    private AttendanceLogRow toRow(AttendanceRecord r, Map<UUID, String> namesById) {
        return new AttendanceLogRow(
                r.getWorkDate(),
                namesById.get(r.getUserId()),
                r.getClockInAt(),
                r.getClockOutAt(),
                r.getTotalBreakMinutes(),
                r.getTotalWorkedMinutes(),
                r.getMode() != null ? r.getMode().name() : null,
                r.getStatus().name());
    }

    private Map<UUID, String> namesFor(List<UUID> userIds) {
        Map<UUID, String> names = new HashMap<>();
        userRepository.findAllById(Set.copyOf(userIds)).forEach(u -> names.put(u.getId(), u.getFullName()));
        return names;
    }

    private List<UUID> resolveTargetUserIds(JwtPrincipal principal, UUID organizationId, String view, UUID departmentId) {
        String v = view == null ? "my" : view.toLowerCase();
        switch (v) {
            case "my":
                return List.of(principal.userId());
            case "team":
                requireManagerRole(principal);
                return userRepository.findByManagerIdAndOrganizationId(principal.userId(), organizationId).stream()
                        .map(User::getId).toList();
            case "org":
                requireManagerRole(principal);
                List<User> users = departmentId != null
                        ? userRepository.findByOrganizationIdAndDepartmentId(organizationId, departmentId)
                        : userRepository.findByOrganizationId(organizationId);
                return users.stream().map(User::getId).toList();
            default:
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid view: " + view);
        }
    }

    private UUID resolveSingleTargetUser(JwtPrincipal principal, UUID organizationId, UUID targetUserId) {
        if (targetUserId == null || targetUserId.equals(principal.userId())) {
            return principal.userId();
        }
        requireManagerRole(principal);
        boolean inOrg = userRepository.findById(targetUserId)
                .map(u -> organizationId.equals(u.getOrganizationId()))
                .orElse(false);
        if (!inOrg) {
            throw new ApiException(HttpStatus.NOT_FOUND, "User not found in this organization");
        }
        return targetUserId;
    }

    private void requireManagerRole(JwtPrincipal principal) {
        if (!MANAGER_ROLES.contains(principal.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Manager or admin role required for this view");
        }
    }

    private UUID requireOrganization(JwtPrincipal principal) {
        UUID organizationId = principal.effectiveOrganizationId();
        if (organizationId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select an organization first");
        }
        return organizationId;
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

    private long countWeekdays(LocalDate start, LocalDate end) {
        long count = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) {
                count++;
            }
        }
        return count;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
