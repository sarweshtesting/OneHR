package com.nforceone.nforcehq.servicerequest;

import com.nforceone.nforcehq.audit.AuditLogService;
import com.nforceone.nforcehq.common.ApiException;
import com.nforceone.nforcehq.common.Roles;
import com.nforceone.nforcehq.notification.NotificationService;
import com.nforceone.nforcehq.security.JwtPrincipal;
import com.nforceone.nforcehq.user.User;
import com.nforceone.nforcehq.user.UserRepository;
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
public class ServiceRequestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Transactional
    public ServiceRequestView submit(JwtPrincipal principal, SubmitServiceRequestRequest request) {
        UUID organizationId = requireOrganization(principal);

        ServiceRequest entity = new ServiceRequest();
        entity.setOrganizationId(organizationId);
        entity.setRequesterId(principal.userId());
        entity.setType(request.type());
        entity.setSubject(request.subject());
        entity.setDescription(request.description());
        entity.setStatus(ServiceRequestStatus.OPEN);
        serviceRequestRepository.save(entity);

        userRepository.findByOrganizationId(organizationId).stream()
                .filter(u -> Roles.CAN_MANAGE_PEOPLE.contains(u.getRole().name()))
                .forEach(hr -> notificationService.notify(organizationId, hr.getId(), "SERVICE_REQUEST_SUBMITTED",
                        "New service request", principal.name() + " raised: " + request.subject(), entity.getId()));

        return ServiceRequestView.from(entity, principal.name(), null);
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestView> myRequests(JwtPrincipal principal) {
        UUID organizationId = requireOrganization(principal);
        return serviceRequestRepository
                .findByOrganizationIdAndRequesterIdOrderByCreatedAtDesc(organizationId, principal.userId())
                .stream().map(r -> ServiceRequestView.from(r, principal.name(), resolveName(r.getAssigneeId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestView> inbox(JwtPrincipal principal) {
        UUID organizationId = requireOrganization(principal);
        List<ServiceRequest> requests = serviceRequestRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
        Set<UUID> userIds = requests.stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getRequesterId(), r.getAssigneeId()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        var namesById = userIds.isEmpty() ? java.util.Map.<UUID, String>of() : userRepository.findAllById(userIds)
                .stream().collect(Collectors.toMap(User::getId, User::getFullName));
        return requests.stream()
                .map(r -> ServiceRequestView.from(r, namesById.getOrDefault(r.getRequesterId(), "Unknown"),
                        r.getAssigneeId() != null ? namesById.getOrDefault(r.getAssigneeId(), "Unknown") : null))
                .toList();
    }

    @Transactional
    public ServiceRequestView update(JwtPrincipal principal, UUID id, UpdateServiceRequestRequest request) {
        UUID organizationId = requireOrganization(principal);
        ServiceRequest entity = serviceRequestRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Service request not found"));

        if (request.status() != null) {
            entity.setStatus(request.status());
        }
        if (request.assigneeId() != null) {
            entity.setAssigneeId(request.assigneeId());
        }
        if (request.resolutionNotes() != null) {
            entity.setResolutionNotes(request.resolutionNotes());
        }
        serviceRequestRepository.save(entity);

        auditLogService.record(principal, "SERVICE_REQUEST_UPDATED",
                principal.name() + " updated service request \"" + entity.getSubject() + "\" to " + entity.getStatus());
        notificationService.notify(organizationId, entity.getRequesterId(), "SERVICE_REQUEST_UPDATED",
                "Your service request was updated",
                "\"" + entity.getSubject() + "\" is now " + entity.getStatus(), entity.getId());

        return ServiceRequestView.from(entity, resolveName(entity.getRequesterId()), resolveName(entity.getAssigneeId()));
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
