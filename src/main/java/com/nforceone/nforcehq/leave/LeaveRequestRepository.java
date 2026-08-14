package com.nforceone.nforcehq.leave;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {

    List<LeaveRequest> findByUserIdAndOrganizationIdOrderByCreatedAtDesc(UUID userId, UUID organizationId);

    List<LeaveRequest> findByOrganizationIdAndStatusAndStartDateGreaterThanEqualOrderByStartDateAsc(
            UUID organizationId, LeaveRequestStatus status, LocalDate fromDate);

    List<LeaveRequest> findByOrganizationIdAndUserIdInAndStatusOrderByCreatedAtAsc(
            UUID organizationId, List<UUID> userIds, LeaveRequestStatus status);

    Optional<LeaveRequest> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<LeaveRequest> findByOrganizationIdAndUserIdInAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            UUID organizationId, List<UUID> userIds, LeaveRequestStatus status, LocalDate onOrAfterStart, LocalDate onOrBeforeEnd);
}
