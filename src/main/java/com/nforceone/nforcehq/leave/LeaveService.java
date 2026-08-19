package com.nforceone.nforcehq.leave;

import com.nforceone.nforcehq.audit.AuditLogService;
import com.nforceone.nforcehq.common.ApiException;
import com.nforceone.nforcehq.notification.NotificationService;
import com.nforceone.nforcehq.security.JwtPrincipal;
import com.nforceone.nforcehq.user.User;
import com.nforceone.nforcehq.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaveService {

    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public List<LeaveTypeSummary> listTypes(JwtPrincipal principal) {
        UUID organizationId = requireOrganization(principal);
        return leaveTypeRepository.findByOrganizationIdAndActiveTrue(organizationId).stream()
                .map(LeaveTypeSummary::from)
                .toList();
    }

    public List<LeaveBalanceView> balances(JwtPrincipal principal) {
        UUID organizationId = requireOrganization(principal);
        int year = LocalDate.now().getYear();
        Map<UUID, LeaveType> typesById = leaveTypeRepository.findByOrganizationIdAndActiveTrue(organizationId).stream()
                .collect(java.util.stream.Collectors.toMap(LeaveType::getId, t -> t));

        return leaveBalanceRepository.findByUserIdAndYear(principal.userId(), year).stream()
                .filter(b -> typesById.containsKey(b.getLeaveTypeId()))
                .map(b -> {
                    LeaveType type = typesById.get(b.getLeaveTypeId());
                    return new LeaveBalanceView(
                            type.getId(), type.getName(), type.getColorToken(),
                            b.getAllocatedDays(), b.getUsedDays(), b.getAllocatedDays().subtract(b.getUsedDays()));
                })
                .toList();
    }

    @Transactional
    public LeaveRequestView apply(JwtPrincipal principal, ApplyLeaveRequest request) {
        UUID organizationId = requireOrganization(principal);
        LeaveType leaveType = leaveTypeRepository.findByIdAndOrganizationId(request.leaveTypeId(), organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Leave type not found"));

        if (request.endDate().isBefore(request.startDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "End date must be on or after start date");
        }
        BigDecimal daysRequested = BigDecimal.valueOf(
                ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1);

        if (leaveType.getCode() != LeaveTypeCode.UNPAID) {
            BigDecimal remaining = leaveBalanceRepository
                    .findByUserIdAndLeaveTypeIdAndYear(principal.userId(), leaveType.getId(), request.startDate().getYear())
                    .map(b -> b.getAllocatedDays().subtract(b.getUsedDays()))
                    .orElse(BigDecimal.ZERO);
            if (daysRequested.compareTo(remaining) > 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Insufficient " + leaveType.getName() + " balance: " + remaining + " day(s) remaining");
            }
        }

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setOrganizationId(organizationId);
        leaveRequest.setUserId(principal.userId());
        leaveRequest.setLeaveTypeId(leaveType.getId());
        leaveRequest.setStartDate(request.startDate());
        leaveRequest.setEndDate(request.endDate());
        leaveRequest.setDaysRequested(daysRequested);
        leaveRequest.setReason(request.reason());
        leaveRequest.setStatus(LeaveRequestStatus.PENDING);
        leaveRequestRepository.save(leaveRequest);

        User requester = userRepository.findById(principal.userId()).orElse(null);
        if (requester != null && requester.getManagerId() != null) {
            notificationService.notify(organizationId, requester.getManagerId(), "LEAVE_SUBMITTED",
                    "New leave request",
                    principal.name() + " requested " + daysRequested + " day(s) of " + leaveType.getName(),
                    leaveRequest.getId());
        }

        return new LeaveRequestView(leaveRequest.getId(), principal.name(), leaveType.getName(), leaveRequest.getStartDate(),
                leaveRequest.getEndDate(), leaveRequest.getDaysRequested(), leaveRequest.getStatus().name());
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestView> pending(JwtPrincipal principal) {
        UUID organizationId = requireOrganization(principal);
        List<UUID> userIds = com.nforceone.nforcehq.common.ApprovalScope.resolve(principal, userRepository, organizationId);
        return pendingFor(organizationId, userIds);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestView> pendingFor(UUID organizationId, List<UUID> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        Map<UUID, LeaveType> typesById = leaveTypeRepository.findByOrganizationIdAndActiveTrue(organizationId).stream()
                .collect(java.util.stream.Collectors.toMap(LeaveType::getId, t -> t));
        Map<UUID, String> namesById = userRepository.findAllById(Set.copyOf(userIds)).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, User::getFullName));

        return leaveRequestRepository
                .findByOrganizationIdAndUserIdInAndStatusOrderByCreatedAtAsc(organizationId, userIds, LeaveRequestStatus.PENDING)
                .stream()
                .map(r -> new LeaveRequestView(r.getId(), namesById.getOrDefault(r.getUserId(), "Unknown"),
                        typesById.containsKey(r.getLeaveTypeId()) ? typesById.get(r.getLeaveTypeId()).getName() : "Leave",
                        r.getStartDate(), r.getEndDate(), r.getDaysRequested(), r.getStatus().name()))
                .toList();
    }

    @Transactional
    public LeaveRequestView approve(JwtPrincipal principal, UUID id) {
        LeaveRequest request = requirePendingInScope(principal, id);
        LeaveType leaveType = leaveTypeRepository.findById(request.getLeaveTypeId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Leave type not found"));

        if (leaveType.getCode() != LeaveTypeCode.UNPAID) {
            LeaveBalance balance = leaveBalanceRepository
                    .findByUserIdAndLeaveTypeIdAndYear(request.getUserId(), request.getLeaveTypeId(), request.getStartDate().getYear())
                    .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "No leave balance record for this employee/type/year"));
            balance.setUsedDays(balance.getUsedDays().add(request.getDaysRequested()));
            leaveBalanceRepository.save(balance);
        }

        request.setStatus(LeaveRequestStatus.APPROVED);
        request.setApproverId(principal.userId());
        request.setDecidedAt(java.time.Instant.now());
        leaveRequestRepository.save(request);

        notificationService.notify(request.getOrganizationId(), request.getUserId(), "LEAVE_APPROVED",
                "Leave request approved",
                "Your " + leaveType.getName() + " request for " + request.getDaysRequested() + " day(s) was approved",
                request.getId());

        auditLogService.record(principal, "LEAVE_APPROVED",
                principal.name() + " approved " + resolveName(request.getUserId()) + "'s " + leaveType.getName() + " request");

        return new LeaveRequestView(request.getId(), resolveName(request.getUserId()), leaveType.getName(),
                request.getStartDate(), request.getEndDate(), request.getDaysRequested(), request.getStatus().name());
    }

    @Transactional
    public LeaveRequestView reject(JwtPrincipal principal, UUID id) {
        LeaveRequest request = requirePendingInScope(principal, id);
        LeaveType leaveType = leaveTypeRepository.findById(request.getLeaveTypeId()).orElse(null);

        request.setStatus(LeaveRequestStatus.REJECTED);
        request.setApproverId(principal.userId());
        request.setDecidedAt(java.time.Instant.now());
        leaveRequestRepository.save(request);

        notificationService.notify(request.getOrganizationId(), request.getUserId(), "LEAVE_REJECTED",
                "Leave request rejected",
                "Your " + (leaveType != null ? leaveType.getName() : "leave") + " request was rejected",
                request.getId());

        auditLogService.record(principal, "LEAVE_REJECTED",
                principal.name() + " rejected " + resolveName(request.getUserId()) + "'s " + (leaveType != null ? leaveType.getName() : "leave") + " request");

        return new LeaveRequestView(request.getId(), resolveName(request.getUserId()),
                leaveType != null ? leaveType.getName() : "Leave",
                request.getStartDate(), request.getEndDate(), request.getDaysRequested(), request.getStatus().name());
    }

    private LeaveRequest requirePendingInScope(JwtPrincipal principal, UUID id) {
        UUID organizationId = requireOrganization(principal);
        LeaveRequest request = leaveRequestRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Leave request not found"));
        if (request.getStatus() != LeaveRequestStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "This request has already been decided");
        }
        List<UUID> scope = com.nforceone.nforcehq.common.ApprovalScope.resolve(principal, userRepository, organizationId);
        if (!scope.contains(request.getUserId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This request is outside your approval scope");
        }
        return request;
    }

    private String resolveName(UUID userId) {
        return userRepository.findById(userId).map(User::getFullName).orElse("Unknown");
    }

    public List<RequestHistoryItem> history(JwtPrincipal principal) {
        UUID organizationId = requireOrganization(principal);
        Map<UUID, LeaveType> typesById = leaveTypeRepository.findByOrganizationIdAndActiveTrue(organizationId).stream()
                .collect(java.util.stream.Collectors.toMap(LeaveType::getId, t -> t));

        return leaveRequestRepository.findByUserIdAndOrganizationIdOrderByCreatedAtDesc(principal.userId(), organizationId)
                .stream()
                .map(r -> new RequestHistoryItem(
                        r.getId(), "LEAVE",
                        typesById.containsKey(r.getLeaveTypeId()) ? typesById.get(r.getLeaveTypeId()).getName() : "Leave",
                        r.getStartDate(), r.getEndDate(), r.getDaysRequested(), r.getStatus().name(), r.getCreatedAt()))
                .toList();
    }

    public List<TeamCalendarEntry> teamCalendar(JwtPrincipal principal, int rangeDays) {
        UUID organizationId = requireOrganization(principal);
        LocalDate today = LocalDate.now();
        LocalDate rangeEnd = today.plusDays(Math.max(1, rangeDays));

        User caller = userRepository.findById(principal.userId()).orElse(null);
        UUID callerDepartmentId = caller != null ? caller.getDepartmentId() : null;

        List<LeaveRequest> upcoming = leaveRequestRepository
                .findByOrganizationIdAndStatusAndStartDateGreaterThanEqualOrderByStartDateAsc(
                        organizationId, LeaveRequestStatus.APPROVED, today)
                .stream()
                .filter(r -> !r.getStartDate().isAfter(rangeEnd))
                .toList();

        Map<UUID, User> usersById = new HashMap<>();
        userRepository.findAllById(Set.copyOf(upcoming.stream().map(LeaveRequest::getUserId).toList()))
                .forEach(u -> usersById.put(u.getId(), u));
        Map<UUID, LeaveType> typesById = leaveTypeRepository.findByOrganizationIdAndActiveTrue(organizationId).stream()
                .collect(java.util.stream.Collectors.toMap(LeaveType::getId, t -> t));

        return upcoming.stream()
                .filter(r -> {
                    if (callerDepartmentId == null) return true;
                    User u = usersById.get(r.getUserId());
                    return u != null && callerDepartmentId.equals(u.getDepartmentId());
                })
                .limit(60)
                .map(r -> {
                    User u = usersById.get(r.getUserId());
                    LeaveType type = typesById.get(r.getLeaveTypeId());
                    return new TeamCalendarEntry(
                            r.getUserId(),
                            u != null ? u.getFullName() : "Unknown",
                            u != null ? u.getAvatarInitials() : "?",
                            r.getStartDate(), r.getEndDate(),
                            type != null ? type.getCode().name() : null,
                            type != null ? type.getName() : "Leave");
                })
                .toList();
    }

    private UUID requireOrganization(JwtPrincipal principal) {
        UUID organizationId = principal.effectiveOrganizationId();
        if (organizationId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select an organization first");
        }
        return organizationId;
    }
}
