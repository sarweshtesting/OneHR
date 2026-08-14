package com.nforceone.nforcehq.attendance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRegularizationRequestRepository extends JpaRepository<AttendanceRegularizationRequest, UUID> {

    List<AttendanceRegularizationRequest> findByOrganizationIdAndUserIdInAndStatusOrderByCreatedAtAsc(
            UUID organizationId, List<UUID> userIds, RegularizationStatus status);

    Optional<AttendanceRegularizationRequest> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
