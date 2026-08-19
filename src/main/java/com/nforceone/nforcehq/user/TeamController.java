package com.nforceone.nforcehq.user;

import com.nforceone.nforcehq.attendance.AttendanceBreakRepository;
import com.nforceone.nforcehq.attendance.AttendanceMode;
import com.nforceone.nforcehq.attendance.AttendanceRecord;
import com.nforceone.nforcehq.attendance.AttendanceRecordRepository;
import com.nforceone.nforcehq.dashboard.ApprovalItem;
import com.nforceone.nforcehq.leave.LeaveRequestRepository;
import com.nforceone.nforcehq.leave.LeaveRequestStatus;
import com.nforceone.nforcehq.security.JwtPrincipal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
}
