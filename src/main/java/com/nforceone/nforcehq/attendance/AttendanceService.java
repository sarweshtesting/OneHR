package com.nforceone.nforcehq.attendance;

import com.nforceone.nforcehq.common.ApiException;
import com.nforceone.nforcehq.org.Organization;
import com.nforceone.nforcehq.org.OrganizationRepository;
import com.nforceone.nforcehq.security.JwtPrincipal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    // Simple, org-wide punctuality cutoff for phase 1 of attendance — a per-shift
    // schedule model can replace this once shift management exists.
    private static final LocalTime LATE_CUTOFF = LocalTime.of(9, 15);

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceBreakRepository attendanceBreakRepository;
    private final OrganizationRepository organizationRepository;
    private final ClientRepository clientRepository;
    private final ClientLogRepository clientLogRepository;

    @Transactional
    public AttendanceTodayResponse clockIn(JwtPrincipal principal, ClockInRequest request) {
        UUID organizationId = requireOrganization(principal);
        ZoneId zone = zoneFor(organizationId);
        LocalDate workDate = LocalDate.now(zone);

        attendanceRecordRepository
                .findByUserIdAndOrganizationIdAndWorkDate(principal.userId(), organizationId, workDate)
                .ifPresent(existing -> {
                    if (existing.getClockOutAt() == null) {
                        throw new ApiException(HttpStatus.CONFLICT, "Already clocked in for today");
                    }
                    throw new ApiException(HttpStatus.CONFLICT, "Today's shift is already complete");
                });

        AttendanceRecord record = new AttendanceRecord();
        record.setOrganizationId(organizationId);
        record.setUserId(principal.userId());
        record.setWorkDate(workDate);
        record.setClockInAt(Instant.now());
        record.setMode(request != null && request.mode() != null ? request.mode() : AttendanceMode.OFFICE);
        record.setStatus(AttendanceStatus.IN_PROGRESS);
        record.setSource(AttendanceSource.WEB_CLOCK);

        if (request != null && request.clientId() != null) {
            Client client = clientRepository.findById(request.clientId())
                    .filter(c -> c.getOrganizationId().equals(organizationId))
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown client"));
            record.setClientId(client.getId());
        }

        attendanceRecordRepository.save(record);

        return toResponse(record, List.of());
    }

    @Transactional
    public AttendanceTodayResponse clockOut(JwtPrincipal principal) {
        AttendanceRecord record = requireOpenRecord(principal);
        ZoneId zone = zoneFor(record.getOrganizationId());

        attendanceBreakRepository.findFirstByAttendanceRecordIdAndBreakEndAtIsNull(record.getId())
                .ifPresent(openBreak -> closeBreak(record, openBreak, Instant.now()));

        Instant now = Instant.now();
        record.setClockOutAt(now);
        long totalMinutes = Duration.between(record.getClockInAt(), now).toMinutes();
        int workedMinutes = (int) Math.max(0, totalMinutes - record.getTotalBreakMinutes());
        record.setTotalWorkedMinutes(workedMinutes);
        record.setStatus(isLate(record.getClockInAt(), zone) ? AttendanceStatus.LATE : AttendanceStatus.ON_TIME);
        attendanceRecordRepository.save(record);

        if (record.getClientId() != null) {
            logClientHoursForShift(record, workedMinutes);
        }

        return toResponse(record, breaksFor(record.getId()));
    }

    @Transactional
    public AttendanceTodayResponse startBreak(JwtPrincipal principal) {
        AttendanceRecord record = requireOpenRecord(principal);

        attendanceBreakRepository.findFirstByAttendanceRecordIdAndBreakEndAtIsNull(record.getId())
                .ifPresent(b -> {
                    throw new ApiException(HttpStatus.CONFLICT, "A break is already in progress");
                });

        AttendanceBreak attendanceBreak = new AttendanceBreak();
        attendanceBreak.setAttendanceRecordId(record.getId());
        attendanceBreak.setBreakStartAt(Instant.now());
        attendanceBreak.setBreakType(BreakType.SHORT);
        attendanceBreakRepository.save(attendanceBreak);

        return toResponse(record, breaksFor(record.getId()));
    }

    @Transactional
    public AttendanceTodayResponse endBreak(JwtPrincipal principal) {
        AttendanceRecord record = requireOpenRecord(principal);
        AttendanceBreak openBreak = attendanceBreakRepository
                .findFirstByAttendanceRecordIdAndBreakEndAtIsNull(record.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "No break is currently in progress"));

        closeBreak(record, openBreak, Instant.now());
        attendanceRecordRepository.save(record);

        return toResponse(record, breaksFor(record.getId()));
    }

    @Transactional(readOnly = true)
    public AttendanceTodayResponse today(JwtPrincipal principal) {
        UUID organizationId = principal.effectiveOrganizationId();
        if (organizationId == null) {
            return AttendanceTodayResponse.empty();
        }
        LocalDate workDate = LocalDate.now(zoneFor(organizationId));
        return attendanceRecordRepository
                .findByUserIdAndOrganizationIdAndWorkDate(principal.userId(), organizationId, workDate)
                .map(record -> toResponse(record, breaksFor(record.getId())))
                .orElseGet(AttendanceTodayResponse::empty);
    }

    private AttendanceRecord requireOpenRecord(JwtPrincipal principal) {
        UUID organizationId = requireOrganization(principal);
        LocalDate workDate = LocalDate.now(zoneFor(organizationId));
        AttendanceRecord record = attendanceRecordRepository
                .findByUserIdAndOrganizationIdAndWorkDate(principal.userId(), organizationId, workDate)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Not currently clocked in"));
        if (record.getClockOutAt() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "Not currently clocked in");
        }
        return record;
    }

    private void closeBreak(AttendanceRecord record, AttendanceBreak openBreak, Instant endAt) {
        openBreak.setBreakEndAt(endAt);
        long minutes = Duration.between(openBreak.getBreakStartAt(), endAt).toMinutes();
        record.setTotalBreakMinutes(record.getTotalBreakMinutes() + (int) minutes);
        attendanceBreakRepository.save(openBreak);
    }

    private boolean isLate(Instant clockInAt, ZoneId zone) {
        return clockInAt.atZone(zone).toLocalTime().isAfter(LATE_CUTOFF);
    }

    private UUID requireOrganization(JwtPrincipal principal) {
        UUID organizationId = principal.effectiveOrganizationId();
        if (organizationId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select an organization first");
        }
        return organizationId;
    }

    private ZoneId zoneFor(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .map(Organization::getTimezone)
                .map(ZoneId::of)
                .orElse(ZoneId.of("UTC"));
    }

    private List<BreakSegment> breaksFor(UUID attendanceRecordId) {
        return attendanceBreakRepository.findByAttendanceRecordIdOrderByBreakStartAtAsc(attendanceRecordId).stream()
                .map(BreakSegment::from)
                .toList();
    }

    /** Auto-logs the just-finished shift's hours against the client picked at clock-in —
     * one client_logs row per shift, upserted in case clock-out is somehow called twice. */
    private void logClientHoursForShift(AttendanceRecord record, int workedMinutes) {
        Client client = clientRepository.findById(record.getClientId()).orElse(null);
        if (client == null) {
            return;
        }
        java.math.BigDecimal hours = java.math.BigDecimal.valueOf(workedMinutes)
                .divide(java.math.BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);

        ClientLog log = clientLogRepository
                .findByUserIdAndWorkDateAndClientIdAndSource(record.getUserId(), record.getWorkDate(), client.getId(), "AUTO_CLOCKOUT")
                .orElseGet(ClientLog::new);
        log.setOrganizationId(record.getOrganizationId());
        log.setUserId(record.getUserId());
        log.setWorkDate(record.getWorkDate());
        log.setClientId(client.getId());
        log.setClientName(client.getName());
        log.setLoggedHours(hours);
        log.setSource("AUTO_CLOCKOUT");
        clientLogRepository.save(log);
    }

    private AttendanceTodayResponse toResponse(AttendanceRecord record, List<BreakSegment> breaks) {
        boolean onBreak = breaks.stream().anyMatch(b -> b.endAt() == null);
        String clientName = record.getClientId() != null
                ? clientRepository.findById(record.getClientId()).map(Client::getName).orElse(null)
                : null;
        return new AttendanceTodayResponse(
                record.getId(),
                record.getWorkDate(),
                record.getClockInAt(),
                record.getClockOutAt(),
                record.getMode() != null ? record.getMode().name() : null,
                record.getStatus().name(),
                record.getTotalBreakMinutes(),
                record.getTotalWorkedMinutes(),
                onBreak,
                breaks,
                record.getClientId(),
                clientName);
    }
}
