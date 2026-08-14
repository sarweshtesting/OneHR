package com.nforceone.nforcehq.dashboard;

import com.nforceone.nforcehq.attendance.AttendanceRecord;
import com.nforceone.nforcehq.attendance.AttendanceRecordRepository;
import com.nforceone.nforcehq.attendance.AttendanceRegularizationRequestRepository;
import com.nforceone.nforcehq.attendance.MismatchService;
import com.nforceone.nforcehq.attendance.RegularizationStatus;
import com.nforceone.nforcehq.common.ApiException;
import com.nforceone.nforcehq.common.ApprovalScope;
import com.nforceone.nforcehq.leave.LeaveRequestRepository;
import com.nforceone.nforcehq.leave.LeaveRequestStatus;
import com.nforceone.nforcehq.security.JwtPrincipal;
import com.nforceone.nforcehq.user.UserRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Composes attendance + leave + regularization + mismatch data into the Overview
 * stat cards and the unified "Needs your attention" approvals inbox. Scope is always
 * derived from the caller's role (MANAGER -> direct reports, ADMIN/PLATFORM_ADMIN ->
 * whole org) rather than a client-supplied value, so a manager can't self-elevate to
 * an org-wide view by passing a query parameter.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final UserRepository userRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AttendanceRegularizationRequestRepository regularizationRequestRepository;
    private final MismatchService mismatchService;

    public DashboardStats stats(JwtPrincipal principal) {
        UUID organizationId = requireOrganization(principal);
        List<UUID> userIds = ApprovalScope.resolve(principal, userRepository, organizationId);
        if (userIds.isEmpty()) {
            return new DashboardStats(0, 0, 0, 0, 0, 0);
        }

        LocalDate today = LocalDate.now();
        List<AttendanceRecord> todayRecords = attendanceRecordRepository
                .findByOrganizationIdAndUserIdInAndWorkDate(organizationId, userIds, today);
        int presentToday = (int) todayRecords.stream().filter(r -> r.getClockInAt() != null).count();

        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<AttendanceRecord> weekRecords = attendanceRecordRepository
                .findByOrganizationIdAndUserIdInAndWorkDateBetweenOrderByWorkDateDesc(organizationId, userIds, weekStart, today);
        double avgHours = weekRecords.stream()
                .filter(r -> r.getTotalWorkedMinutes() != null)
                .mapToInt(AttendanceRecord::getTotalWorkedMinutes)
                .average()
                .orElse(0) / 60.0;

        int pendingLeave = leaveRequestRepository
                .findByOrganizationIdAndUserIdInAndStatusOrderByCreatedAtAsc(organizationId, userIds, LeaveRequestStatus.PENDING)
                .size();
        int pendingRegularization = regularizationRequestRepository
                .findByOrganizationIdAndUserIdInAndStatusOrderByCreatedAtAsc(organizationId, userIds, RegularizationStatus.PENDING)
                .size();
        int mismatches = mismatchService.openMismatches(principal).size();

        return new DashboardStats(presentToday, userIds.size(), pendingLeave, pendingRegularization,
                mismatches, Math.round(avgHours * 10.0) / 10.0);
    }

    private UUID requireOrganization(JwtPrincipal principal) {
        UUID organizationId = principal.effectiveOrganizationId();
        if (organizationId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select an organization first");
        }
        return organizationId;
    }
}
