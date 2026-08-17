package com.nforceone.nforcehq.attendance;

import com.nforceone.nforcehq.common.ApiException;
import com.nforceone.nforcehq.common.ApprovalScope;
import com.nforceone.nforcehq.notification.NotificationService;
import com.nforceone.nforcehq.org.Organization;
import com.nforceone.nforcehq.org.OrganizationRepository;
import com.nforceone.nforcehq.security.JwtPrincipal;
import com.nforceone.nforcehq.user.User;
import com.nforceone.nforcehq.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
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
public class RegularizationService {

    private static final LocalTime LATE_CUTOFF = LocalTime.of(9, 15);

    private final AttendanceRegularizationRequestRepository regularizationRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final NotificationService notificationService;

    @Transactional
    public RegularizationView submit(JwtPrincipal principal, SubmitRegularizationRequest request) {
        UUID organizationId = requireOrganization(principal);

        AttendanceRegularizationRequest entity = new AttendanceRegularizationRequest();
        entity.setOrganizationId(organizationId);
        entity.setUserId(principal.userId());
        entity.setWorkDate(request.workDate());
        entity.setRequestedClockIn(request.requestedClockIn());
        entity.setRequestedClockOut(request.requestedClockOut());
        entity.setReason(request.reason());
        entity.setStatus(RegularizationStatus.PENDING);
        regularizationRepository.save(entity);

        User requester = userRepository.findById(principal.userId()).orElse(null);
        if (requester != null && requester.getManagerId() != null) {
            notificationService.notify(organizationId, requester.getManagerId(), "REGULARIZATION_SUBMITTED",
                    "New regularization request",
                    principal.name() + " requested attendance regularization for " + request.workDate(),
                    entity.getId());
        }

        return RegularizationView.from(entity, principal.name());
    }

    @Transactional(readOnly = true)
    public List<RegularizationView> pending(JwtPrincipal principal) {
        UUID organizationId = requireOrganization(principal);
        List<UUID> userIds = ApprovalScope.resolve(principal, userRepository, organizationId);
        if (userIds.isEmpty()) {
            return List.of();
        }
        var namesById = userRepository.findAllById(java.util.Set.copyOf(userIds)).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, User::getFullName));
        return regularizationRepository
                .findByOrganizationIdAndUserIdInAndStatusOrderByCreatedAtAsc(organizationId, userIds, RegularizationStatus.PENDING)
                .stream()
                .map(r -> RegularizationView.from(r, namesById.getOrDefault(r.getUserId(), "Unknown")))
                .toList();
    }

    @Transactional
    public RegularizationView approve(JwtPrincipal principal, UUID id) {
        AttendanceRegularizationRequest request = requirePendingInScope(principal, id);

        AttendanceRecord record = attendanceRecordRepository
                .findByUserIdAndOrganizationIdAndWorkDate(request.getUserId(), request.getOrganizationId(), request.getWorkDate())
                .orElseGet(() -> {
                    AttendanceRecord r = new AttendanceRecord();
                    r.setOrganizationId(request.getOrganizationId());
                    r.setUserId(request.getUserId());
                    r.setWorkDate(request.getWorkDate());
                    r.setMode(AttendanceMode.OFFICE);
                    return r;
                });

        if (request.getRequestedClockIn() != null) record.setClockInAt(request.getRequestedClockIn());
        if (request.getRequestedClockOut() != null) record.setClockOutAt(request.getRequestedClockOut());
        record.setSource(AttendanceSource.REGULARIZED);

        if (record.getClockInAt() != null && record.getClockOutAt() != null) {
            long totalMinutes = Duration.between(record.getClockInAt(), record.getClockOutAt()).toMinutes();
            record.setTotalWorkedMinutes((int) Math.max(0, totalMinutes - record.getTotalBreakMinutes()));
            ZoneId zone = zoneFor(request.getOrganizationId());
            record.setStatus(record.getClockInAt().atZone(zone).toLocalTime().isAfter(LATE_CUTOFF)
                    ? AttendanceStatus.LATE : AttendanceStatus.ON_TIME);
        }
        attendanceRecordRepository.save(record);

        request.setStatus(RegularizationStatus.APPROVED);
        request.setApproverId(principal.userId());
        request.setDecidedAt(Instant.now());
        request.setResultingAttendanceRecordId(record.getId());
        regularizationRepository.save(request);

        notificationService.notify(request.getOrganizationId(), request.getUserId(), "REGULARIZATION_APPROVED",
                "Regularization request approved",
                "Your attendance regularization for " + request.getWorkDate() + " was approved",
                request.getId());

        return RegularizationView.from(request, resolveName(request.getUserId()));
    }

    @Transactional
    public RegularizationView reject(JwtPrincipal principal, UUID id) {
        AttendanceRegularizationRequest request = requirePendingInScope(principal, id);
        request.setStatus(RegularizationStatus.REJECTED);
        request.setApproverId(principal.userId());
        request.setDecidedAt(Instant.now());
        regularizationRepository.save(request);

        notificationService.notify(request.getOrganizationId(), request.getUserId(), "REGULARIZATION_REJECTED",
                "Regularization request rejected",
                "Your attendance regularization for " + request.getWorkDate() + " was rejected",
                request.getId());

        return RegularizationView.from(request, resolveName(request.getUserId()));
    }

    private AttendanceRegularizationRequest requirePendingInScope(JwtPrincipal principal, UUID id) {
        UUID organizationId = requireOrganization(principal);
        AttendanceRegularizationRequest request = regularizationRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Regularization request not found"));
        if (request.getStatus() != RegularizationStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "This request has already been decided");
        }
        List<UUID> scope = ApprovalScope.resolve(principal, userRepository, organizationId);
        if (!scope.contains(request.getUserId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This request is outside your approval scope");
        }
        return request;
    }

    private String resolveName(UUID userId) {
        return userRepository.findById(userId).map(User::getFullName).orElse("Unknown");
    }

    private ZoneId zoneFor(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .map(Organization::getTimezone)
                .map(ZoneId::of)
                .orElse(ZoneId.of("UTC"));
    }

    private UUID requireOrganization(JwtPrincipal principal) {
        UUID organizationId = principal.effectiveOrganizationId();
        if (organizationId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select an organization first");
        }
        return organizationId;
    }
}
