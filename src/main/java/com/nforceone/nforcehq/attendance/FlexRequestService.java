package com.nforceone.nforcehq.attendance;

import com.nforceone.nforcehq.audit.AuditLogService;
import com.nforceone.nforcehq.common.ApiException;
import com.nforceone.nforcehq.common.ApprovalScope;
import com.nforceone.nforcehq.notification.NotificationService;
import com.nforceone.nforcehq.security.JwtPrincipal;
import com.nforceone.nforcehq.user.User;
import com.nforceone.nforcehq.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FlexRequestService {

    private final AttendanceFlexRequestRepository flexRequestRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Transactional
    public FlexRequestView submit(JwtPrincipal principal, SubmitFlexRequestRequest request) {
        UUID organizationId = requireOrganization(principal);

        AttendanceFlexRequest entity = new AttendanceFlexRequest();
        entity.setOrganizationId(organizationId);
        entity.setUserId(principal.userId());
        entity.setType(request.type());
        entity.setWorkDate(request.workDate());
        entity.setHours(request.hours());
        entity.setReason(request.reason());
        entity.setStatus(RegularizationStatus.PENDING);
        flexRequestRepository.save(entity);

        User requester = userRepository.findById(principal.userId()).orElse(null);
        if (requester != null && requester.getManagerId() != null) {
            notificationService.notify(organizationId, requester.getManagerId(), "FLEX_REQUEST_SUBMITTED",
                    "New " + labelFor(request.type()).toLowerCase() + " request",
                    principal.name() + " requested " + labelFor(request.type()).toLowerCase() + " for " + request.workDate(),
                    entity.getId());
        }

        return FlexRequestView.from(entity, principal.name(), null);
    }

    @Transactional(readOnly = true)
    public List<FlexRequestView> myRequests(JwtPrincipal principal) {
        UUID organizationId = requireOrganization(principal);
        return flexRequestRepository.findByOrganizationIdAndUserIdOrderByCreatedAtDesc(organizationId, principal.userId())
                .stream().map(r -> FlexRequestView.from(r, principal.name(), resolveName(r.getApproverId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FlexRequestView> pending(JwtPrincipal principal) {
        UUID organizationId = requireOrganization(principal);
        List<UUID> userIds = ApprovalScope.resolve(principal, userRepository, organizationId);
        if (userIds.isEmpty()) {
            return List.of();
        }
        var namesById = userRepository.findAllById(Set.copyOf(userIds)).stream()
                .collect(Collectors.toMap(User::getId, User::getFullName));
        return flexRequestRepository
                .findByOrganizationIdAndUserIdInAndStatusOrderByCreatedAtAsc(organizationId, userIds, RegularizationStatus.PENDING)
                .stream()
                .map(r -> FlexRequestView.from(r, namesById.getOrDefault(r.getUserId(), "Unknown"), null))
                .toList();
    }

    @Transactional
    public FlexRequestView approve(JwtPrincipal principal, UUID id) {
        AttendanceFlexRequest request = requirePendingInScope(principal, id);
        request.setStatus(RegularizationStatus.APPROVED);
        request.setApproverId(principal.userId());
        request.setDecidedAt(Instant.now());
        flexRequestRepository.save(request);

        notificationService.notify(request.getOrganizationId(), request.getUserId(), "FLEX_REQUEST_APPROVED",
                labelFor(request.getType()) + " request approved",
                "Your " + labelFor(request.getType()).toLowerCase() + " request for " + request.getWorkDate() + " was approved",
                request.getId());
        auditLogService.record(principal, "FLEX_REQUEST_APPROVED",
                principal.name() + " approved " + resolveName(request.getUserId()) + "'s " + labelFor(request.getType()).toLowerCase()
                        + " request for " + request.getWorkDate());

        return FlexRequestView.from(request, resolveName(request.getUserId()), principal.name());
    }

    @Transactional
    public FlexRequestView reject(JwtPrincipal principal, UUID id) {
        AttendanceFlexRequest request = requirePendingInScope(principal, id);
        request.setStatus(RegularizationStatus.REJECTED);
        request.setApproverId(principal.userId());
        request.setDecidedAt(Instant.now());
        flexRequestRepository.save(request);

        notificationService.notify(request.getOrganizationId(), request.getUserId(), "FLEX_REQUEST_REJECTED",
                labelFor(request.getType()) + " request rejected",
                "Your " + labelFor(request.getType()).toLowerCase() + " request for " + request.getWorkDate() + " was rejected",
                request.getId());
        auditLogService.record(principal, "FLEX_REQUEST_REJECTED",
                principal.name() + " rejected " + resolveName(request.getUserId()) + "'s " + labelFor(request.getType()).toLowerCase()
                        + " request for " + request.getWorkDate());

        return FlexRequestView.from(request, resolveName(request.getUserId()), principal.name());
    }

    private AttendanceFlexRequest requirePendingInScope(JwtPrincipal principal, UUID id) {
        UUID organizationId = requireOrganization(principal);
        AttendanceFlexRequest request = flexRequestRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Request not found"));
        if (request.getStatus() != RegularizationStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "This request has already been decided");
        }
        List<UUID> scope = ApprovalScope.resolve(principal, userRepository, organizationId);
        if (!scope.contains(request.getUserId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This request is outside your approval scope");
        }
        return request;
    }

    private String labelFor(FlexRequestType type) {
        return switch (type) {
            case WFH -> "Work From Home";
            case PARTIAL_DAY_LATE_ARRIVAL -> "Partial Day (Late Arrival)";
            case PARTIAL_DAY_LEAVING_EARLY -> "Partial Day (Leaving Early)";
            case OVERTIME -> "Overtime";
        };
    }

    private String resolveName(UUID userId) {
        return userId == null ? null : userRepository.findById(userId).map(User::getFullName).orElse("Unknown");
    }

    private UUID requireOrganization(JwtPrincipal principal) {
        UUID organizationId = principal.effectiveOrganizationId();
        if (organizationId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select an organization first");
        }
        return organizationId;
    }
}
