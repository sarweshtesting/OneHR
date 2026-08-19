package com.nforceone.nforcehq.attendance;

import com.nforceone.nforcehq.common.ApiException;
import com.nforceone.nforcehq.common.ApprovalScope;
import com.nforceone.nforcehq.org.Organization;
import com.nforceone.nforcehq.org.OrganizationRepository;
import com.nforceone.nforcehq.security.JwtPrincipal;
import com.nforceone.nforcehq.user.User;
import com.nforceone.nforcehq.user.UserRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A single, browsable feed of attendance anomalies (late arrivals, missed clock-outs,
 * client-hours mismatches) this month — the "Exception Dashboard" HR keeps asking for
 * instead of only seeing today's flagged count on Overview. */
@RestController
@RequestMapping("/api/attendance/exceptions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN','SUPER_ADMIN','PLATFORM_ADMIN')")
public class ExceptionController {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final MismatchService mismatchService;

    @GetMapping
    public List<ExceptionView> list(@AuthenticationPrincipal JwtPrincipal principal) {
        UUID organizationId = principal.effectiveOrganizationId();
        if (organizationId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select an organization first");
        }
        List<UUID> userIds = ApprovalScope.resolve(principal, userRepository, organizationId);
        List<ExceptionView> results = new ArrayList<>();
        if (userIds.isEmpty()) {
            return results;
        }

        ZoneId zone = organizationRepository.findById(organizationId).map(Organization::getTimezone).map(ZoneId::of).orElse(ZoneId.of("UTC"));
        LocalDate today = LocalDate.now(zone);
        YearMonth month = YearMonth.from(today);

        Map<UUID, String> namesById = userRepository.findAllById(Set.copyOf(userIds)).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, User::getFullName));

        List<AttendanceRecord> monthRecords = attendanceRecordRepository
                .findByOrganizationIdAndUserIdInAndWorkDateBetweenOrderByWorkDateDesc(organizationId, userIds, month.atDay(1), month.atEndOfMonth());

        for (AttendanceRecord record : monthRecords) {
            String userName = namesById.getOrDefault(record.getUserId(), "Unknown");
            if (record.getStatus() == AttendanceStatus.LATE) {
                results.add(new ExceptionView("LATE_ARRIVAL", record.getUserId(), userName, record.getWorkDate(),
                        userName + " clocked in late on " + record.getWorkDate(), null));
            }
            if (record.getClockInAt() != null && record.getClockOutAt() == null && record.getWorkDate().isBefore(today)) {
                results.add(new ExceptionView("MISSED_CLOCKOUT", record.getUserId(), userName, record.getWorkDate(),
                        userName + " never clocked out on " + record.getWorkDate(), null));
            }
        }

        mismatchService.openMismatches(principal).forEach(m -> results.add(new ExceptionView(
                "HOURS_MISMATCH", m.userId(), m.userName(), m.workDate(),
                String.format("%s logged %.1fh with %s but attendance shows %.1fh (Δ%.1fh)",
                        m.userName(), m.clientHours(), m.clientName() != null ? m.clientName() : "a client", m.internalHours(), m.hoursDelta()),
                m.pseudoId())));

        return results.stream().sorted((a, b) -> b.workDate().compareTo(a.workDate())).toList();
    }
}
